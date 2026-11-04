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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * PICSimLab with the MSP430FR2355 backend (github.com/jjd408/picsimlab-msp430): found by its run.sh, which builds
 * it and runs it with settings kept apart from any installed PICSimLab.
 */
public final class PicSimLab {

    public static final String PROCESSOR = "MSP430FR2355";

    public final Path dir;

    private PicSimLab(Path dir) {
        this.dir = dir;
    }

    /** Finds it, preferring {@code configured} (its folder; may be null or empty). */
    public static PicSimLab find(String configured) {
        List<Path> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(Paths.get(configured));
        }
        String home = System.getProperty("user.home");
        candidates.add(Paths.get(home, "projects", "solitaire", "sim", "picsimlab-msp430"));
        candidates.add(Paths.get(home, "picsimlab-msp430"));
        for (Path dir : candidates) {
            if (Files.isExecutable(dir.resolve("run.sh"))) {
                return new PicSimLab(dir);
            }
        }
        return null;
    }

    /** The command that starts it on a board with a hex file. */
    public List<String> command(String board, Path hex) {
        return List.of(dir.resolve("run.sh").toString(), board, PROCESSOR, hex.toString());
    }

    /** Starts it; the caller reads its output. */
    public Process launch(String board, Path hex) throws IOException {
        return new ProcessBuilder(command(board, hex)).directory(dir.toFile()).redirectErrorStream(true).start();
    }
}
