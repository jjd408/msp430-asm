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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerAdapter;
import org.openide.filesystems.FileObject;
import org.openide.text.AnnotationProvider;
import org.openide.text.Line;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 * Keeps a gutter mark on the line of every MSP430 breakpoint: for files as they're opened (breakpoints saved
 * from an earlier session), and as breakpoints are added, removed, enabled or disabled.
 */
@ServiceProvider(service = AnnotationProvider.class)
public final class BreakpointAnnotationProvider extends DebuggerManagerAdapter implements AnnotationProvider {

    private static final RequestProcessor RP = new RequestProcessor("MSP430 breakpoint marks", 1);

    private final Map<Msp430LineBreakpoint, Msp430BreakpointAnnotation> marks = new HashMap<>();
    private boolean listening;

    @Override
    public void annotate(Line.Set set, Lookup context) {
        FileObject fo = context.lookup(FileObject.class);
        if (!ToggleBreakpointActionProvider.isAssembly(fo)) {
            return;
        }
        listen();
        String url = fo.toURL().toString();
        for (Breakpoint b : DebuggerManager.getDebuggerManager().getBreakpoints()) {
            if (b instanceof Msp430LineBreakpoint && ((Msp430LineBreakpoint) b).getURL().equals(url)) {
                Msp430LineBreakpoint lb = (Msp430LineBreakpoint) b;
                try {
                    lb.setLine(set.getCurrent(lb.getLineNumber() - 1));
                } catch (IndexOutOfBoundsException e) {
                    continue;  // the file is shorter than it was
                }
                mark(lb);
            }
        }
    }

    private synchronized void listen() {
        if (!listening) {
            listening = true;
            DebuggerManager.getDebuggerManager().addDebuggerListener(DebuggerManager.PROP_BREAKPOINTS, this);
        }
    }

    private final PropertyChangeListener changes = (PropertyChangeEvent evt) -> {
        Object source = evt.getSource();
        if (source instanceof Msp430LineBreakpoint) {
            RP.post(() -> {
                Msp430BreakpointAnnotation a;
                synchronized (marks) {
                    a = marks.get(source);
                }
                if (a != null) {
                    a.refresh();
                }
            });
        }
    };

    private void mark(Msp430LineBreakpoint b) {
        Line line = b.getLine();
        if (line == null) {
            return;
        }
        synchronized (marks) {
            Msp430BreakpointAnnotation old = marks.remove(b);
            if (old != null) {
                old.detach();
            } else {
                b.addPropertyChangeListener(changes);
            }
            marks.put(b, new Msp430BreakpointAnnotation(b, line));
        }
    }

    private void unmark(Msp430LineBreakpoint b) {
        synchronized (marks) {
            Msp430BreakpointAnnotation a = marks.remove(b);
            if (a != null) {
                a.detach();
                b.removePropertyChangeListener(changes);
            }
        }
    }

    @Override
    public void breakpointAdded(Breakpoint b) {
        if (b instanceof Msp430LineBreakpoint) {
            RP.post(() -> mark((Msp430LineBreakpoint) b));
        }
    }

    @Override
    public void breakpointRemoved(Breakpoint b) {
        if (b instanceof Msp430LineBreakpoint) {
            RP.post(() -> unmark((Msp430LineBreakpoint) b));
        }
    }
}
