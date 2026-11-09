/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.format;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out MSP430 assembly in columns: labels at column 0, the mnemonic (instruction or directive) at one column,
 * operands at the next, and end-of-line comments at a third. Columns count from 0, and the output uses spaces.
 *
 * <p>A part that doesn't fit before its column follows the previous part after one space. Each line stays one line,
 * so line numbers (and the breakpoints on them) don't move.
 *
 * <p>Left as they are, apart from trailing spaces: preprocessor lines and their backslash continuations, lines
 * inside or starting a block comment, and symbol assignments (<code>x = 5</code>). Whole-line comments keep their
 * text and go to column 0, or to the mnemonic column if they were indented.
 *
 * <p>Within operands, a comma at the top level gets one space after it and none before; anything inside parentheses,
 * strings or character constants is untouched.
 */
public final class AsmFormatter {

    public static final int DEFAULT_MNEMONIC_COLUMN = 8;
    public static final int DEFAULT_OPERAND_COLUMN = 16;
    public static final int DEFAULT_COMMENT_COLUMN = 56;

    private final int mnemonicColumn;
    private final int operandColumn;
    private final int commentColumn;

    public AsmFormatter(int mnemonicColumn, int operandColumn, int commentColumn) {
        this.mnemonicColumn = Math.max(1, mnemonicColumn);
        this.operandColumn = Math.max(this.mnemonicColumn + 1, operandColumn);
        this.commentColumn = Math.max(this.operandColumn + 1, commentColumn);
    }

    public AsmFormatter() {
        this(DEFAULT_MNEMONIC_COLUMN, DEFAULT_OPERAND_COLUMN, DEFAULT_COMMENT_COLUMN);
    }

    /** Formats a whole text, line by line, ending it with a newline (unless it's empty). */
    public String format(String text) {
        List<String> lines = splitLines(text);
        List<String> out = formatLines(lines);
        StringBuilder sb = new StringBuilder();
        for (String l : out) {
            sb.append(l).append('\n');
        }
        return sb.toString();
    }

    /** Formats each line (without its line ending). Lines are read in order, as block comments span lines. */
    public List<String> formatLines(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        State state = new State();
        for (String l : lines) {
            out.add(formatLine(l, state));
        }
        return out;
    }

    /** What carries over from one line to the next. */
    public static final class State {
        boolean inBlockComment;
        boolean continuation;  // the previous line ended in a backslash (a preprocessor line goes on)
    }

    /** Formats one line (without its line ending), given the state from the lines before it. */
    public String formatLine(String line, State state) {
        String l = stripTrailing(line);
        if (state.continuation) {
            state.continuation = l.endsWith("\\");
            return l;
        }
        if (state.inBlockComment) {
            int end = l.indexOf("*/");
            if (end < 0) {
                return l;
            }
            state.inBlockComment = false;
            // Anything after the comment's end is left as it is; it's rare and hard to place
            if (opensBlockComment(l.substring(end + 2))) {
                state.inBlockComment = true;
            }
            return l;
        }
        String t = l.trim();
        if (t.isEmpty()) {
            return "";
        }
        if (t.startsWith("#")) {
            state.continuation = l.endsWith("\\");
            return l;
        }
        if (t.startsWith(";") || t.startsWith("//")) {
            return (Character.isWhitespace(l.charAt(0)) ? spaces(mnemonicColumn) : "") + t;
        }
        if (opensBlockComment(l)) {
            state.inBlockComment = true;
            return l;
        }
        if (l.contains("/*")) {
            return l;  // a block comment within the line: leave the line alone
        }
        return statement(t);
    }

