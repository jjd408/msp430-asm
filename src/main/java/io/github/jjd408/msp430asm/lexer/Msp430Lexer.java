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

import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;

/**
 * Lexer for MSP430 assembly in GNU as syntax (msp430-elf-as), including .S files that go through the C
 * preprocessor first.
 *
 * <p>A statement is an optional label, then a mnemonic (an instruction, a directive or a macro), then operands.
 * The lexer keeps two flags across tokens, as its restart state: whether the line has had anything but
 * whitespace yet (a # there starts a preprocessor line; elsewhere it marks an immediate operand), and whether
 * the statement's mnemonic has been seen (the first word is the mnemonic; later words are operands).
 *
 * <p>Comments: ; to the end of the line (as's comment character for the MSP430), and // and /* *&#47;, which
 * the C preprocessor removes from .S files.
 */
final class Msp430Lexer implements Lexer<Msp430TokenId> {

    private static final int AT_LINE_START = 1;
    private static final int MNEMONIC_SEEN = 2;

    private final LexerInput input;
    private final TokenFactory<Msp430TokenId> factory;
    private int flags;

    Msp430Lexer(LexerRestartInfo<Msp430TokenId> info) {
        input = info.input();
        factory = info.tokenFactory();
        flags = info.state() == null ? AT_LINE_START : (Integer) info.state();
    }

    @Override
    public Token<Msp430TokenId> nextToken() {
        int c = input.read();
        if (c == LexerInput.EOF) {
            return null;
        }
        Msp430TokenId id = token(c);
        if (id == null) {
            return null;
        }
        if (id != Msp430TokenId.WHITESPACE && id != Msp430TokenId.COMMENT) {
            flags &= ~AT_LINE_START;
        }
        return factory.createToken(id);
    }

    private Msp430TokenId token(int c) {
        switch (c) {
            case '\n':
            case '\r':
                if (c == '\r' && input.read() != '\n') {
                    input.backup(1);
                }
                flags = AT_LINE_START;  // a new statement
                return Msp430TokenId.WHITESPACE;
            case ';':
                toEndOfLine();
                return Msp430TokenId.COMMENT;
            case '/': {
                int n = input.read();
                if (n == '/') {
                    toEndOfLine();
                    return Msp430TokenId.COMMENT;
                }
                if (n == '*') {
                    blockComment();
                    return Msp430TokenId.COMMENT;
                }
                if (n != LexerInput.EOF) {
                    input.backup(1);
                }
                return Msp430TokenId.OPERATOR;
            }
            case '#':
                if ((flags & AT_LINE_START) != 0) {
                    preprocessorLine();
                    return Msp430TokenId.PREPROCESSOR;
                }
                return Msp430TokenId.ADDRESSING;
            case '&':
            case '@':
                return Msp430TokenId.ADDRESSING;
            case '"':
                string();
                return Msp430TokenId.STRING;
            case '\'':
                character();
                return Msp430TokenId.CHAR;
            default:
                break;
        }
        if (c == ' ' || c == '\t' || c == '\f') {
            while (true) {
                int n = input.read();
                if (n != ' ' && n != '\t' && n != '\f') {
                    if (n != LexerInput.EOF) {
                        input.backup(1);
                    }
                    return Msp430TokenId.WHITESPACE;
                }
            }
        }
        if (c >= '0' && c <= '9') {
            // Numbers in any as/IAR form (0x1F, 0b101, 0FFh), and local label references (1f, 1b)
            readWhile(Msp430Lexer::isIdentifierPart);
            if (label()) {
                return Msp430TokenId.LABEL;  // a local label, "1:"
            }
            return Msp430TokenId.NUMBER;
        }
        if (isIdentifierStart(c)) {
            readWhile(Msp430Lexer::isIdentifierPart);
            String word = input.readText().toString();
            if (label()) {
                return Msp430TokenId.LABEL;
            }
            if ((flags & MNEMONIC_SEEN) == 0) {
                flags |= MNEMONIC_SEEN;
                if (word.startsWith(".")) {
                    return Msp430TokenId.DIRECTIVE;
                }
                if (Msp430Isa.isInstruction(word)) {
                    return Msp430TokenId.INSTRUCTION;
                }
                return Msp430TokenId.IDENTIFIER;  // a macro, or a mnemonic as doesn't know
            }
            return Msp430Isa.isRegister(word) ? Msp430TokenId.REGISTER : Msp430TokenId.IDENTIFIER;
        }
        if ("+-*%()[]{},=<>|^~!?".indexOf(c) >= 0) {
            return Msp430TokenId.OPERATOR;
        }
        return Msp430TokenId.ERROR;
    }

    /** A word followed by ':' before the mnemonic is a label; the colon is part of it. */
    private boolean label() {
        if ((flags & MNEMONIC_SEEN) != 0) {
            return false;
        }
        int n = input.read();
        if (n == ':') {
            return true;
        }
        if (n != LexerInput.EOF) {
            input.backup(1);
        }
        return false;
    }

    private void toEndOfLine() {
        while (true) {
            int n = input.read();
            if (n == '\n' || n == '\r' || n == LexerInput.EOF) {
                if (n != LexerInput.EOF) {
                    input.backup(1);
                }
                return;
            }
        }
    }

    private void blockComment() {
        int prev = 0;
        while (true) {
            int n = input.read();
            if (n == LexerInput.EOF || (prev == '*' && n == '/')) {
                return;
            }
            prev = n;
        }
    }

    /** A preprocessor line runs to the end of the line, continued by a backslash before the newline. */
    private void preprocessorLine() {
        int prev = 0;
        while (true) {
            int n = input.read();
            if (n == LexerInput.EOF) {
                return;
            }
            if ((n == '\n' || n == '\r') && prev != '\\') {
                input.backup(1);
                return;
            }
            if (n == '\r' && prev == '\\') {
                if (input.read() != '\n') {
                    input.backup(1);
                }
                n = '\n';
            }
            prev = n;
        }
    }

    private void string() {
        while (true) {
            int n = input.read();
            if (n == '"' || n == LexerInput.EOF) {
                return;
            }
            if (n == '\n' || n == '\r') {
                input.backup(1);  // unterminated: stop at the end of the line
                return;
            }
            if (n == '\\' && input.read() == LexerInput.EOF) {
                return;
            }
        }
    }

    /** as writes a character constant as 'A; a closing quote ('A') is accepted too. */
    private void character() {
        int n = input.read();
        if (n == '\\') {
            n = input.read();
        }
        if (n == LexerInput.EOF || n == '\n' || n == '\r') {
            if (n != LexerInput.EOF) {
                input.backup(1);
            }
            return;
        }
        int close = input.read();
        if (close != '\'' && close != LexerInput.EOF) {
            input.backup(1);
        }
    }

    private interface CharTest {
        boolean test(int c);
    }

    private void readWhile(CharTest test) {
        while (true) {
            int n = input.read();
            if (n == LexerInput.EOF) {
                return;
            }
            if (!test.test(n)) {
                input.backup(1);
                return;
            }
        }
    }

    private static boolean isIdentifierStart(int c) {
        return Character.isLetter(c) || c == '_' || c == '.' || c == '$';
    }

    private static boolean isIdentifierPart(int c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$';
    }

    @Override
    public Object state() {
        return flags;
    }

    @Override
    public void release() {
    }
}
