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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Errors and warnings from msp430-elf-as and cpp, e.g. {@code src.s:13: Error: unknown opcode `movz'}. */
public final class Diagnostic {

    public enum Severity { ERROR, WARNING }

    /** 1-based line in the source file. */
    public final int line;
    public final Severity severity;
    public final String message;

    public Diagnostic(int line, Severity severity, String message) {
        this.line = line;
        this.severity = severity;
        this.message = message;
    }

    // as: "file:line: Error: msg"; cpp: "file:line:col: error: msg" or "fatal error: msg"
    private static final Pattern MESSAGE = Pattern.compile(
            "^(.+?):(\\d+):(?:\\d+:)? *(Error|Warning|error|warning|fatal error): (.*)$");

    /**
     * Picks out the messages about a file. Messages about other files (headers it includes) go on line 1,
     * naming the file.
     */
    public static List<Diagnostic> parse(String output, String file) {
        return parse(output, file, true);
    }

    /**
     * Picks out the messages about a file.
     *
     * @param othersOnLine1 put messages about other files on line 1, naming them; when several files are built
     *     together, each file's messages belong to it alone, so pass false
     */
    public static List<Diagnostic> parse(String output, String file, boolean othersOnLine1) {
        List<Diagnostic> out = new ArrayList<>();
        for (String raw : output.split("\r?\n")) {
            Matcher m = MESSAGE.matcher(raw);
            if (!m.matches()) {
                continue;
            }
            Severity sev = m.group(3).toLowerCase().contains("error") ? Severity.ERROR : Severity.WARNING;
            if (m.group(1).equals(file)) {
                out.add(new Diagnostic(Integer.parseInt(m.group(2)), sev, m.group(4)));
            } else if (othersOnLine1) {
                out.add(new Diagnostic(1, sev, m.group(1) + ":" + m.group(2) + ": " + m.group(4)));
            }
        }
        return out;
    }

    @Override
    public String toString() {
        return line + ": " + severity + ": " + message;
    }
}
