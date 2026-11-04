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

import java.io.File;
import javax.swing.Action;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.NodeActionsProvider;
import org.netbeans.spi.viewmodel.NodeActionsProviderFilter;
import org.netbeans.spi.viewmodel.NodeModel;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.text.Line;

/** MSP430 breakpoints in the Breakpoints window: "main.S:12", the usual icons, and double-click to go there. */
@DebuggerServiceRegistration(path = "BreakpointsView", types = {NodeModel.class, NodeActionsProviderFilter.class})
public final class BreakpointsViewModel implements NodeModel, NodeActionsProviderFilter {

    private static final String ICONS = "org/netbeans/modules/debugger/resources/breakpointsView/";

    @Override
    public String getDisplayName(Object node) throws UnknownTypeException {
        if (node instanceof Msp430LineBreakpoint) {
            return ((Msp430LineBreakpoint) node).getDisplayName();
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public String getIconBase(Object node) throws UnknownTypeException {
        if (node instanceof Msp430LineBreakpoint) {
            Msp430LineBreakpoint b = (Msp430LineBreakpoint) node;
            if (!b.isEnabled()) {
                return ICONS + "DisabledBreakpoint";
            }
            return ICONS + (b.getValidity() == Breakpoint.VALIDITY.INVALID ? "Breakpoint_broken" : "Breakpoint");
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public String getShortDescription(Object node) throws UnknownTypeException {
        if (node instanceof Msp430LineBreakpoint) {
            Msp430LineBreakpoint b = (Msp430LineBreakpoint) node;
            File f = b.getFile();
            String where = (f != null ? f.getPath() : b.getURL()) + ":" + b.getLineNumber();
            return b.getValidity() == Breakpoint.VALIDITY.INVALID ? where + " - " + b.getValidityMessage() : where;
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public void performDefaultAction(NodeActionsProvider original, Object node) throws UnknownTypeException {
        if (node instanceof Msp430LineBreakpoint) {
            Line line = ((Msp430LineBreakpoint) node).getLine();
            if (line != null) {
                line.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
            }
            return;
        }
        original.performDefaultAction(node);
    }

    @Override
    public Action[] getActions(NodeActionsProvider original, Object node) throws UnknownTypeException {
        return original.getActions(node);
    }

    @Override
    public void addModelListener(ModelListener l) {
    }

    @Override
    public void removeModelListener(ModelListener l) {
    }
}
