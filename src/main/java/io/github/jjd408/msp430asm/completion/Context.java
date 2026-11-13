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

/**
 * What's being typed at the caret, from the text of its line before it: the word so far, and whether it's the
 * statement's mnemonic (after an optional label) or an operand, and what mark ({@code #} immediate, {@code &}
 * absolute) comes just before it. Nothing is offered inside comments or strings.
 */
public final class Context {

    public enum Kind { NONE, MNEMONIC, OPERAND }

    public final Kind kind;
    /** The partial word before the caret. */
    public final String prefix;
    /** The character just before the word: '#', '&amp;', '@' or 0. */
    public final char mark;
    /** For an operand: the line's mnemonic (instruction or directive), lower case; else "". */
    public final String mnemonic;
    /** For an operand: which one it is, from 0 (the commas before it). */
    public final int operand;

    private Context(Kind kind, String prefix, char mark) {
        this(kind, prefix, mark, "", 0);
    }

    private Context(Kind kind, String prefix, char mark, String mnemonic, int operand) {
        this.kind = kind;
        this.prefix = prefix;
        this.mark = mark;
        this.mnemonic = mnemonic;
        this.operand = operand;
    }

    static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$';
    }

    /** @param before the caret's line, up to the caret */
    public static Context of(String before) {
        // Inside a comment or a string: nothing to offer
        boolean inString = false;
        for (int i = 0; i < before.length(); i++) {
            char c = before.charAt(i);
            if (c == '"') {
                inString = !inString;
            } else if (!inString && (c == ';' || (c == '/' && i + 1 < before.length()
                    && (before.charAt(i + 1) == '/' || before.charAt(i + 1) == '*')))) {
                return new Context(Kind.NONE, "", (char) 0);
            }
        }
        // A '#' first on the line starts a preprocessor line (#include, #define); an immediate can't come first
        if (inString || before.stripLeading().startsWith("#")) {
            return new Context(Kind.NONE, "", (char) 0);
        }

        int start = before.length();
        while (start > 0 && isWordChar(before.charAt(start - 1))) {
            start--;
        }
        String prefix = before.substring(start);
        char mark = start > 0 && "#&@".indexOf(before.charAt(start - 1)) >= 0 ? before.charAt(start - 1) : 0;

        // Drop a leading label ("name:" or "1:"); what's left before the word decides the position
        String head = before.substring(0, start);
        int colon = head.indexOf(':');
        if (colon >= 0 && head.substring(0, colon).trim().matches("[A-Za-z0-9_.$]+")) {
            head = head.substring(colon + 1);
        }
        if (head.isBlank() && mark == 0) {
            return new Context(Kind.MNEMONIC, prefix, mark);
        }
        String rest = head.strip();
        int end = 0;
        while (end < rest.length() && isWordChar(rest.charAt(end))) {
            end++;
        }
        String mnemonic = rest.substring(0, end).toLowerCase(java.util.Locale.ROOT);
        int operand = (int) rest.substring(end).chars().filter(c -> c == ',').count();
        return new Context(Kind.OPERAND, prefix, mark, mnemonic, operand);
    }
}
