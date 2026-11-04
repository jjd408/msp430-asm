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

import io.github.jjd408.msp430asm.lexer.Msp430Isa;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reference help for MSP430 and MSP430X instructions, registers and GNU as directives, from the MSP430FR4xx/FR2xx
 * family user's guide (SLAU445), chapter 4, and the GNU as manual.
 */
public final class InstructionHelp {

    /** One mnemonic's help. */
    public static final class Entry {
        public final String mnemonic;
        public final String syntax;
        public final String operation;
        /** Status bits V N Z C: '*' set by the result, '-' unchanged, '0' cleared, '1' set. */
        public final String flags;
        /** Sizes: some of "B", "W", "A". */
        public final String sizes;
        /** What an emulated instruction assembles as, or null. */
        public final String emulation;
        public final String group;

        Entry(String mnemonic, String group, String syntax, String operation, String flags, String sizes,
                String emulation) {
            this.mnemonic = mnemonic;
            this.group = group;
            this.syntax = syntax;
            this.operation = operation;
            this.flags = flags;
            this.sizes = sizes;
            this.emulation = emulation;
        }
    }

    private static final Map<String, Entry> INSTRUCTIONS = new LinkedHashMap<>();
    private static final Map<String, String> DIRECTIVES = new LinkedHashMap<>();
    private static final Map<String, String> REGISTERS = new LinkedHashMap<>();

    private static void core(String m, String syntax, String op, String flags, String sizes) {
        INSTRUCTIONS.put(m, new Entry(m, "core", syntax, op, flags, sizes, null));
    }

    private static void emulated(String m, String syntax, String op, String flags, String sizes, String as) {
        INSTRUCTIONS.put(m, new Entry(m, "emulated", syntax, op, flags, sizes, as));
    }

    private static void x(String m, String syntax, String op, String flags, String sizes) {
        INSTRUCTIONS.put(m, new Entry(m, "MSP430X", syntax, op, flags, sizes, null));
    }

