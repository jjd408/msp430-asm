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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.jjd408.msp430asm.assemble.Toolchain;
import io.github.jjd408.msp430asm.help.DeviceInfo;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CompletionTest {

    private static String ctx(String before) {
        Context c = Context.of(before);
        return c.kind + " '" + c.prefix + "'" + (c.mark != 0 ? " " + c.mark : "");
    }

    @Test
    void contextFromTheLineBeforeTheCaret() {
        assertEquals("MNEMONIC 'mo'", ctx("        mo"));
        assertEquals("MNEMONIC ''", ctx("main:   "));
        assertEquals("MNEMONIC 'x'", ctx("1:  x"));
        assertEquals("MNEMONIC '.te'", ctx("   .te"));
        assertEquals("OPERAND 'R1'", ctx("  mov.w R1"));
        assertEquals("OPERAND 'P1' &", ctx("  bis.b #1, &P1"));
        assertEquals("OPERAND 'WDT' #", ctx("  mov #WDT"));
        assertEquals("OPERAND '' @", ctx("  mova @"));
        assertEquals("OPERAND 'lo'", ctx("loop: jmp lo"));
        assertEquals("NONE ''", ctx("  nop ; a comm"));
        assertEquals("NONE ''", ctx("  nop // a comm"));
        assertEquals("NONE ''", ctx("  .ascii \"MSP"));
        assertEquals("NONE ''", ctx("#incl"));
    }

    private static List<String> choices(String before, String text, DeviceInfo d) {
        return Choices.of(Context.of(before), text, d).stream().map(Object::toString).collect(Collectors.toList());
    }

    @Test
    void mnemonicsInEachSizeAndDirectives() {
        List<String> c = choices("  mov", "", DeviceInfo.empty());
        assertTrue(c.containsAll(List.of("INSTRUCTION mov", "INSTRUCTION mov.b", "INSTRUCTION mov.w",
                "INSTRUCTION mova", "INSTRUCTION movx.a")), c.toString());
        assertFalse(c.contains("INSTRUCTION mov.a"), "plain MOV has no .a form");
        assertTrue(choices("  MOV", "", DeviceInfo.empty()).contains("INSTRUCTION MOV.B"), "keeps upper case");
        assertEquals(List.of("DIRECTIVE .word"), choices("  .wo", "", DeviceInfo.empty()));
    }

    @Test
    void operandsRegistersAndLabels() {
        String text = "main: nop\nloop: jmp loop\n";
        List<String> c = choices("  mov r1", text, DeviceInfo.empty());
        assertEquals(List.of("REGISTER r10", "REGISTER r11", "REGISTER r12", "REGISTER r13", "REGISTER r14",
                "REGISTER r15", "REGISTER r1"), c);
        assertEquals(List.of("LABEL loop"), choices("  jmp lo", text, DeviceInfo.empty()));
        assertTrue(choices("  mov @", text, DeviceInfo.empty()).stream().allMatch(s -> s.startsWith("REGISTER")));
        assertTrue(choices("  nop ; R", text, DeviceInfo.empty()).isEmpty());
    }

    @Test
    void peripheralRegistersAndConstantsFromTisHeader() {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null && tc.include() != null, "TI's support files aren't installed");
        DeviceInfo d = DeviceInfo.load(tc.include(), "msp430fr2355");
        List<String> amp = choices("  bis.b #1, &P1O", "", d);
        assertEquals(List.of("PERIPHERAL P1OUT"), amp);
        List<String> hash = choices("  mov #WDTP", "", d);
        assertTrue(hash.contains("CONSTANT WDTPW"), hash.toString());
        assertTrue(hash.stream().noneMatch(s -> s.startsWith("PERIPHERAL")), "after # only constants and labels");
        assertTrue(choices("  mov P", "", d).isEmpty() || choices("  mov P", "", d).stream()
                .noneMatch(s -> s.startsWith("CONSTANT")), "one letter without a mark: not the whole header");

        assertTrue(Choices.helpFor(Context.of("  bis.b #1, &"), "P1OUT", "", d).contains("0x0202"));
        assertTrue(Choices.helpFor(Context.of("  mov #"), "WDTPW", "", d).contains("0x5a00"));
    }

    @Test
    void operandKnowsItsMnemonicAndPosition() {
        Context c = Context.of("        .section __int");
        assertEquals(".section", c.mnemonic);
        assertEquals(0, c.operand);
        Context d = Context.of("vec:    .section __interrupt_vector_port1,\"ax\",@pro");
        assertEquals(".section", d.mnemonic);
        assertEquals(2, d.operand);
        assertEquals("mov.w", Context.of("  mov.w #1, &P1").mnemonic);
    }

    @Test
    void interruptVectorSectionsAfterSection() {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null && tc.include() != null, "TI's support files aren't installed");
        DeviceInfo d = DeviceInfo.load(tc.include(), "msp430fr2355");
        DeviceInfo.Vector port1 = d.vector("__interrupt_vector_port1");
        assertEquals(25, port1.number);
        assertEquals("PORT1_VECTOR", port1.headerName);
        assertEquals("0xFFD4", port1.address);
        assertEquals(port1, d.vector("__interrupt_vector_25"), "by number too");
        assertEquals("__interrupt_vector_timer0_b0", d.vector("__interrupt_vector_43").namedSection);

        // the start of the section name, any part of the vector's name, or the header's name
        assertTrue(choices("  .section __interrupt_vector_po", "", d).contains("SECTION __interrupt_vector_port1"));
        List<String> port = choices("  .section port", "", d);
        assertTrue(port.containsAll(List.of("SECTION __interrupt_vector_port1", "SECTION __interrupt_vector_port4")),
                port.toString());
        assertTrue(choices("  .section TIMER0_B1", "", d).contains("SECTION __interrupt_vector_timer0_b1"));
        assertTrue(choices("  .section .b", "", d).contains("SECTION .bss"));
        assertTrue(choices("  .section ", "", d).size() > 30, "everything when nothing is typed yet");
        assertTrue(choices("  .section port", "", d).stream().noneMatch(s -> s.startsWith("REGISTER")),
                "only sections there");

        // choosing one writes the flags too; hover explains it
        Choices.Choice c = Choices.of(Context.of("  .section port1"), "", d).get(0);
        assertEquals("__interrupt_vector_port1,\"ax\",@progbits", c.insert);
        assertTrue(c.note.contains("PORT1_VECTOR") && c.note.contains("0xFFD4"), c.note);
        assertTrue(Choices.helpFor(Context.of("  .section "), "__interrupt_vector_port1", "", d).contains("0xFFD4"));

        // after the name (the flags), ordinary operand completion again
        assertTrue(choices("  .section __interrupt_vector_port1, po", "", d).stream()
                .noneMatch(s -> s.startsWith("SECTION")));
    }

    @Test
    void hoverHelpByPosition() {
        String text = "main: nop\n  jmp main\n";
        assertTrue(Choices.helpFor(Context.of("  "), "mov.b", text, DeviceInfo.empty()).contains("MOV src, dst"));
        assertTrue(Choices.helpFor(Context.of("  mov "), "R12", text, DeviceInfo.empty()).contains("argument"));
        assertTrue(Choices.helpFor(Context.of("  jmp "), "main", text, DeviceInfo.empty()).contains("line 1"));
        assertTrue(Choices.helpFor(Context.of("  "), ".word", text, DeviceInfo.empty()).contains("16-bit"));
        assertNull(Choices.helpFor(Context.of("  mov "), "clr", text, DeviceInfo.empty()),
                "an operand named like an instruction isn't one");
    }
}
