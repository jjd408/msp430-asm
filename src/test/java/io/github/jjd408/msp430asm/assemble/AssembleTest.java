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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AssembleTest {

    // As msp430-elf-as 9.3.1 writes it (tabs before the source)
    private static final String LISTING = String.join("\n",
            "GAS LISTING src.s \t\t\tpage 1",
            "",
            "",
            "   1               \t; test",
            "   2               \t        .text",
            "   4 0000 B240 805A \tmain:   mov.w   #0x5A80, &0x01CC",
            "   4      CC01 ",
            "   6 000a 1D0C      \t        mova    @R12+, R13",
            "   9 0012 3412 7856 \t        .word   0x1234, 0x5678, 0x9ABC",
            "   9      BC9A ",
            "  14 0000 0100      \tv:      .word 1",
            "  20 ???? 0343      \t        nop",
            "  21 0040 41        \t        .byte 'A");

    @Test
    void listingGivesWordsAsTheCpuReadsThem() {
        Map<Integer, Listing.Entry> e = Listing.parse(LISTING);
        assertEquals("0000", e.get(4).address);
        assertEquals("40B2 5A80 01CC", e.get(4).words());
        assertEquals(6, e.get(4).byteCount());
        assertEquals("000A", e.get(6).address);
        assertEquals("0C1D", e.get(6).words());
        assertEquals("1234 5678 9ABC", e.get(9).words());
        assertEquals("0001", e.get(14).words());
        assertNull(e.get(20).address, "???? while the source has errors");
        assertEquals("4303", e.get(20).words());
        assertEquals("41", e.get(21).words(), "an odd byte on its own");
        assertNull(e.get(1), "lines without code have no entry");
        assertNull(e.get(2));
    }

    @Test
    void lineMapFollowsCppMarkers() {
        String cpp = String.join("\n",
                "# 1 \"src.S\"",
                "# 1 \"<built-in>\"",
                "# 1 \"src.S\"",
                "# 1 \"inc/msp430.h\" 1",
                "header line",
                "# 2 \"src.S\" 2",
                "",
                "  .text",
                "main: nop");
        LineMap m = LineMap.parse(cpp, "src.S");
        assertEquals(0, m.sourceLine(5), "header lines aren't from the main file");
        assertEquals(2, m.sourceLine(7));
        assertEquals(3, m.sourceLine(8));
        assertEquals(4, m.sourceLine(9));
    }

    @Test
    void diagnosticsForTheFileAndOthers() {
        List<Diagnostic> d = Diagnostic.parse(String.join("\n",
                "src.s: Assembler messages:",
                "src.s:13: Error: unknown opcode `movz'",
                "src.s:14: Warning: a warning",
                "src.S:3:10: fatal error: missing.h: No such file or directory",
                "inc/x.h:7: Error: in a header"), "src.s");
        assertEquals(4, d.size());
        assertEquals("13: ERROR: unknown opcode `movz'", d.get(0).toString());
        assertEquals("14: WARNING: a warning", d.get(1).toString());
        // messages about other files go on line 1, naming the file
        assertEquals("1: ERROR: src.S:3: missing.h: No such file or directory", d.get(2).toString());
        assertEquals("1: ERROR: inc/x.h:7: in a header", d.get(3).toString());
        List<Diagnostic> c = Diagnostic.parse("src.S:3:10: fatal error: missing.h: No such file or directory",
                "src.S");
        assertEquals("3: ERROR: missing.h: No such file or directory", c.get(0).toString());
    }

    // --- With the real toolchain (skipped when it isn't installed)

    private static Assembler assembler() {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null, "MSP430-GCC isn't installed");
        return new Assembler(tc, "msp430fr2355");
    }

    @Test
    void assemblesPlainSource() throws Exception {
        Assembler.Result r = assembler().assemble(String.join("\n",
                "; test",
                "        .text",
                "main:   mov.w   #0x5A80, &0x01CC",
                "        nop",
                "        movz    r4",
                ""), false, null);
        assertEquals("40B2 5A80 01CC", r.code.get(3).words());
        assertEquals("4303", r.code.get(4).words());
        // an error elsewhere doesn't cost the addresses
        assertEquals("0000", r.code.get(3).address);
        assertEquals("0006", r.code.get(4).address);
        assertEquals(1, r.diagnostics.size());
        assertEquals("5: ERROR: unknown opcode `movz'", r.diagnostics.get(0).toString());
    }

    @Test
    void assemblesPreprocessedSourceWithTisHeader() throws Exception {
        Assembler.Result r = assembler().assemble(String.join("\n",
                "#include <msp430.h>",
                "#define LED BIT0",
                "        .text",
                "main:   bis.b   #LED, &P1DIR",
                "        mov.w   #WDTPW|WDTHOLD, &WDTCTL",
                ""), true, null);
        assertTrue(r.diagnostics.isEmpty(), () -> "no messages: " + r.diagnostics);
        // P1DIR and WDTCTL are resolved by the linker, so their fields are 0000 here
        assertEquals("D3D2 0000", r.code.get(4).words());
        assertEquals("40B2 5A80 0000", r.code.get(5).words());
        assertEquals(2, r.code.size(), "only the two instructions, not the header");
    }

    @Test
    void reportsPreprocessorErrorsOnTheirLine() throws Exception {
        Assembler.Result r = assembler().assemble("\n#include \"missing.h\"\n nop\n", true, null);
        assertEquals(2, r.diagnostics.get(0).line);
        assertTrue(r.diagnostics.get(0).message.contains("missing.h"));
    }
}
