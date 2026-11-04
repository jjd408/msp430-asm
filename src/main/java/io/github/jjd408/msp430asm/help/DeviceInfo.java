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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A device's peripheral registers and bit constants, read from TI's support files: msp430&lt;mcu&gt;.h
 * ({@code sfr_w(WDTCTL); /* Watchdog Timer Control Register *&#47;} and {@code #define WDTPW (0x5a00) /* ... *&#47;},
 * grouped under {@code /* WDTCTL Control Bits *&#47;}) and msp430&lt;mcu&gt;_symbols.ld
 * ({@code PROVIDE(WDTCTL = 0x01CC);}).
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

    private static final Pattern SFR = Pattern.compile("^sfr_([bwa])\\((\\w+)\\);\\s*(?:/\\*\\s*(.*?)\\s*\\*/)?");
    private static final Pattern DEFINE = Pattern.compile(
            "^#define\\s+(\\w+)\\s+(\\([^)]*\\)|\\S+)\\s*(?:/\\*\\s*(.*?)\\s*\\*/)?\\s*$");
    private static final Pattern GROUP = Pattern.compile("^/\\*\\s*(\\w+) Control Bits\\s*\\*/");
    private static final Pattern PROVIDE = Pattern.compile("^PROVIDE\\((\\w+)\\s*=\\s*(0x[0-9A-Fa-f]+)\\);");

    private static final Map<String, DeviceInfo> CACHE = new ConcurrentHashMap<>();

    public final Map<String, Register> registers;
    public final Map<String, Constant> constants;

    private DeviceInfo(Map<String, Register> registers, Map<String, Constant> constants) {
        this.registers = Collections.unmodifiableMap(registers);
        this.constants = Collections.unmodifiableMap(constants);
    }

    /** An empty database, for when the support files aren't there. */
    public static DeviceInfo empty() {
        return new DeviceInfo(new LinkedHashMap<>(), new LinkedHashMap<>());
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
            return new DeviceInfo(registers, constants);
        } catch (IOException e) {
            return empty();
        }
    }
}
