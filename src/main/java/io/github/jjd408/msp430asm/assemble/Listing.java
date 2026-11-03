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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the listing msp430-elf-as writes with {@code -al}: for each input line that produced code or data, its
 * address (an offset in its section, since nothing is linked yet) and its bytes.
 *
 * <pre>
 *    4 0000 B240 805A 	main:   mov.w   #0x5A80, &amp;0x01CC
 *    4      CC01
 *    5 0006 D2D3 0000 	        bis.b   #1, &amp;P1DIR
 * </pre>
 *
 * Each line is the input line number, a four-character address (blank on continuation lines, "????" while the
 * source has errors), up to four groups of bytes in memory order, then a tab and the source.
 */
public final class Listing {

    /** The code or data for one input line. */
    public static final class Entry {
        /** Offset in the section, or null when as couldn't give one. */
        public final String address;
        private final StringBuilder hex = new StringBuilder();

        Entry(String address) {
            this.address = address;
        }

        /** The bytes as little-endian 16-bit words, as the CPU reads them ("40B2 5A80 01CC"); an odd last byte
         * is shown alone. */
        public String words() {
            StringBuilder out = new StringBuilder();
            String h = hex.toString();
            int i = 0;
            for (; i + 4 <= h.length(); i += 4) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(h, i + 2, i + 4).append(h, i, i + 2);
            }
            if (i + 2 <= h.length()) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(h, i, i + 2);
            }
            return out.toString();
        }

        public int byteCount() {
            return hex.length() / 2;
        }
    }

    // line number, a space, the address field (4 characters), a space, the byte groups
    private static final Pattern LINE = Pattern.compile("^\\s*(\\d+) (.{4}) ?([0-9A-Fa-f ]*)$");

    private Listing() {
    }

    /** Parses a listing into entries by input line number (1-based), for lines with at least one byte. */
    public static Map<Integer, Entry> parse(String listing) {
        Map<Integer, Entry> entries = new HashMap<>();
        for (String raw : listing.split("\r?\n")) {
            int tab = raw.indexOf('\t');
            String left = tab >= 0 ? raw.substring(0, tab) : raw;
            Matcher m = LINE.matcher(left);
            if (!m.matches()) {
                continue;  // page headers, blank lines
            }
            String bytes = m.group(3).replace(" ", "");
            if (bytes.isEmpty() || bytes.length() % 2 != 0) {
                continue;
            }
            int line = Integer.parseInt(m.group(1));
            String address = m.group(2).trim();
            Entry e = entries.get(line);
            if (e == null) {
                e = new Entry(address.isEmpty() || address.contains("?") ? null : address.toUpperCase());
                entries.put(line, e);
            }
            e.hex.append(bytes.toUpperCase());
        }
        return entries;
    }
}
