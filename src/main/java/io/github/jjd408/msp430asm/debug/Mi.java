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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GDB's machine interface (MI) output: records like {@code 12^done,value="5"} or
 * {@code *stopped,reason="breakpoint-hit",frame={file="main.S",line="9"}}. Values are strings, tuples
 * ({@code Map<String, Object>}) and lists ({@code List<Object>}; a list of results keeps only their values).
 */
public final class Mi {

    /** One line of output. */
    public static final class Record {
        /** '^' result, '*' exec, '+' status, '=' notify, '~' console, '@' target, '&' log, '(' prompt */
        public final char kind;
        /** The command's token, or -1. */
        public final int token;
        /** "done", "running", "error", "stopped"...; for stream records, the text. */
        public final String name;
        public final Map<String, Object> results;

        Record(char kind, int token, String name, Map<String, Object> results) {
            this.kind = kind;
            this.token = token;
            this.name = name;
            this.results = results;
        }

        public String get(String key) {
            Object v = results.get(key);
            return v instanceof String ? (String) v : null;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> tuple(String key) {
            Object v = results.get(key);
            return v instanceof Map ? (Map<String, Object>) v : Map.of();
        }

        @SuppressWarnings("unchecked")
        public List<Object> list(String key) {
            Object v = results.get(key);
            return v instanceof List ? (List<Object>) v : List.of();
        }

        @Override
        public String toString() {
            return kind + name + results;
        }
    }

    private final String s;
    private int i;

    private Mi(String s) {
        this.s = s;
    }

    /** Parses a line of MI output. */
    public static Record parse(String line) {
        if (line.startsWith("(gdb)")) {
            return new Record('(', -1, "", Map.of());
        }
        Mi p = new Mi(line);
        int start = 0;
        while (p.i < line.length() && Character.isDigit(line.charAt(p.i))) {
            p.i++;
        }
        int token = p.i > start ? Integer.parseInt(line.substring(start, p.i)) : -1;
        if (p.i >= line.length()) {
            return new Record('~', token, line, Map.of());
        }
        char kind = line.charAt(p.i++);
        switch (kind) {
            case '~':
            case '@':
            case '&':
                return new Record(kind, token, p.cstring(), Map.of());
            case '^':
            case '*':
            case '+':
            case '=': {
                int comma = line.indexOf(',', p.i);
                String name = comma < 0 ? line.substring(p.i) : line.substring(p.i, comma);
                Map<String, Object> results = new LinkedHashMap<>();
                if (comma >= 0) {
                    p.i = comma + 1;
                    p.results(results, '\0');
                }
                return new Record(kind, token, name, results);
            }
            default:
                return new Record('~', -1, line, Map.of());  // not MI: the program's or gdb's own output
        }
    }

    private void results(Map<String, Object> into, char end) {
        while (i < s.length() && s.charAt(i) != end) {
            int eq = s.indexOf('=', i);
            if (eq < 0) {
                return;
            }
            String key = s.substring(i, eq);
            i = eq + 1;
            into.put(key, value());
            if (i < s.length() && s.charAt(i) == ',') {
                i++;
            }
        }
    }

    private Object value() {
        if (i >= s.length()) {
            return "";
        }
        char c = s.charAt(i);
        if (c == '"') {
            return cstring();
        }
        if (c == '{') {
            i++;
            Map<String, Object> t = new LinkedHashMap<>();
            results(t, '}');
            i++;
            return t;
        }
        if (c == '[') {
            i++;
            List<Object> l = new ArrayList<>();
            while (i < s.length() && s.charAt(i) != ']') {
                char d = s.charAt(i);
                if (d != '"' && d != '{' && d != '[') {
                    i = s.indexOf('=', i) + 1;  // a result: keep its value
                }
                l.add(value());
                if (i < s.length() && s.charAt(i) == ',') {
                    i++;
                }
            }
            i++;
            return l;
        }
        int end = i;
        while (end < s.length() && ",}]".indexOf(s.charAt(end)) < 0) {
            end++;
        }
        String v = s.substring(i, end);
        i = end;
        return v;
    }

    private String cstring() {
        StringBuilder b = new StringBuilder();
        if (i < s.length() && s.charAt(i) == '"') {
            i++;
        }
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') {
                break;
            }
            if (c == '\\' && i < s.length()) {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n':
                        b.append('\n');
                        break;
                    case 't':
                        b.append('\t');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    default:
                        if (e >= '0' && e <= '7' && i + 1 < s.length()) {  // \ooo
                            b.append((char) Integer.parseInt(s.substring(i - 1, i + 2), 8));
                            i += 2;
                        } else {
                            b.append(e);
                        }
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /** A C string for an MI command argument. */
    public static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
