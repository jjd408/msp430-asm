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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds source files (assembly and C) into one .elf and an Intel HEX file with msp430-elf-gcc, linked with TI's
 * linker script for the device and gcc's start-up code (which calls {@code main}), then reports the size. Every
 * command and its output go to a consumer as they happen, for an output window.
 */
public final class Build {

    /** The outcome of a build. */
    public static final class Result {
        public final boolean ok;
        public final Path elf;
        public final Path hex;
        /** Messages about the (first) source file, by its line. */
        public final List<Diagnostic> diagnostics;
        /** Messages by source file. */
        public final Map<Path, List<Diagnostic>> byFile;

        Result(boolean ok, Path elf, Path hex, Map<Path, List<Diagnostic>> byFile, Path first) {
            this.ok = ok;
            this.elf = elf;
            this.hex = hex;
            this.byFile = byFile;
            this.diagnostics = byFile.getOrDefault(first, List.of());
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
     * Builds one source file into &lt;outDir&gt;/&lt;name&gt;.elf and .hex.
     *
     * @param source the source file (saved)
     * @param outDir where the .elf and .hex go (made if needed)
     * @param out receives each command ("$ ...") and each line it prints
     */
    public Result build(Path source, Path outDir, Consumer<String> out) throws IOException, InterruptedException {
        String name = source.getFileName().toString();
        String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        List<Path> includes = source.getParent() == null ? List.of() : List.of(source.getParent());
        return buildAll(List.of(source), includes, "", outDir, base, out);
    }

    /**
     * Builds source files into one program, &lt;outDir&gt;/&lt;name&gt;.elf and .hex.
     *
     * @param sources assembly (.s .asm .s43; .S .S43 through cpp) and C (.c) files
     * @param includes folders for #include and .include, besides TI's
     * @param cflags extra compiler flags, separated by spaces (for C; may be empty)
     */
    public Result buildAll(List<Path> sources, List<Path> includes, String cflags, Path outDir, String name,
            Consumer<String> out) throws IOException, InterruptedException {
        Files.createDirectories(outDir);
        Path elf = outDir.resolve(name + ".elf");
        Path hex = outDir.resolve(name + ".hex");

        List<String> gcc = new ArrayList<>(List.of(toolchain.gcc().toString(), "-mmcu=" + mcu, "-g",
                "-Wa,--gdwarf-2"));
        for (String flag : cflags.trim().split("\\s+")) {
            if (!flag.isEmpty()) {
                gcc.add(flag);
            }
        }
        if (toolchain.include() != null) {
            gcc.add("-I" + toolchain.include());
            gcc.add("-L" + toolchain.include());
        }
        for (Path inc : includes) {
            gcc.add("-I" + inc);
        }
        for (Path source : sources) {
            // gcc knows .s, .S and .c; .asm and .s43 need telling, so every file says what it is
            gcc.addAll(List.of("-x", language(source), source.toString()));
        }
        gcc.addAll(List.of("-x", "none", "-o", elf.toString()));

        List<String> output = new ArrayList<>();
        int status = run(gcc, out, output);
        Map<Path, List<Diagnostic>> byFile = new LinkedHashMap<>();
        String text = String.join("\n", output);
        for (Path source : sources) {
            List<Diagnostic> d = new ArrayList<>(Diagnostic.parse(text, source.toString(), sources.size() == 1));
            d.addAll(linkerErrors(output, source.toString()));
            byFile.put(source, d);
        }
        if (status != 0) {
            out.accept("Build failed.");
            return new Result(false, elf, hex, byFile, sources.get(0));
        }
        if (run(List.of(toolchain.bin.resolve("msp430-elf-objcopy").toString(), "-O", "ihex", elf.toString(),
                hex.toString()), out, new ArrayList<>()) != 0) {
            out.accept("Build failed: couldn't write the hex file.");
            return new Result(false, elf, hex, byFile, sources.get(0));
        }
        run(List.of(toolchain.bin.resolve("msp430-elf-size").toString(), elf.toString()), out, new ArrayList<>());
        out.accept("Built " + hex);
        return new Result(true, elf, hex, byFile, sources.get(0));
    }

    /** gcc's -x language for a source file. */
    static String language(Path source) {
        String name = source.getFileName().toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        switch (ext) {
            case "c":
                return "c";
            case "S":
            case "S43":
                return "assembler-with-cpp";
            default:
                return "assembler";
        }
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
