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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Where TI's MSP430-GCC is: a configured bin folder, else the usual install places, else the PATH. TI's
 * support files (msp430.h and the device headers) are in the include folder beside bin.
 */
public final class Toolchain {

    public final Path bin;

    private Toolchain(Path bin) {
        this.bin = bin;
    }

    /** Finds the toolchain, preferring {@code configured} (a bin folder; may be null or empty). */
    public static Toolchain find(String configured) {
        List<Path> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(Paths.get(configured));
        }
        String home = System.getProperty("user.home");
        candidates.add(Paths.get(home, "ti", "msp430-gcc", "bin"));
        candidates.add(Paths.get("/opt/ti/msp430-gcc/bin"));
        candidates.add(Paths.get("/usr/local/msp430-gcc/bin"));
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                if (!dir.isEmpty()) {
                    candidates.add(Paths.get(dir));
                }
            }
        }
        for (Path dir : candidates) {
            if (Files.isExecutable(dir.resolve("msp430-elf-as"))) {
                return new Toolchain(dir);
            }
        }
        return null;
    }

    public Path as() {
        return bin.resolve("msp430-elf-as");
    }

    public Path gcc() {
        return bin.resolve("msp430-elf-gcc");
    }

    /** TI's support-file headers, or null when they aren't installed beside bin. */
    public Path include() {
        Path inc = bin.getParent() == null ? null : bin.getParent().resolve("include");
        return inc != null && Files.isRegularFile(inc.resolve("msp430.h")) ? inc : null;
    }
}
