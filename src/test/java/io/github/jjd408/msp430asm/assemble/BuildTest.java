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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildTest {

    @Test
    void linkerErrorsUseTheLocLine() {
        List<String> out = List.of(
                "/x/bin/ld: /tmp/cc1.o: in function `.Loc.3.1':",
                "/src/bad.s:3: undefined reference to `undefined_sym'",
                "/x/bin/ld: /tmp/cc1.o: in function `.Loc.4.1':",
                "/src/bad.s:3: undefined reference to `nothere'",
                "/other.s:9: undefined reference to `elsewhere'",
                "collect2: error: ld returned 1 exit status");
        assertEquals(List.of("3: ERROR: undefined reference to `undefined_sym'",
                "4: ERROR: undefined reference to `nothere'"),
                Build.linkerErrors(out, "/src/bad.s").stream().map(Object::toString).collect(Collectors.toList()));
    }

    private static Build build() {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null && tc.include() != null, "MSP430-GCC with TI's support files isn't installed");
        return new Build(tc, "msp430fr2355");
    }

    @Test
    void buildsAHexFile(@TempDir Path dir) throws Exception {
        Path src = dir.resolve("blink.S");
        Files.writeString(src, String.join("\n",
                "#include <msp430.h>",
                "        .text",
                "        .global main",
                "main:   mov.w   #WDTPW|WDTHOLD, &WDTCTL",
                "        bis.b   #BIT0, &P1DIR",
                "loop:   xor.b   #BIT0, &P1OUT",
                "        jmp     loop",
                ""));
        List<String> log = new ArrayList<>();
        Build.Result r = build().build(src, dir.resolve("build"), log::add);
        assertTrue(r.ok, () -> String.join("\n", log));
        assertTrue(r.diagnostics.isEmpty(), r.diagnostics::toString);
        List<String> hex = Files.readAllLines(r.hex);
        assertTrue(hex.get(0).startsWith(":"), "Intel HEX");
        assertEquals(":00000001FF", hex.get(hex.size() - 1));
        assertTrue(Files.isRegularFile(r.elf));
        assertTrue(log.stream().anyMatch(l -> l.startsWith("$ ") && l.contains("msp430-elf-gcc")));
        assertTrue(log.stream().anyMatch(l -> l.contains("text") && l.contains("filename")), "the size table");
    }

    @Test
    void reportsAssemblerAndLinkerErrorsOnTheirLines(@TempDir Path dir) throws Exception {
        Path bad = dir.resolve("bad.s");
        Files.writeString(bad, String.join("\n",
                "        .text",
                "        .global main",
                "main:   mov.w #1, &undefined_sym",
                "        call  #nothere",
                "        ret",
                ""));
        Build.Result r = build().build(bad, dir.resolve("build"), l -> { });
        assertFalse(r.ok);
        assertEquals(List.of("3: ERROR: undefined reference to `undefined_sym'",
                "4: ERROR: undefined reference to `nothere'"),
                r.diagnostics.stream().map(Object::toString).collect(Collectors.toList()));

        Path typo = dir.resolve("typo.asm");
        Files.writeString(typo, "        .text\n        .global main\nmain:   movz r4\n");
        Build.Result t = build().build(typo, dir.resolve("build"), l -> { });
        assertFalse(t.ok);
        assertEquals("3: ERROR: unknown opcode `movz'", t.diagnostics.get(0).toString(), ".asm is assembled too");
    }

    @Test
    void picsimlabCommand() {
        PicSimLab sim = PicSimLab.find(null);
        assumeTrue(sim != null, "picsimlab-msp430 isn't checked out");
        assertEquals(List.of(sim.dir.resolve("run.sh").toString(), "Solitaire", "MSP430FR2355", "/x/fw.hex"),
                sim.command("Solitaire", Path.of("/x/fw.hex")));
    }
}
