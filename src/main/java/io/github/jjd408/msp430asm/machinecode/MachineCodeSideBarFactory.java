/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.machinecode;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import org.netbeans.spi.editor.SideBarFactory;

/** Makes the machine code column for MSP430 assembly editors (registered in layer.xml). */
public final class MachineCodeSideBarFactory implements SideBarFactory {

    @Override
    public JComponent createSideBar(JTextComponent target) {
        return new MachineCodeSideBar(target);
    }
}
