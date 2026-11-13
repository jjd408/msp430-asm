/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.help;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A device's peripheral registers and bit constants, read from TI's support files: msp430&lt;mcu&gt;.h
 * ({@code sfr_w(WDTCTL); /* Watchdog Timer Control Register *&#47;} and {@code #define WDTPW (0x5a00) /* ... *&#47;},
 * grouped under {@code /* WDTCTL Control Bits *&#47;}) and msp430&lt;mcu&gt;_symbols.ld
 * ({@code PROVIDE(WDTCTL = 0x01CC);}), and the interrupt vectors' sections from msp430&lt;mcu&gt;.ld
 * ({@code __interrupt_vector_25 : { KEEP (*(__interrupt_vector_25)) KEEP (*(__interrupt_vector_port1)) } > VECT25}).
 */
public final class DeviceInfo {

    /** A peripheral register. */
    public static final class Register {
        public final String name;
        /** "byte", "word" or "address" (20-bit). */
        public final String size;
        public final String description;
        /** Its address, e.g. "0x01CC", or null. */
        public final String address;

        Register(String name, String size, String description, String address) {
            this.name = name;
            this.size = size;
            this.description = description;
            this.address = address;
        }
    }

    /** A constant: a register bit or field value, or a general one such as BIT0. */
    public static final class Constant {
        public final String name;
        public final String value;
        public final String description;
        /** The register its bits belong to, or null. */
        public final String register;

        Constant(String name, String value, String description, String register) {
            this.name = name;
            this.value = value;
            this.description = description;
            this.register = register;
        }
    }

    /** An interrupt vector: the sections that put a routine's address in it. */
    public static final class Vector {
        public final int number;
        /** The section by number, e.g. "__interrupt_vector_25". */
        public final String section;
        /** The section by name, e.g. "__interrupt_vector_port1", or null if the linker script gives none. */
        public final String namedSection;
        /** The header's name for it, e.g. "PORT1_VECTOR", or null. */
        public final String headerName;
        /** Its address, e.g. "0xFFD4", or null. */
        public final String address;

        Vector(int number, String section, String namedSection, String headerName, String address) {
            this.number = number;
            this.section = section;
            this.namedSection = namedSection;
            this.headerName = headerName;
            this.address = address;
        }

        /** The section to use: the named one if there is one. */
        public String preferred() {
            return namedSection != null ? namedSection : section;
        }
    }

    private static final Pattern VECTOR_SECTION = Pattern.compile(
            "^\\s*(__interrupt_vector_(\\d+))\\s*:\\s*\\{(.*)\\}\\s*>\\s*(\\w+)");
    private static final Pattern KEEP = Pattern.compile("KEEP\\s*\\(\\s*\\*\\s*\\(\\s*(\\w+)\\s*\\)\\s*\\)");
    private static final Pattern MEMORY = Pattern.compile("^\\s*(\\w+)\\s*:\\s*ORIGIN\\s*=\\s*(0x[0-9A-Fa-f]+)");

    private static final Pattern SFR = Pattern.compile("^sfr_([bwa])\\((\\w+)\\);\\s*(?:/\\*\\s*(.*?)\\s*\\*/)?");
    private static final Pattern DEFINE = Pattern.compile(
            "^#define\\s+(\\w+)\\s+(\\([^)]*\\)|\\S+)\\s*(?:/\\*\\s*(.*?)\\s*\\*/)?\\s*$");
    private static final Pattern GROUP = Pattern.compile("^/\\*\\s*(\\w+) Control Bits\\s*\\*/");
    private static final Pattern PROVIDE = Pattern.compile("^PROVIDE\\((\\w+)\\s*=\\s*(0x[0-9A-Fa-f]+)\\);");

    private static final Map<String, DeviceInfo> CACHE = new ConcurrentHashMap<>();

    public final Map<String, Register> registers;
    public final Map<String, Constant> constants;
    /** The interrupt vectors, by number. */
    public final List<Vector> vectors;

    private DeviceInfo(Map<String, Register> registers, Map<String, Constant> constants, List<Vector> vectors) {
        this.registers = Collections.unmodifiableMap(registers);
        this.constants = Collections.unmodifiableMap(constants);
        this.vectors = Collections.unmodifiableList(vectors);
    }

    /** The vector a section name puts a routine in (by number or by name), or null. */
    public Vector vector(String section) {
        for (Vector v : vectors) {
            if (v.section.equals(section) || section.equals(v.namedSection)) {
                return v;
            }
        }
        return null;
    }

    /** An empty database, for when the support files aren't there. */
    public static DeviceInfo empty() {
        return new DeviceInfo(new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
    }

    /** The device's database from an include folder (cached), or an empty one if its files are missing. */
    public static DeviceInfo load(Path include, String mcu) {
        if (include == null) {
            return empty();
        }
        return CACHE.computeIfAbsent(include + "|" + mcu, k -> read(include, mcu));
    }

    static DeviceInfo read(Path include, String mcu) {
        Path header = include.resolve(mcu + ".h");
        Path symbols = include.resolve(mcu + "_symbols.ld");
        if (!Files.isRegularFile(header)) {
            return empty();
        }
        try {
            Map<String, String> addresses = new LinkedHashMap<>();
            if (Files.isRegularFile(symbols)) {
                for (String line : Files.readAllLines(symbols, StandardCharsets.ISO_8859_1)) {
                    Matcher m = PROVIDE.matcher(line);
                    if (m.find()) {
                        addresses.put(m.group(1), "0x" + m.group(2).substring(2).toUpperCase());
                    }
                }
            }
            Map<String, Register> registers = new LinkedHashMap<>();
            Map<String, Constant> constants = new LinkedHashMap<>();
            String group = null;
            for (String line : Files.readAllLines(header, StandardCharsets.ISO_8859_1)) {
                Matcher m = SFR.matcher(line);
                if (m.find()) {
                    String name = m.group(2);
                    if (name.endsWith("_L") || name.endsWith("_H")) {
                        continue;  // the byte halves of word registers
                    }
                    String size = m.group(1).equals("b") ? "byte" : m.group(1).equals("w") ? "word" : "address";
                    registers.putIfAbsent(name, new Register(name, size,
                            m.group(3) == null ? "" : m.group(3), addresses.get(name)));
                    continue;
                }
                m = GROUP.matcher(line);
                if (m.find()) {
                    group = m.group(1);
                    continue;
                }
                m = DEFINE.matcher(line);
                if (m.find()) {
                    String name = m.group(1);
                    if (name.endsWith("_L") || name.endsWith("_H") || name.startsWith("__")
                            || name.endsWith("_VECTOR") && m.group(2).startsWith("(\"")) {
                        continue;
                    }
                    String value = m.group(2);
                    if (value.startsWith("(") && value.endsWith(")")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    constants.putIfAbsent(name, new Constant(name, value, m.group(3) == null ? "" : m.group(3),
                            group != null && registers.containsKey(group) ? group : null));
                }
            }
            return new DeviceInfo(registers, constants, vectors(include.resolve(mcu + ".ld"), constants));
        } catch (IOException e) {
            return empty();
        }
    }

    /** The vectors in a linker script, with the header's names for them (PORT1_VECTOR = 25). */
    static List<Vector> vectors(Path linkerScript, Map<String, Constant> constants) throws IOException {
        List<Vector> out = new ArrayList<>();
        if (!Files.isRegularFile(linkerScript)) {
            return out;
        }
        Map<Integer, String> headerNames = new HashMap<>();
        for (Constant c : constants.values()) {
            if (c.name.endsWith("_VECTOR") && c.value.matches("\\d+")) {
                headerNames.putIfAbsent(Integer.parseInt(c.value), c.name);
            }
        }
        List<String> lines = Files.readAllLines(linkerScript, StandardCharsets.ISO_8859_1);
        Map<String, String> origins = new HashMap<>();
        for (String line : lines) {
            Matcher m = MEMORY.matcher(line);
            if (m.find()) {
                origins.put(m.group(1), "0x" + m.group(2).substring(2).toUpperCase());
            }
        }
        for (String line : lines) {
            Matcher m = VECTOR_SECTION.matcher(line);
            if (!m.find()) {
                continue;
            }
            int number = Integer.parseInt(m.group(2));
            String named = null;
            Matcher k = KEEP.matcher(m.group(3));
            while (k.find()) {
                if (!k.group(1).equals(m.group(1))) {
                    named = k.group(1);
                }
            }
            out.add(new Vector(number, m.group(1), named, headerNames.get(number), origins.get(m.group(4))));
        }
        return out;
    }
}
