/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.build;

import io.github.jjd408.msp430asm.file.Msp430AsmDataObject;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/** Builds the MSP430 assembly file and debugs it in PICSimLab, on the board set in Options: breakpoints, stepping. */
@ActionID(category = "Build", id = "io.github.jjd408.msp430asm.build.DebugInPicsimlabAction")
@ActionRegistration(displayName = "#CTL_DebugInPicsimlabAction",
        iconBase = "io/github/jjd408/msp430asm/build/debug.png")
@ActionReferences({
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions", position = 165),
    @ActionReference(path = "Editors/text/x-msp430-asm/Popup", position = 415),
    @ActionReference(path = "Menu/RunProject", position = 1760),
    @ActionReference(path = "Toolbars/Build", position = 615)
})
@Messages("CTL_DebugInPicsimlabAction=Debug in PICSimLab")
public final class DebugInPicsimlabAction implements ActionListener {

    private final Msp430AsmDataObject context;

    public DebugInPicsimlabAction(Msp430AsmDataObject context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        BuildRunner.buildFile(context, BuildRunner.Mode.DEBUG);
    }
}
