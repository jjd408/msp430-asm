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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.jjd408.msp430asm.assemble.Toolchain;
import org.junit.jupiter.api.Test;

class HelpTest {

    @Test
    void everyMnemonicTheLexerKnowsHasHelp() {
        assertTrue(InstructionHelp.coversLexer());
    }

    @Test
    void instructionHelpWithAndWithoutSizes() {
        assertEquals("ADD src, dst", InstructionHelp.instruction("ADD.B").syntax);
        assertEquals("****", InstructionHelp.instruction("add").flags);
        assertEquals("MOV #0, dst", InstructionHelp.instruction("clr.w").emulation);
        assertEquals("MOVX src, dst", InstructionHelp.instruction("movx.a").syntax);
        assertNull(InstructionHelp.instruction("movz"));
        String html = HelpText.instruction(InstructionHelp.instruction("bit"));
        assertTrue(html.contains("V cleared") && html.contains("Z set by the result"), html);
    }

    @Test
    void readsTisFilesForTheFr2355() {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null && tc.include() != null, "TI's support files aren't installed");
        DeviceInfo d = DeviceInfo.read(tc.include(), "msp430fr2355");
        DeviceInfo.Register p1out = d.registers.get("P1OUT");
        assertEquals("byte", p1out.size);
        assertEquals("0x0202", p1out.address);
        assertEquals("Port 1 Output", p1out.description);
        assertEquals("0x01CC", d.registers.get("WDTCTL").address);
        assertNull(d.registers.get("WDTCTL_L"), "byte halves aren't listed");
        DeviceInfo.Constant pw = d.constants.get("WDTPW");
        assertEquals("0x5a00", pw.value);
        assertEquals("WDTCTL", pw.register);
        assertEquals("Watchdog timer password", pw.description);
        assertEquals("0x0001", d.constants.get("BIT0").value);
        assertNotNull(d.constants.get("LPM3_bits"));
        assertEquals(342, d.registers.size(), "the header declares 342 registers besides byte halves");
        assertTrue(HelpText.peripheral(d.registers.get("WDTCTL"), d).contains("WDTHOLD"));
    }
}