    private String statement(String t) {
        int commentAt = commentStart(t);
        String code = commentAt < 0 ? t : t.substring(0, commentAt).trim();
        String comment = commentAt < 0 ? null : t.substring(commentAt);

        // Labels: words ending in ':' at the start (as allows several)
        StringBuilder labels = new StringBuilder();
        int i = 0;
        while (true) {
            int end = wordEnd(code, i);
            if (end > i && end < code.length() && code.charAt(end) == ':') {
                if (labels.length() > 0) {
                    labels.append(' ');
                }
                labels.append(code, i, end + 1);
                i = skipSpaces(code, end + 1);
            } else {
                break;
            }
        }
        String rest = code.substring(i);

        String mnemonic = "";
        String operands = "";
        if (!rest.isEmpty()) {
            int end = wordEnd(rest, 0);
            if (end == 0) {
                return t;  // doesn't start with a word: leave it
            }
            mnemonic = rest.substring(0, end);
            operands = rest.substring(end).trim();
            if (operands.startsWith("=") && !operands.startsWith("==")) {
                return t;  // a symbol assignment, x = 5
            }
            operands = operands(operands);
        }

        StringBuilder sb = new StringBuilder(labels);
        if (!mnemonic.isEmpty()) {
            padTo(sb, mnemonicColumn);
            sb.append(mnemonic);
            if (!operands.isEmpty()) {
                padTo(sb, operandColumn);
                sb.append(operands);
            }
        }
        if (comment != null) {
            if (sb.length() > 0) {
                padTo(sb, commentColumn);
            } else {
                sb.append(spaces(mnemonicColumn));  // only a comment after all; can't happen, but be safe
            }
            sb.append(comment(comment));
        }
        return sb.toString();
    }

    /** Pads to a column, or adds one space if the text already reaches it. */
    private static void padTo(StringBuilder sb, int column) {
        if (sb.length() == 0) {
            sb.append(spaces(column));
        } else if (sb.length() < column) {
            sb.append(spaces(column - sb.length()));
        } else {
            sb.append(' ');
        }
    }

    /** One space after a ';' (or '//') that's followed straight away by a letter or digit. */
    private static String comment(String c) {
        int mark = c.startsWith("//") ? 2 : 1;
        if (c.length() > mark && Character.isLetterOrDigit(c.charAt(mark))) {
            return c.substring(0, mark) + " " + c.substring(mark);
        }
        return c;
    }

    /** Operands with ", " between them at the top level. */
    private static String operands(String s) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                int end = stringEnd(s, i);
                sb.append(s, i, end);
                i = end - 1;
            } else if (c == '\'') {
                int end = charEnd(s, i);
                sb.append(s, i, end);
                i = end - 1;
            } else if (c == '(' || c == '[') {
                depth++;
                sb.append(c);
            } else if ((c == ')' || c == ']') && depth > 0) {
                depth--;
                sb.append(c);
            } else if (c == ',' && depth == 0) {
                while (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
                    sb.setLength(sb.length() - 1);
                }
                sb.append(", ");
                i = skipSpaces(s, i + 1) - 1;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    /** Where the end-of-line comment starts (';' or '//' outside strings and character constants), or -1. */
    static int commentStart(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                i = stringEnd(s, i) - 1;
            } else if (c == '\'') {
                i = charEnd(s, i) - 1;
            } else if (c == ';' || (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/')) {
                return i;
            }
        }
        return -1;
    }

    /** True if the line opens a block comment that it doesn't close. */
    private static boolean opensBlockComment(String s) {
        boolean open = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (open) {
                if (c == '*' && i + 1 < s.length() && s.charAt(i + 1) == '/') {
                    open = false;
                    i++;
                }
            } else if (c == '"') {
                i = stringEnd(s, i) - 1;
            } else if (c == '\'') {
                i = charEnd(s, i) - 1;
            } else if (c == ';') {
                return false;  // the rest is a comment already
            } else if (c == '/' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == '/') {
                    return false;
                }
                if (n == '*') {
                    open = true;
                    i++;
                }
            }
        }
        return open;
    }

    /** The index after a string starting at i (or the end of the text if it isn't closed). */
    private static int stringEnd(String s, int i) {
        for (int j = i + 1; j < s.length(); j++) {
            char c = s.charAt(j);
            if (c == '\\') {
                j++;
            } else if (c == '"') {
                return j + 1;
            }
        }
        return s.length();
    }

    /** The index after a character constant starting at i: as writes 'A, and 'A' is accepted too. */
    private static int charEnd(String s, int i) {
        int j = i + 1;
        if (j < s.length() && s.charAt(j) == '\\') {
            j++;
        }
        j = Math.min(j + 1, s.length());
        if (j < s.length() && s.charAt(j) == '\'') {
            j++;
        }
        return j;
    }

    private static int wordEnd(String s, int i) {
        int j = i;
        while (j < s.length()) {
            char c = s.charAt(j);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$') {
                j++;
            } else {
                break;
            }
        }
        return j;
    }

    private static int skipSpaces(String s, int i) {
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    private static String spaces(int n) {
        return " ".repeat(Math.max(0, n));
    }

    /** Splits text into lines without their endings (\n, \r\n or \r). */
    public static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                lines.add(text.substring(start, i));
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start));
        }
        return lines;
    }
}
