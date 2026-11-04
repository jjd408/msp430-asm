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

import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.spi.debugger.ui.BreakpointAnnotation;
import org.openide.text.Annotatable;
import org.openide.util.NbBundle.Messages;

/** A breakpoint's mark in the editor's glyph gutter: the debugger's own red square, grey when disabled. */
@Messages({
    "TTP_Breakpoint=Breakpoint at {0}",
    "TTP_DisabledBreakpoint=Disabled breakpoint at {0}",
    "TTP_InvalidBreakpoint=Breakpoint at {0}: {1}"
})
final class Msp430BreakpointAnnotation extends BreakpointAnnotation {

    private final Msp430LineBreakpoint breakpoint;

    Msp430BreakpointAnnotation(Msp430LineBreakpoint breakpoint, Annotatable line) {
        this.breakpoint = breakpoint;
        attach(line);
    }

    @Override
    public String getAnnotationType() {
        if (!breakpoint.isEnabled()) {
            return "DisabledBreakpoint";
        }
        return breakpoint.getValidity() == Breakpoint.VALIDITY.INVALID ? "Breakpoint_broken" : "Breakpoint";
    }

    @Override
    public String getShortDescription() {
        String where = breakpoint.getDisplayName();
        if (breakpoint.getValidity() == Breakpoint.VALIDITY.INVALID) {
            return Bundle.TTP_InvalidBreakpoint(where, breakpoint.getValidityMessage());
        }
        return breakpoint.isEnabled() ? Bundle.TTP_Breakpoint(where) : Bundle.TTP_DisabledBreakpoint(where);
    }

    @Override
    public Breakpoint getBreakpoint() {
        return breakpoint;
    }

    /** Redraws it after its breakpoint was enabled, disabled, or found valid or not. */
    void refresh() {
        firePropertyChange(PROP_ANNOTATION_TYPE, null, getAnnotationType());
        firePropertyChange(PROP_SHORT_DESCRIPTION, null, getShortDescription());
    }
}
