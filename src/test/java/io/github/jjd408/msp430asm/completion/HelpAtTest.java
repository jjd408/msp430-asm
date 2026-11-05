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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.jjd408.msp430asm.help.DeviceInfo;
import javax.swing.text.PlainDocument;
import org.junit.jupiter.api.Test;

class HelpAtTest {

    private static PlainDocument doc(String text) throws Exception {
        PlainDocument d = new PlainDocument();
        d.insertString(0, text, null);
        return d;
    }

    @Test
    void wordUnderTheCursor() throws Exception {
        String t = "loop:   xor.b   #BIT0, &P1OUT    ; toggle P1.0\n";
        PlainDocument d = doc(t);
        String h = HelpAt.help(d, t.indexOf("xor") + 1, DeviceInfo.empty());
        assertTrue(h != null && h.contains("XOR"), h);
    }

    @Test
    void elsewhereOnTheLineItsInstruction() throws Exception {
        String t = "loop:   xor.b   #BIT0, &P1OUT    ; toggle P1.0\n        nop\n";
        PlainDocument d = doc(t);
        int between = t.indexOf(", &") + 1;  // the space between the operands
        assertNull(HelpAt.help(d, between, DeviceInfo.empty()));
        String h = HelpAt.lineInstruction(d, between, DeviceInfo.empty());
        assertTrue(h != null && h.contains("XOR"), h);
        String nop = HelpAt.lineInstruction(d, t.indexOf("nop") - 3, DeviceInfo.empty());
        assertTrue(nop != null && nop.contains("NOP"), nop);
        assertNull(HelpAt.lineInstruction(doc("; just a comment\n"), 3, DeviceInfo.empty()));
    }
}
