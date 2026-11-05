/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.debug;

import org.openide.text.Annotatable;
import org.openide.text.Annotation;
import org.openide.util.NbBundle.Messages;

/** The line the program is stopped on: the debugger's usual green line and arrow. */
@Messages("TTP_CurrentPC=The program is stopped here")
final class CurrentLineAnnotation extends Annotation {

    CurrentLineAnnotation(Annotatable line) {
        attach(line);
    }

    @Override
    public String getAnnotationType() {
        return "CurrentPC";
    }

    @Override
    public String getShortDescription() {
        return Bundle.TTP_CurrentPC();
    }
}
