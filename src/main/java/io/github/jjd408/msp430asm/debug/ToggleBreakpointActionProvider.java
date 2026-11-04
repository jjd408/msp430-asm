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

import io.github.jjd408.msp430asm.Msp430Asm;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.spi.debugger.ActionsProvider;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.filesystems.FileObject;
import org.openide.text.Line;
import org.openide.util.WeakListeners;

/**
 * Toggle Line Breakpoint (Ctrl+F8, or a click in the editor's glyph gutter) in an MSP430 assembly file: adds a
 * breakpoint on the caret's line, or removes the one there.
 */
@ActionsProvider.Registration(actions = {"toggleBreakpoint"}, activateForMIMETypes = {Msp430Asm.MIME_TYPE})
public final class ToggleBreakpointActionProvider extends ActionsProviderSupport implements PropertyChangeListener {

    private final EditorContextDispatcher context = EditorContextDispatcher.getDefault();

    public ToggleBreakpointActionProvider() {
        context.addPropertyChangeListener(Msp430Asm.MIME_TYPE,
                WeakListeners.propertyChange(this, Msp430Asm.MIME_TYPE, context));
        update();
    }

    static boolean isAssembly(FileObject fo) {
        return fo != null && Msp430Asm.MIME_TYPE.equals(fo.getMIMEType());
    }

    @Override
    public Set getActions() {
        return Collections.singleton(ActionsManager.ACTION_TOGGLE_BREAKPOINT);
    }

    @Override
    public void doAction(Object action) {
        Line line = context.getCurrentLine();
        if (line == null) {
            return;
        }
        FileObject fo = line.getLookup().lookup(FileObject.class);
        if (!isAssembly(fo)) {
            return;
        }
        int number = line.getLineNumber() + 1;
        String url = fo.toURL().toString();
        DebuggerManager dm = DebuggerManager.getDebuggerManager();
        for (Breakpoint b : dm.getBreakpoints()) {
            if (b instanceof Msp430LineBreakpoint) {
                Msp430LineBreakpoint lb = (Msp430LineBreakpoint) b;
                if (lb.getURL().equals(url) && lb.getLineNumber() == number) {
                    dm.removeBreakpoint(lb);
                    return;
                }
            }
        }
        dm.addBreakpoint(Msp430LineBreakpoint.create(line));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        update();
    }

    private void update() {
        setEnabled(ActionsManager.ACTION_TOGGLE_BREAKPOINT, isAssembly(context.getCurrentFile()));
    }
}
