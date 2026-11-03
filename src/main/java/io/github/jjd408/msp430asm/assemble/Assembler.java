/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.assemble;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Assembles source text with msp430-elf-as (through cpp first for .S files) and reports the machine code each
 * source line became and the assembler's messages. Runs in a temporary folder and deletes it afterwards.
 */
public final class Assembler {

    /** The outcome of one run. */
    public static final class Result {
        /** Machine code by source line (1-based). */
        public final Map<Integer, Listing.Entry> code;
        public final List<Diagnostic> diagnostics;

        Result(Map<Integer, Listing.Entry> code, List<Diagnostic> diagnostics) {
            this.code = code;
            this.diagnostics = diagnostics;
        }
    }

    private static final long TIMEOUT_SECONDS = 20;

    private final Toolchain toolchain;
    private final String mcu;

    public Assembler(Toolchain toolchain, String mcu) {
        this.toolchain = toolchain;
        this.mcu = mcu;
    }

    /**
     * @param text the source (it needn't be saved)
     * @param preprocess run cpp first (.S files)
     * @param sourceDir the source file's folder, for #include "..." and .include, or null
     */
    public Result assemble(String text, boolean preprocess, Path sourceDir) throws IOException, InterruptedException {
        Result first = assembleOnce(text, preprocess, sourceDir);
        // With any error, as gives no addresses ("????") anywhere. Assemble again with the lines in error
        // commented out to get them for the rest; the messages still come from the first run.
        List<Integer> errorLines = new ArrayList<>();
        for (Diagnostic d : first.diagnostics) {
            if (d.severity == Diagnostic.Severity.ERROR) {
                errorLines.add(d.line);
            }
        }
        if (errorLines.isEmpty()) {
            return first;
        }
        String[] lines = text.split("\n", -1);
        for (int line : errorLines) {
            if (line >= 1 && line <= lines.length) {
                lines[line - 1] = ";" + lines[line - 1];
            }
        }
        Result second = assembleOnce(String.join("\n", lines), preprocess, sourceDir);
        boolean clean = second.diagnostics.stream().noneMatch(d -> d.severity == Diagnostic.Severity.ERROR);
        return new Result(clean ? second.code : first.code, first.diagnostics);
    }

    private Result assembleOnce(String text, boolean preprocess, Path sourceDir)
            throws IOException, InterruptedException {
        Path tmp = Files.createTempDirectory("msp430asm");
        try {
            String srcName = preprocess ? "src.S" : "src.s";
            Files.writeString(tmp.resolve(srcName), text, StandardCharsets.UTF_8);
            List<Diagnostic> diagnostics = new ArrayList<>();
            String asInput = srcName;
            LineMap lineMap = null;

            if (preprocess) {
                List<String> cpp = new ArrayList<>(List.of(toolchain.gcc().toString(), "-E", "-x",
                        "assembler-with-cpp", "-mmcu=" + mcu));
                if (toolchain.include() != null) {
                    cpp.add("-I" + toolchain.include());
                }
                if (sourceDir != null) {
                    cpp.add("-iquote" + sourceDir);
                    cpp.add("-I" + sourceDir);
                }
                cpp.addAll(List.of(srcName, "-o", "src.i"));
                String out = run(cpp, tmp);
                diagnostics.addAll(Diagnostic.parse(out, srcName));
                if (!Files.isRegularFile(tmp.resolve("src.i"))) {
                    return new Result(Collections.emptyMap(), diagnostics);
                }
                String i = Files.readString(tmp.resolve("src.i"), StandardCharsets.UTF_8);
                lineMap = LineMap.parse(i, srcName);
                asInput = "src.i";
            }

            List<String> as = new ArrayList<>(List.of(toolchain.as().toString(), "-mmcu=" + mcu,
                    "--listing-cont-lines=1000", "-al=src.lst", "-o", "src.o"));
            if (sourceDir != null) {
                as.add("-I" + sourceDir);
            }
            as.add(asInput);
            String out = run(as, tmp);
            // With cpp's line markers in its input, as already names the .S file and its lines
            diagnostics.addAll(Diagnostic.parse(out, srcName));

            Map<Integer, Listing.Entry> code = new HashMap<>();
            Path lst = tmp.resolve("src.lst");
            if (Files.isRegularFile(lst)) {
                for (Map.Entry<Integer, Listing.Entry> e
                        : Listing.parse(Files.readString(lst, StandardCharsets.ISO_8859_1)).entrySet()) {
                    int line = lineMap == null ? e.getKey() : lineMap.sourceLine(e.getKey());
                    if (line > 0) {
                        code.put(line, e.getValue());
                    }
                }
            }
            return new Result(code, diagnostics);
        } finally {
            try (Stream<Path> files = Files.walk(tmp)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    /** Runs a command in a folder and returns what it printed (stdout and stderr together). */
    private static String run(List<String> command, Path dir) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        // Read the output on another thread, so a stuck process can't keep the timeout from running
        CompletableFuture<byte[]> out = CompletableFuture.supplyAsync(() -> {
            try {
                return p.getInputStream().readAllBytes();
            } catch (IOException e) {
                return new byte[0];
            }
        });
        if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException(command.get(0) + " took longer than " + TIMEOUT_SECONDS + " s");
        }
        return new String(out.join(), StandardCharsets.UTF_8);
    }
}
