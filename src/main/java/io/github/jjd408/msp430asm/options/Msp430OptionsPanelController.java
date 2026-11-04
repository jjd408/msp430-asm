/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/** Tools &gt; Options &gt; Miscellaneous &gt; MSP430. */
@OptionsPanelController.SubRegistration(
        location = "Advanced",
        displayName = "#AdvancedOption_DisplayName_Msp430",
        keywords = "#AdvancedOption_Keywords_Msp430",
        keywordsCategory = "Advanced/Msp430")
@Messages({
    "AdvancedOption_DisplayName_Msp430=MSP430",
    "AdvancedOption_Keywords_Msp430=msp430 toolchain gcc mcu picsimlab assembly"
})
public final class Msp430OptionsPanelController extends OptionsPanelController {

    private Msp430OptionsPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;

    @Override
    public void update() {
        panel().load();
        changed = false;
    }

    @Override
    public void applyChanges() {
        SwingUtilities.invokeLater(() -> {
            panel().store();
            changed = false;
        });
    }

    @Override
    public void cancel() {
    }

    @Override
    public boolean isValid() {
        return panel().valid();
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return panel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    private Msp430OptionsPanel panel() {
        if (panel == null) {
            panel = new Msp430OptionsPanel(this);
        }
        return panel;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }
}
