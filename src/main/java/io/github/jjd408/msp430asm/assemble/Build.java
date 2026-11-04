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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a source file into an .elf and an Intel HEX file with msp430-elf-gcc, linked with TI's linker script
 * for the device and gcc's start-up code (which calls {@code main}), then reports the size. Every command and
 * its output go to a consumer as they happen, for an output window.
 */
public final class Build {

    /** The outcome of a build. */
    public static final class Result {
        public final boolean ok;
        public final Path elf;
        public final Path hex;
        /** Messages about the source file, by its line. */
        public final List<Diagnostic> diagnostics;

        Result(boolean ok, Path elf, Path hex, List<Diagnostic> diagnostics) {
            this.ok = ok;
            this.elf = elf;
            this.hex = hex;
            this.diagnostics = diagnostics;
        }
    }

    // The linker names the function a reference is in; with as's line information that's ".Loc.<line>.<n>"
    private static final Pattern LOC = Pattern.compile("in function `\\.Loc\\.(\\d+)\\.\\d+'");
    private static final Pattern UNDEFINED = Pattern.compile("^(.+?):(\\d+): (undefined reference to .*)$");

    private final Toolchain toolchain;
    private final String mcu;

    public Build(Toolchain toolchain, String mcu) {
        this.toolchain = toolchain;
        this.mcu = mcu;
    }

    /**
     * @param source the source file (saved)
     * @param outDir where the .elf and .hex go (made if needed)
     * @param out receives each command ("$ ...") and each line it prints
     */
    public Result build(Path source, Path outDir, Consumer<String> out) throws IOException, InterruptedException {
        Files.createDirectories(outDir);
        String name = source.getFileName().toString();
        String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        Path elf = outDir.resolve(base + ".elf");
        Path hex = outDir.resolve(base + ".hex");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        boolean cpp = ext.equals("S") || ext.equals("S43");

        List<String> gcc = new ArrayList<>(List.of(toolchain.gcc().toString(), "-mmcu=" + mcu, "-g",
                "-Wa,--gdwarf-2"));
        if (toolchain.include() != null) {
            gcc.add("-I" + toolchain.include());
            gcc.add("-L" + toolchain.include());
        }
        if (source.getParent() != null) {
            gcc.add("-I" + source.getParent());
        }
        // gcc knows .s and .S; .asm and .s43 need telling
        gcc.addAll(List.of("-x", cpp ? "assembler-with-cpp" : "assembler", source.toString(), "-x", "none",
                "-o", elf.toString()));

        List<String> output = new ArrayList<>();
        int status = run(gcc, out, output);
        List<Diagnostic> diagnostics = new ArrayList<>(Diagnostic.parse(String.join("\n", output),
                source.toString()));
        diagnostics.addAll(linkerErrors(output, source.toString()));
        if (status != 0) {
            out.accept("Build failed.");
            return new Result(false, elf, hex, diagnostics);
        }
        if (run(List.of(toolchain.bin.resolve("msp430-elf-objcopy").toString(), "-O", "ihex", elf.toString(),
                hex.toString()), out, new ArrayList<>()) != 0) {
            out.accept("Build failed: couldn't write the hex file.");
            return new Result(false, elf, hex, diagnostics);
        }
        run(List.of(toolchain.bin.resolve("msp430-elf-size").toString(), elf.toString()), out, new ArrayList<>());
        out.accept("Built " + hex);
        return new Result(true, elf, hex, diagnostics);
    }

    /** "undefined reference" errors from the linker, on the line the .Loc symbol or the message names. */
    static List<Diagnostic> linkerErrors(List<String> output, String file) {
        List<Diagnostic> out = new ArrayList<>();
        int locLine = 0;
        for (String raw : output) {
            String line = raw.replaceFirst("^.*?/bin/ld: ", "");
            Matcher loc = LOC.matcher(line);
            if (loc.find()) {
                locLine = Integer.parseInt(loc.group(1));
                continue;
            }
            Matcher m = UNDEFINED.matcher(line);
            if (m.matches() && m.group(1).equals(file)) {
                int at = locLine > 0 ? locLine : Integer.parseInt(m.group(2));
                out.add(new Diagnostic(at, Diagnostic.Severity.ERROR, m.group(3)));
                locLine = 0;
            }
        }
        return out;
    }

    private static int run(List<String> command, Consumer<String> out, List<String> collected)
            throws IOException, InterruptedException {
        out.accept("$ " + String.join(" ", command));
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                collected.add(line);
                out.accept(line);
            }
        }
        return p.waitFor();
    }
}
