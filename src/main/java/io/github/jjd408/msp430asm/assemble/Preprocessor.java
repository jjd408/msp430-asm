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

/**
 * Catches C preprocessor lines ({@code #include <msp430.h>}, {@code #define}...) in a file that doesn't go through
 * the preprocessor: only .S and .S43 files do. In a .s, .s43 or .asm file the assembler takes them as comments, so
 * TI's register names stay undefined and the errors that follow ("unknown operand #WDTPW|WDTHOLD") don't say why.
 */
public final class Preprocessor {

    private static final Pattern DIRECTIVE = Pattern.compile(
            "^\\s*#\\s*(include|define|undef|if|ifdef|ifndef|elif|else|endif|pragma|error)\\b");

    private Preprocessor() {
    }

    /** Whether files with this extension go through the C preprocessor. */
    public static boolean runsOn(String extension) {
        return extension.equals("S") || extension.equals("S43");
    }

    /** One message per preprocessor line in text that won't be preprocessed, for a file with this name. */
    public static List<Diagnostic> ignoredDirectives(String text, String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot + 1) : "";
        String rename = base + "." + (ext.equalsIgnoreCase("s43") ? "S43" : "S");
        List<Diagnostic> out = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher m = DIRECTIVE.matcher(lines[i]);
            if (m.find()) {
                out.add(new Diagnostic(i + 1, Diagnostic.Severity.ERROR, "#" + m.group(1) + " is ignored in "
                        + fileName + ": only .S files go through the C preprocessor. Rename the file " + rename
                        + " (capital S)."));
            }
        }
        return out;
    }
}