    static {
        // Core: double operand
        core("mov", "MOV src, dst", "dst = src", "----", "BW");
        core("add", "ADD src, dst", "dst = dst + src", "****", "BW");
        core("addc", "ADDC src, dst", "dst = dst + src + C", "****", "BW");
        core("sub", "SUB src, dst", "dst = dst - src (dst + ~src + 1)", "****", "BW");
        core("subc", "SUBC src, dst", "dst = dst - src - 1 + C (dst + ~src + C)", "****", "BW");
        core("cmp", "CMP src, dst", "dst - src, setting the flags only; dst is unchanged", "****", "BW");
        core("dadd", "DADD src, dst", "dst = src + dst + C, in binary-coded decimal", "****", "BW");
        core("bit", "BIT src, dst", "src AND dst, setting the flags only (C = NOT Z)", "0***", "BW");
        core("bic", "BIC src, dst", "dst = dst AND NOT src: clears the bits set in src", "----", "BW");
        core("bis", "BIS src, dst", "dst = dst OR src: sets the bits set in src", "----", "BW");
        core("xor", "XOR src, dst", "dst = dst XOR src (V set when both operands are negative, C = NOT Z)",
                "****", "BW");
        core("and", "AND src, dst", "dst = dst AND src (C = NOT Z)", "0***", "BW");
        // Core: single operand
        core("rrc", "RRC dst", "Rotate right through carry: C → MSB → … → LSB → C", "****", "BW");
        core("rra", "RRA dst", "Arithmetic shift right: MSB → MSB → … → LSB → C", "0***", "BW");
        core("push", "PUSH src", "SP = SP - 2; @SP = src", "----", "BW");
        core("swpb", "SWPB dst", "Swap the high and low bytes of a word", "----", "W");
        core("call", "CALL dst", "SP = SP - 2; @SP = PC (return address); PC = dst. 16-bit addresses: use CALLA "
                + "beyond 64 KB", "----", "W");
        core("reti", "RETI", "Return from interrupt: SR = @SP+, PC = @SP+ (with PC bits 19:16 from the saved SR "
                + "word on MSP430X)", "****", "");
        core("sxt", "SXT dst", "Sign-extend the low byte into the whole word (bit 7 → bits 15:8)", "0***", "W");
        // Core: jumps (PC-relative, ±512 words)
        core("jmp", "JMP label", "Jump always (PC-relative, -511 to +512 words)", "----", "");
        core("jeq", "JEQ label  (= JZ)", "Jump if equal / zero: Z = 1", "----", "");
        core("jz", "JZ label  (= JEQ)", "Jump if zero / equal: Z = 1", "----", "");
        core("jne", "JNE label  (= JNZ)", "Jump if not equal / not zero: Z = 0", "----", "");
        core("jnz", "JNZ label  (= JNE)", "Jump if not zero / not equal: Z = 0", "----", "");
        core("jc", "JC label  (= JHS)", "Jump if carry: C = 1 (unsigned higher or same)", "----", "");
        core("jhs", "JHS label  (= JC)", "Jump if higher or same (unsigned): C = 1", "----", "");
        core("jnc", "JNC label  (= JLO)", "Jump if no carry: C = 0 (unsigned lower)", "----", "");
        core("jlo", "JLO label  (= JNC)", "Jump if lower (unsigned): C = 0", "----", "");
        core("jn", "JN label", "Jump if negative: N = 1", "----", "");
        core("jge", "JGE label", "Jump if greater or equal (signed): N XOR V = 0", "----", "");
        core("jl", "JL label", "Jump if less (signed): N XOR V = 1", "----", "");

        // Emulated
        emulated("adc", "ADC dst", "dst = dst + C", "****", "BW", "ADDC #0, dst");
        emulated("br", "BR dst", "Branch: PC = dst", "----", "W", "MOV dst, PC");
        emulated("clr", "CLR dst", "dst = 0", "----", "BW", "MOV #0, dst");
        emulated("clrc", "CLRC", "C = 0", "---0", "", "BIC #1, SR");
        emulated("clrn", "CLRN", "N = 0", "-0--", "", "BIC #4, SR");
        emulated("clrz", "CLRZ", "Z = 0", "--0-", "", "BIC #2, SR");
        emulated("dadc", "DADC dst", "dst = dst + C, in binary-coded decimal", "****", "BW", "DADD #0, dst");
        emulated("dec", "DEC dst", "dst = dst - 1", "****", "BW", "SUB #1, dst");
        emulated("decd", "DECD dst", "dst = dst - 2", "****", "BW", "SUB #2, dst");
        emulated("dint", "DINT", "Disable maskable interrupts: GIE = 0 (follow it with a NOP)", "----", "",
                "BIC #8, SR");
        emulated("eint", "EINT", "Enable maskable interrupts: GIE = 1 (put a NOP before it)", "----", "",
                "BIS #8, SR");
        emulated("inc", "INC dst", "dst = dst + 1", "****", "BW", "ADD #1, dst");
        emulated("incd", "INCD dst", "dst = dst + 2", "****", "BW", "ADD #2, dst");
        emulated("inv", "INV dst", "Invert every bit: dst = NOT dst", "****", "BW", "XOR #-1, dst");
        emulated("nop", "NOP", "No operation", "----", "", "MOV #0, R3");
        emulated("pop", "POP dst", "dst = @SP; SP = SP + 2", "----", "BW", "MOV @SP+, dst");
        emulated("ret", "RET", "Return from a CALL: PC = @SP; SP = SP + 2", "----", "", "MOV @SP+, PC");
        emulated("rla", "RLA dst", "Shift left: C ← MSB ← … ← LSB ← 0", "****", "BW", "ADD dst, dst");
        emulated("rlc", "RLC dst", "Rotate left through carry: C ← MSB ← … ← LSB ← C", "****", "BW",
                "ADDC dst, dst");
        emulated("sbc", "SBC dst", "dst = dst - 1 + C (subtract the borrow)", "****", "BW", "SUBC #0, dst");
        emulated("setc", "SETC", "C = 1", "---1", "", "BIS #1, SR");
        emulated("setn", "SETN", "N = 1", "-1--", "", "BIS #4, SR");
        emulated("setz", "SETZ", "Z = 1", "--1-", "", "BIS #2, SR");
        emulated("tst", "TST dst", "Compare with 0, setting the flags only", "0***", "BW", "CMP #0, dst");

        // MSP430X: address-word instructions, multi-bit shifts, push/pop multiple
        x("mova", "MOVA src, dst", "20-bit move between registers and memory (address word)", "----", "A");
        x("cmpa", "CMPA src, Rdst", "20-bit compare: Rdst - src, setting the flags only", "****", "A");
        x("adda", "ADDA src, Rdst", "20-bit add: Rdst = Rdst + src", "****", "A");
        x("suba", "SUBA src, Rdst", "20-bit subtract: Rdst = Rdst - src", "****", "A");
        x("calla", "CALLA dst", "Call with a 20-bit address: pushes the 20-bit return address (two words), PC "
                + "= dst", "----", "A");
        x("reta", "RETA", "Return from a CALLA: pops the 20-bit return address (two words)", "----", "A");
        x("bra", "BRA dst", "20-bit branch: PC = dst (MOVA dst, PC)", "----", "A");
        x("clra", "CLRA Rdst", "Rdst = 0, all 20 bits (MOVA #0, Rdst)", "----", "A");
        x("decda", "DECDA Rdst", "Rdst = Rdst - 2, 20-bit (SUBA #2, Rdst)", "****", "A");
        x("incda", "INCDA Rdst", "Rdst = Rdst + 2, 20-bit (ADDA #2, Rdst)", "****", "A");
        x("tsta", "TSTA Rdst", "20-bit compare with 0 (CMPA #0, Rdst)", "0***", "A");
        x("pushm", "PUSHM.A / PUSHM.W #n, Rdst", "Push n registers (1-16): Rdst, Rdst-1, … Rdst-n+1, in that "
                + "order", "----", "WA");
        x("popm", "POPM.A / POPM.W #n, Rdst", "Pop n registers (1-16): Rdst-n+1 … Rdst, undoing a PUSHM",
                "----", "WA");
        x("rrcm", "RRCM.A / RRCM.W #n, Rdst", "Rotate a register right through carry by n bits (1-4)",
                "0***", "WA");
        x("rram", "RRAM.A / RRAM.W #n, Rdst", "Arithmetic shift of a register right by n bits (1-4)",
                "0***", "WA");
        x("rlam", "RLAM.A / RLAM.W #n, Rdst", "Shift a register left by n bits (1-4)", "****", "WA");
        x("rrum", "RRUM.A / RRUM.W #n, Rdst", "Logical shift of a register right by n bits (1-4): zeros in at "
                + "the top", "0***", "WA");
        x("rrux", "RRUX dst", "Logical shift right by one bit: zero into the MSB", "0***", "BWA");
        // MSP430X extended forms of the core and emulated instructions
        for (String base : new String[] {"mov", "add", "addc", "sub", "subc", "cmp", "dadd", "bit", "bic", "bis",
            "xor", "and", "rrc", "rra", "push", "swpb", "sxt", "adc", "clr", "dadc", "dec", "decd", "inc", "incd",
            "inv", "pop", "rla", "rlc", "sbc", "tst"}) {
            Entry b = INSTRUCTIONS.get(base);
            String m = base + "x";
            INSTRUCTIONS.put(m, new Entry(m, "MSP430X", b.syntax.replaceFirst("^(\\S+)", "$1X"),
                    b.operation + ". Extended form: an extension word lets it work on 20-bit data (.A), reach "
                    + "the whole 1 MB address space, and repeat (RPT) with register operands",
                    b.flags, b.sizes.contains("B") ? "BWA" : "WA", b.emulation == null ? null
                    : b.emulation.replaceFirst("^(\\S+)", "$1X")));
        }

        // Directives (GNU as)
        DIRECTIVES.put(".text", "Assemble into the code section (FRAM on the FR2355)");
        DIRECTIVES.put(".data", "Assemble into the initialized-data section (copied to RAM at start-up)");
        DIRECTIVES.put(".bss", "Assemble into the zero-filled data section (RAM)");
        DIRECTIVES.put(".section", ".section name[, \"flags\"[, @type]]: switch to a named section, e.g. "
                + ".section .persistent or __interrupt_vector_43");
        DIRECTIVES.put(".global", ".global symbol: make a symbol visible to the linker (same as .globl)");
        DIRECTIVES.put(".globl", ".globl symbol: make a symbol visible to the linker");
        DIRECTIVES.put(".local", ".local symbol: keep a symbol local to this file");
        DIRECTIVES.put(".weak", ".weak symbol: a symbol another definition may override");
        DIRECTIVES.put(".byte", ".byte expr, …: 8-bit values");
        DIRECTIVES.put(".word", ".word expr, …: 16-bit values (little-endian)");
        DIRECTIVES.put(".short", ".short expr, …: 16-bit values");
        DIRECTIVES.put(".long", ".long expr, …: 32-bit values");
        DIRECTIVES.put(".ascii", ".ascii \"text\": characters, without a terminating zero");
        DIRECTIVES.put(".asciz", ".asciz \"text\": characters with a terminating zero");
        DIRECTIVES.put(".string", ".string \"text\": characters with a terminating zero");
        DIRECTIVES.put(".align", ".align n: pad to a multiple of n bytes (instructions need even addresses)");
        DIRECTIVES.put(".balign", ".balign n[, fill]: pad to a multiple of n bytes");
        DIRECTIVES.put(".p2align", ".p2align n: pad to a multiple of 2^n bytes");
        DIRECTIVES.put(".equ", ".equ symbol, expr: give a symbol a value");
        DIRECTIVES.put(".set", ".set symbol, expr: give a symbol a value (can be redefined)");
        DIRECTIVES.put(".org", ".org offset: move to an offset in the section");
        DIRECTIVES.put(".skip", ".skip n[, fill]: reserve n bytes");
        DIRECTIVES.put(".space", ".space n[, fill]: reserve n bytes");
        DIRECTIVES.put(".fill", ".fill repeat[, size[, value]]: repeat a value");
        DIRECTIVES.put(".include", ".include \"file\": assemble another file here");
        DIRECTIVES.put(".if", ".if expr … .else … .endif: assemble conditionally");
        DIRECTIVES.put(".ifdef", ".ifdef symbol: assemble if the symbol is defined");
        DIRECTIVES.put(".ifndef", ".ifndef symbol: assemble if the symbol isn't defined");
        DIRECTIVES.put(".else", "The other branch of an .if");
        DIRECTIVES.put(".endif", "Ends an .if");
        DIRECTIVES.put(".macro", ".macro name args … .endm: define a macro");
        DIRECTIVES.put(".endm", "Ends a .macro");
        DIRECTIVES.put(".rept", ".rept n … .endr: repeat the lines between n times");
        DIRECTIVES.put(".endr", "Ends a .rept");
        DIRECTIVES.put(".type", ".type symbol, @function or @object: the symbol's type, for debuggers");
        DIRECTIVES.put(".size", ".size symbol, expr: the symbol's size, for debuggers");
        DIRECTIVES.put(".comm", ".comm symbol, size: a common (shared, zeroed) variable");
        DIRECTIVES.put(".lcomm", ".lcomm symbol, size: a local zeroed variable");
        DIRECTIVES.put(".end", "End of the source; anything after it is ignored");

        // Registers
        REGISTERS.put("pc", "R0 / PC: the program counter (20 bits on MSP430X)");
        REGISTERS.put("sp", "R1 / SP: the stack pointer; the stack grows down, in words");
        REGISTERS.put("sr", "R2 / SR: the status register (C, Z, N, GIE, CPUOFF, OSCOFF, SCG0, SCG1, V); as a "
                + "source it's also constant generator 1 (#4, #8, and &addr)");
        REGISTERS.put("cg1", "R2 as constant generator 1: #4 and #8, and absolute addressing (&addr)");
        REGISTERS.put("cg2", "R3, constant generator 2: #0, #1, #2 and #-1 without an extra word");
        REGISTERS.put("r0", REGISTERS.get("pc"));
        REGISTERS.put("r1", REGISTERS.get("sp"));
        REGISTERS.put("r2", REGISTERS.get("sr"));
        REGISTERS.put("r3", REGISTERS.get("cg2"));
        for (int i = 4; i <= 15; i++) {
            String use = i <= 10 ? "kept across calls by msp430-gcc (callee-saved)"
                    : i == 11 ? "free to change in a function (caller-saved)"
                    : "argument and return value in msp430-gcc (R12 first; caller-saved)";
            REGISTERS.put("r" + i, "R" + i + ": general purpose, 20 bits; " + use);
        }
    }

    private InstructionHelp() {
    }

    /** Help for a mnemonic, with or without a .b/.w/.a suffix, or null. */
    public static Entry instruction(String word) {
        String w = word.toLowerCase(Locale.ROOT);
        if (w.endsWith(".b") || w.endsWith(".w") || w.endsWith(".a")) {
            w = w.substring(0, w.length() - 2);
        }
        return INSTRUCTIONS.get(w);
    }

    public static Map<String, Entry> instructions() {
        return INSTRUCTIONS;
    }

    public static String directive(String word) {
        return DIRECTIVES.get(word.toLowerCase(Locale.ROOT));
    }

    public static Map<String, String> directives() {
        return DIRECTIVES;
    }

    public static String register(String word) {
        return REGISTERS.get(word.toLowerCase(Locale.ROOT));
    }

    /** Whether every mnemonic the lexer knows has help. */
    static boolean coversLexer() {
        for (java.util.Set<String> set : java.util.List.of(Msp430Isa.CORE, Msp430Isa.EMULATED, Msp430Isa.MSP430X)) {
            for (String m : set) {
                if (!INSTRUCTIONS.containsKey(m)) {
                    return false;
                }
            }
        }
        return true;
    }
}
