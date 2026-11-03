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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps the lines of C-preprocessor output back to the source they came from. A .S file goes through cpp before
 * as, and the listing numbers the preprocessed lines; cpp's line markers ({@code # 12 "file.S" 2}) say which
 * source line each one is.
 */
public final class LineMap {

    private static final Pattern MARKER = Pattern.compile("^# (\\d+) \"((?:[^\"\\\\]|\\\\.)*)\".*$");

    private final int[] sourceLine;  // by preprocessed line (1-based); 0 when it isn't from the main file

    private LineMap(int[] sourceLine) {
        this.sourceLine = sourceLine;
    }

    /**
     * @param preprocessed cpp's output, with line markers
     * @param mainFile the file name cpp was given, as it appears in the markers
     */
    public static LineMap parse(String preprocessed, String mainFile) {
        String[] lines = preprocessed.split("\n", -1);
        int[] map = new int[lines.length + 1];
        String file = mainFile;
        int next = 1;
        for (int i = 0; i < lines.length; i++) {
            Matcher m = MARKER.matcher(lines[i]);
            if (m.matches()) {
                next = Integer.parseInt(m.group(1));
                file = m.group(2);
                continue;  // the marker line itself isn't source
            }
            map[i + 1] = file.equals(mainFile) ? next : 0;
            next++;
        }
        return new LineMap(map);
    }

    /** The main file's line (1-based) that a preprocessed line came from, or 0. */
    public int sourceLine(int preprocessedLine) {
        return preprocessedLine > 0 && preprocessedLine < sourceLine.length ? sourceLine[preprocessedLine] : 0;
    }
}
