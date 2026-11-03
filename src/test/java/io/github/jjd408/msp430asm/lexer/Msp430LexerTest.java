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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;

class Msp430LexerTest {

    /** The non-whitespace tokens of the text, as "KIND text". */
    private static List<String> tokens(String text) {
        TokenHierarchy<String> th = TokenHierarchy.create(text, Msp430LanguageHierarchy.getLanguage());
        TokenSequence<Msp430TokenId> ts = th.tokenSequence(Msp430LanguageHierarchy.getLanguage());
        List<String> out = new ArrayList<>();
        StringBuilder all = new StringBuilder();
        while (ts.moveNext()) {
            all.append(ts.token().text());
            if (ts.token().id() != Msp430TokenId.WHITESPACE) {
                out.add(ts.token().id() + " " + ts.token().text());
            }
        }
        assertEquals(text, all.toString(), "tokens cover the text exactly");
        return out;
    }

    @Test
    void instructionWithAddressingModes() {
        assertEquals(List.of("LABEL main:", "INSTRUCTION mov.w", "ADDRESSING #", "NUMBER 0x5A80", "OPERATOR ,",
                "ADDRESSING &", "IDENTIFIER WDTCTL", "COMMENT ; stop the watchdog"),
                tokens("main:   mov.w   #0x5A80, &WDTCTL   ; stop the watchdog"));
    }

    @Test
    void registersIndirectAutoincrementAndMsp430x() {
        assertEquals(List.of("INSTRUCTION MOVA", "ADDRESSING @", "REGISTER R12", "OPERATOR +", "OPERATOR ,",
                "REGISTER r13"), tokens("MOVA @R12+, r13"));
        assertEquals(List.of("INSTRUCTION pushm.a", "ADDRESSING #", "NUMBER 4", "OPERATOR ,", "REGISTER r10"),
                tokens("pushm.a #4, r10"));
        assertEquals(List.of("INSTRUCTION mov", "REGISTER sp", "OPERATOR ,", "REGISTER PC"), tokens("mov sp, PC"));
    }

    @Test
    void indexedOperand() {
        assertEquals(List.of("INSTRUCTION add.b", "NUMBER 2", "OPERATOR (", "REGISTER r5", "OPERATOR )",
                "OPERATOR ,", "REGISTER r6"), tokens("add.b 2(r5), r6"));
    }

    @Test
    void directivesNumbersStringsAndCharacters() {
        assertEquals(List.of("DIRECTIVE .word", "NUMBER 0x1234", "OPERATOR ,", "NUMBER 0b1010", "OPERATOR ,",
                "NUMBER 017", "OPERATOR ,", "NUMBER 0FFh"), tokens(".word 0x1234, 0b1010, 017, 0FFh"));
        assertEquals(List.of("DIRECTIVE .ascii", "STRING \"MSP\\\"430\""), tokens(".ascii \"MSP\\\"430\""));
        assertEquals(List.of("INSTRUCTION mov.b", "ADDRESSING #", "CHAR 'A", "OPERATOR ,", "REGISTER r15"),
                tokens("mov.b #'A, r15"));
        assertEquals(List.of("INSTRUCTION mov.b", "ADDRESSING #", "CHAR 'A'", "OPERATOR ,", "REGISTER r15"),
                tokens("mov.b #'A', r15"));
    }

    @Test
    void localLabelsAndReferences() {
        assertEquals(List.of("LABEL 1:", "INSTRUCTION dec.w", "REGISTER r15"), tokens("1:  dec.w r15"));
        assertEquals(List.of("INSTRUCTION jnz", "NUMBER 1b"), tokens("    jnz 1b"));
        assertEquals(List.of("LABEL .Lloop:", "INSTRUCTION jmp", "IDENTIFIER .Lloop"), tokens(".Lloop: jmp .Lloop"));
    }

    @Test
    void preprocessorOnlyAtLineStart() {
        assertEquals(List.of("PREPROCESSOR #include <msp430.h>", "INSTRUCTION mov", "ADDRESSING #", "NUMBER 1",
                "OPERATOR ,", "REGISTER r4"), tokens("#include <msp430.h>\n  mov #1, r4"));
        assertEquals(List.of("PREPROCESSOR #define LED \\\n    BIT0"), tokens("#define LED \\\n    BIT0"));
        // indented # is still a preprocessor line; after a token it's an immediate
        assertEquals(List.of("PREPROCESSOR #if 1"), tokens("   #if 1"));
    }

    @Test
    void comments() {
        assertEquals(List.of("INSTRUCTION nop", "COMMENT // c++ style"), tokens("nop // c++ style"));
        assertEquals(List.of("COMMENT /* block\n   comment */", "INSTRUCTION ret"), tokens("/* block\n   comment */ ret"));
        assertEquals(List.of("COMMENT ; all comment"), tokens("; all comment"));
    }

    @Test
    void eachLineIsANewStatement() {
        // the second line's first word is a mnemonic again; operand words aren't
        assertEquals(List.of("INSTRUCTION clr", "REGISTER r4", "INSTRUCTION inc", "REGISTER r4"),
                tokens("clr r4\r\ninc r4"));
        assertEquals(List.of("IDENTIFIER MY_MACRO", "REGISTER r4", "OPERATOR ,", "IDENTIFIER clr"),
                tokens("MY_MACRO r4, clr"));
    }

    @Test
    void emulatedAndUnknownMnemonics() {
        assertEquals(List.of("INSTRUCTION setc"), tokens("setc"));
        assertEquals(List.of("INSTRUCTION RETA"), tokens("RETA"));
        assertEquals(List.of("IDENTIFIER movz", "REGISTER r4"), tokens("movz r4"));
    }

    @Test
    void unexpectedCharacterIsAnError() {
        assertEquals(List.of("INSTRUCTION nop", "ERROR `"), tokens("nop `"));
    }
}
