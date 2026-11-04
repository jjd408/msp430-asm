/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.project;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The files of an MSP430 project: a folder with {@code msp430-project.properties} (its name and optional device,
 * C flags and PICSimLab board, each overriding Tools &gt; Options), its sources under {@code src/}, and its output
 * in {@code build/}.
 */
public final class ProjectFiles {

    public static final String MARKER = "msp430-project.properties";
    public static final String SRC = "src";
    public static final String BUILD = "build";

    public static final String NAME = "name";
    public static final String MCU = "mcu";
    public static final String CFLAGS = "cflags";
    public static final String BOARD = "board";
    public static final String DEFAULT_CFLAGS = "-Os -Wall";

    /** Source extensions built into the program. */
    private static final Set<String> SOURCES = Set.of("s", "S", "s43", "S43", "asm", "c");

    private ProjectFiles() {
    }

    public static boolean isProject(Path dir) {
        return Files.isRegularFile(dir.resolve(MARKER));
    }

    public static Properties load(Path dir) {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(dir.resolve(MARKER), StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            // an unreadable file: defaults
        }
        return p;
    }

    /** The project's name: its {@code name} property, else its folder's name. */
    public static String name(Path dir, Properties p) {
        String n = p.getProperty(NAME, "").trim();
        return n.isEmpty() ? dir.getFileName().toString() : n;
    }

    /** A property, or {@code fallback} when it's missing or empty. */
    public static String get(Properties p, String key, String fallback) {
        String v = p.getProperty(key, "").trim();
        return v.isEmpty() ? fallback : v;
    }

    /** The sources under src/, in a stable order. */
    public static List<Path> sources(Path dir) throws IOException {
        Path src = dir.resolve(SRC);
        if (!Files.isDirectory(src)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(src)) {
            return files.filter(Files::isRegularFile).filter(ProjectFiles::isSource).sorted()
                    .collect(Collectors.toList());
        }
    }

    static boolean isSource(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 && SOURCES.contains(name.substring(dot + 1));
    }

    /** A name usable as a file name: anything but letters, digits, '.', '_' and '-' becomes '_'. */
    public static String outputName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** Deletes build/, reporting to {@code out}. */
    public static void clean(Path dir, java.util.function.Consumer<String> out) throws IOException {
        Path build = dir.resolve(BUILD);
        if (!Files.exists(build)) {
            out.accept("Nothing to clean.");
            return;
        }
        try (Stream<Path> files = Files.walk(build)) {
            for (Path f : files.sorted(java.util.Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.delete(f);
            }
        }
        out.accept("Deleted " + build);
    }

    /** Makes a new project in {@code dir} (which may exist but must be empty): two sources that link together. */
    public static void create(Path dir, String name) throws IOException {
        Files.createDirectories(dir.resolve(SRC));
        Files.writeString(dir.resolve(MARKER), String.join("\n",
                "# MSP430 project (MSP430 Assembly for Apache NetBeans)",
                "# Everything under src/ (.s .S .s43 .asm .c) is built into build/<name>.hex.",
                "name=" + name,
                "# Device for -mmcu; empty: the one in Tools > Options > Miscellaneous > MSP430",
                "mcu=",
                "# Extra compiler flags (for C sources)",
                "cflags=" + DEFAULT_CFLAGS,
                "# PICSimLab board to run on (Solitaire or MSP430_Board); empty: the one in Options",
                "board=",
                ""), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(SRC).resolve("main.S"), String.join("\n",
                "; " + name + ": an MSP430FR2355 program in GNU assembler",
                "; .S files go through the C preprocessor, so TI names from msp430.h work here",
                "#include <msp430.h>",
                "",
                "        .text",
                "        .global main",
                "main:   mov.w   #WDTPW|WDTHOLD, &WDTCTL ; stop the watchdog",
                "        bic.w   #LOCKLPM5, &PM5CTL0     ; let the port settings take effect",
                "        bis.b   #BIT0, &P1DIR           ; P1.0 is an output",
                "loop:   xor.b   #BIT0, &P1OUT           ; toggle P1.0",
                "        mov.w   #50000, R12",
                "        call    #delay                  ; in delay.S",
                "        jmp     loop",
                ""), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(SRC).resolve("delay.S"), String.join("\n",
                "; delay: waits about R12 x 3 cycles",
                "; A routine in a file of its own, linked with main.S into one program",
                "#include <msp430.h>",
                "",
                "        .text",
                "        .global delay",
                "delay:  dec.w   R12",
                "        jnz     delay",
                "        ret",
                ""), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(".gitignore"), "build/\n", StandardCharsets.UTF_8);
    }
}
