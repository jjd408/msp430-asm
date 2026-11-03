/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
/** The MSP430 assembly file type, its editor and its new-file template. */
@TemplateRegistration(
        folder = "Other",
        content = "main.s",
        displayName = "#Msp430Template_display",
        description = "Msp430TemplateDescription.html",
        iconBase = "io/github/jjd408/msp430asm/file/msp430.png",
        position = 1900)
@Messages("Msp430Template_display=MSP430 Assembly File")
package io.github.jjd408.msp430asm.file;

import org.netbeans.api.templates.TemplateRegistration;
import org.openide.util.NbBundle.Messages;
