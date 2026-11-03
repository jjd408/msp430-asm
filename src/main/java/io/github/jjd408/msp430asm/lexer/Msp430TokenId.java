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

import org.netbeans.api.lexer.TokenId;

/** Token kinds of MSP430 assembly. The category names the colours in Tools > Options > Fonts &amp; Colors. */
public enum Msp430TokenId implements TokenId {

    WHITESPACE("whitespace"),
    COMMENT("comment"),
    PREPROCESSOR("preprocessor"),
    LABEL("label"),
    INSTRUCTION("instruction"),
    DIRECTIVE("directive"),
    REGISTER("register"),
    NUMBER("number"),
    STRING("string"),
    CHAR("char"),
    /** The addressing-mode marks: # (immediate), &amp; (absolute), @ (indirect). */
    ADDRESSING("addressing"),
    OPERATOR("operator"),
    IDENTIFIER("identifier"),
    ERROR("error");

    private final String category;

    Msp430TokenId(String category) {
        this.category = category;
    }

    @Override
    public String primaryCategory() {
        return category;
    }
}
