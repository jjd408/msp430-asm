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

import io.github.jjd408.msp430asm.Msp430Asm;
import java.util.Collection;
import java.util.EnumSet;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Language;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/** The MSP430 assembly language, registered for its MIME type so the editor colours it. */
public final class Msp430LanguageHierarchy extends LanguageHierarchy<Msp430TokenId> {

    private static final Language<Msp430TokenId> LANGUAGE = new Msp430LanguageHierarchy().language();

    @MimeRegistration(mimeType = Msp430Asm.MIME_TYPE, service = Language.class)
    public static Language<Msp430TokenId> getLanguage() {
        return LANGUAGE;
    }

    @Override
    protected Collection<Msp430TokenId> createTokenIds() {
        return EnumSet.allOf(Msp430TokenId.class);
    }

    @Override
    protected Lexer<Msp430TokenId> createLexer(LexerRestartInfo<Msp430TokenId> info) {
        return new Msp430Lexer(info);
    }

    @Override
    protected String mimeType() {
        return Msp430Asm.MIME_TYPE;
    }
}
