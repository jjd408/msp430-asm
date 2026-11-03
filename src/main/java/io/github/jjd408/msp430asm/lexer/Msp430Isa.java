/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.lexer;

import java.util.Locale;
import java.util.Set;

/**
 * The MSP430 and MSP430X instruction set as the GNU assembler accepts it, for the lexer: mnemonics (core,
 * emulated and MSP430X), registers and assembler directives. From the MSP430FR4xx/FR2xx family user's guide
 * (SLAU445), chapter 4, and the GNU as manual's MSP430 section.
 */
final class Msp430Isa {

    private Msp430Isa() {
    }

    /** The 27 core instructions, including the conditional jumps' alternative names. */
    static final Set<String> CORE = Set.of(
            "mov", "add", "addc", "sub", "subc", "cmp", "dadd", "bit", "bic", "bis", "xor", "and",
            "rrc", "rra", "push", "swpb", "call", "reti", "sxt",
            "jmp", "jeq", "jz", "jne", "jnz", "jc", "jhs", "jnc", "jlo", "jn", "jge", "jl");

    /** Emulated instructions: the assembler turns these into core instructions. */
    static final Set<String> EMULATED = Set.of(
            "adc", "br", "clr", "clrc", "clrn", "clrz", "dadc", "dec", "decd", "dint", "eint", "inc", "incd",
            "inv", "nop", "pop", "ret", "rla", "rlc", "sbc", "setc", "setn", "setz", "tst");

    /** MSP430X additions: extended (X) forms, address-word instructions and the multi-bit shifts. */
    static final Set<String> MSP430X = Set.of(
            "movx", "addx", "addcx", "subx", "subcx", "cmpx", "daddx", "bitx", "bicx", "bisx", "xorx", "andx",
            "rrcx", "rrax", "pushx", "swpbx", "sxtx", "rrux",
            "adcx", "clrx", "dadcx", "decx", "decdx", "incx", "incdx", "invx", "popx", "rlax", "rlcx", "sbcx",
            "tstx",
            "mova", "cmpa", "adda", "suba", "calla", "reta", "bra", "clra", "decda", "incda", "tsta",
            "pushm", "popm", "rrcm", "rram", "rlam", "rrum");

    /** Registers by number and by their special names (R2/SR doubles as constant generator 1, R3 as CG2). */
    static final Set<String> REGISTERS;

    static {
        String[] regs = new String[16 + 5];
        for (int i = 0; i < 16; i++) {
            regs[i] = "r" + i;
        }
        regs[16] = "pc";
        regs[17] = "sp";
        regs[18] = "sr";
        regs[19] = "cg1";
        regs[20] = "cg2";
        REGISTERS = Set.of(regs);
    }

    /** Whether a word is an instruction mnemonic, with or without a .b, .w or .a size suffix. */
    static boolean isInstruction(String word) {
        String w = word.toLowerCase(Locale.ROOT);
        if (w.endsWith(".b") || w.endsWith(".w") || w.endsWith(".a")) {
            w = w.substring(0, w.length() - 2);
        }
        return CORE.contains(w) || EMULATED.contains(w) || MSP430X.contains(w);
    }

    static boolean isRegister(String word) {
        return REGISTERS.contains(word.toLowerCase(Locale.ROOT));
    }
}
