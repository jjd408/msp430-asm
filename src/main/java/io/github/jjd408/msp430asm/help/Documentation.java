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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.HtmlBrowser;
import org.openide.util.NbBundle.Messages;

/** Help &gt; MSP430 Documentation: TI's guides for the MSP430 and its tools, opened in the web browser. */
@Messages({
    "CTL_FamilyGuide=MSP430FR4xx/FR2xx Family User's Guide (SLAU445)",
    "CTL_Datasheet=MSP430FR2355 Datasheet",
    "CTL_GccGuide=MSP430-GCC User's Guide (SLAU646)",
    "CTL_AsGuide=GNU Assembler Manual: MSP430"
})
public final class Documentation {

    static final String FAMILY_GUIDE = "https://www.ti.com/lit/pdf/slau445";
    static final String DATASHEET = "https://www.ti.com/lit/ds/symlink/msp430fr2355.pdf";
    static final String GCC_GUIDE = "https://www.ti.com/lit/pdf/slau646";
    static final String AS_GUIDE = "https://sourceware.org/binutils/docs/as/MSP430_002dDependent.html";

    private Documentation() {
    }

    static void open(String url) {
        try {
            HtmlBrowser.URLDisplayer.getDefault().showURLExternal(new URL(url));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @ActionID(category = "Help", id = "io.github.jjd408.msp430asm.help.FamilyGuide")
    @ActionRegistration(displayName = "#CTL_FamilyGuide")
    @ActionReference(path = "Menu/Help/MSP430 Documentation", position = 100)
    public static final class FamilyGuide implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            open(FAMILY_GUIDE);
        }
    }

    @ActionID(category = "Help", id = "io.github.jjd408.msp430asm.help.Datasheet")
    @ActionRegistration(displayName = "#CTL_Datasheet")
    @ActionReference(path = "Menu/Help/MSP430 Documentation", position = 200)
    public static final class Datasheet implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            open(DATASHEET);
        }
    }

    @ActionID(category = "Help", id = "io.github.jjd408.msp430asm.help.GccGuide")
    @ActionRegistration(displayName = "#CTL_GccGuide")
    @ActionReference(path = "Menu/Help/MSP430 Documentation", position = 300)
    public static final class GccGuide implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            open(GCC_GUIDE);
        }
    }

    @ActionID(category = "Help", id = "io.github.jjd408.msp430asm.help.AsGuide")
    @ActionRegistration(displayName = "#CTL_AsGuide")
    @ActionReference(path = "Menu/Help/MSP430 Documentation", position = 400)
    public static final class AsGuide implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            open(AS_GUIDE);
        }
    }
}
