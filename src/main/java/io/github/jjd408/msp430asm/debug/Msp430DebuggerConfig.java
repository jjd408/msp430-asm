/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.debug;

import io.github.jjd408.msp430asm.assemble.PicSimLab;
import io.github.jjd408.msp430asm.assemble.Toolchain;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** What a debugging session needs: the program, its sources, the tools, and where to report. */
public final class Msp430DebuggerConfig {
    /** The program's name, for the session ("Blinky"). */
    public final String name;
    public final Path elf;
    /** The sources it was built from; breakpoints in other files are left alone. */
    public final List<Path> sources;
    public final Toolchain toolchain;
    public final PicSimLab picsimlab;
    public final String board;
    public final String mcu;
    public final int port;
    /** The Output window tab: messages, and errors. */
    public final Consumer<String> out;
    public final Consumer<String> err;

    public Msp430DebuggerConfig(String name, Path elf, List<Path> sources, Toolchain toolchain, PicSimLab picsimlab,
            String board, String mcu, int port, Consumer<String> out, Consumer<String> err) {
        this.name = name;
        this.elf = elf;
        this.sources = List.copyOf(sources);
        this.toolchain = toolchain;
        this.picsimlab = picsimlab;
        this.board = board;
        this.mcu = mcu;
        this.port = port;
        this.out = out;
        this.err = err;
    }
}
