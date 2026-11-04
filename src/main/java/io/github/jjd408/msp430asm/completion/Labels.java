/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.completion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Labels defined in a source text ({@code name:} at the start of a statement), with their 1-based lines. */
public final class Labels {

    private static final Pattern LABEL = Pattern.compile("^\\s*([A-Za-z_.$][A-Za-z0-9_.$]*):");

    private Labels() {
    }

    public static Map<String, Integer> scan(String text) {
        Map<String, Integer> labels = new LinkedHashMap<>();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher m = LABEL.matcher(lines[i]);
            if (m.find()) {
                labels.putIfAbsent(m.group(1), i + 1);
            }
        }
        return labels;
    }
}
