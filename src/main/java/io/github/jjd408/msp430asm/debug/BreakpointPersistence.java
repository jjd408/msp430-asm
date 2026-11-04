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
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.LazyDebuggerManagerListener;
import org.netbeans.api.debugger.Properties;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.Watch;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.openide.modules.OnStop;

/**
 * Saves MSP430 breakpoints with the IDE's debugger settings and restores them in the next session: on every
 * change, and once more when the IDE exits, so lines that moved with edits are saved where they are now.
 */
@DebuggerServiceRegistration(types = {LazyDebuggerManagerListener.class})
public final class BreakpointPersistence implements LazyDebuggerManagerListener {

    static final String KEY = "msp430";

    private static Properties properties() {
        return Properties.getDefault().getProperties("debugger").getProperties(DebuggerManager.PROP_BREAKPOINTS);
    }

    static void save() {
        List<Breakpoint> ours = new ArrayList<>();
        for (Breakpoint b : DebuggerManager.getDebuggerManager().getBreakpoints()) {
            if (b instanceof Msp430LineBreakpoint) {
                ours.add(b);
            }
        }
        properties().setArray(KEY, ours.toArray());
    }

    @Override
    public Breakpoint[] initBreakpoints() {
        Object[] saved = properties().getArray(KEY, new Object[0]);
        List<Breakpoint> out = new ArrayList<>();
        for (Object o : saved) {
            if (o instanceof Msp430LineBreakpoint) {
                ((Msp430LineBreakpoint) o).addPropertyChangeListener(this);
                out.add((Msp430LineBreakpoint) o);
            }
        }
        return out.toArray(new Breakpoint[0]);
    }

    @Override
    public String[] getProperties() {
        return new String[] {DebuggerManager.PROP_BREAKPOINTS_INIT, DebuggerManager.PROP_BREAKPOINTS};
    }

    @Override
    public void breakpointAdded(Breakpoint b) {
        if (b instanceof Msp430LineBreakpoint) {
            b.addPropertyChangeListener(this);
            save();
        }
    }

    @Override
    public void breakpointRemoved(Breakpoint b) {
        if (b instanceof Msp430LineBreakpoint) {
            b.removePropertyChangeListener(this);
            save();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof Msp430LineBreakpoint
                && !Breakpoint.PROP_VALIDITY.equals(evt.getPropertyName())) {
            save();
        }
    }

    @Override
    public void initWatches() {
    }

    @Override
    public void watchAdded(Watch watch) {
    }

    @Override
    public void watchRemoved(Watch watch) {
    }

    @Override
    public void sessionAdded(Session session) {
    }

    @Override
    public void sessionRemoved(Session session) {
    }

    @Override
    public void engineAdded(DebuggerEngine engine) {
    }

    @Override
    public void engineRemoved(DebuggerEngine engine) {
    }

    /** Saves where the lines are now, as the IDE exits. */
    @OnStop
    public static final class SaveOnExit implements Runnable {
        @Override
        public void run() {
            save();
        }
    }

    /** Reads and writes a breakpoint in the settings. */
    @DebuggerServiceRegistration(types = {Properties.Reader.class})
    public static final class Reader implements Properties.Reader {
        @Override
        public String[] getSupportedClassNames() {
            return new String[] {Msp430LineBreakpoint.class.getName()};
        }

        @Override
        public Object read(String className, Properties p) {
            String url = p.getString("url", null);
            int line = p.getInt("lineNumber", 0);
            if (url == null || line < 1) {
                return null;
            }
            Msp430LineBreakpoint b = Msp430LineBreakpoint.create(url, line);
            if (!p.getBoolean(Breakpoint.PROP_ENABLED, true)) {
                b.disable();
            }
            return b;
        }

        @Override
        public void write(Object object, Properties p) {
            Msp430LineBreakpoint b = (Msp430LineBreakpoint) object;
            p.setString("url", b.getURL());
            p.setInt("lineNumber", b.getLineNumber());
            p.setBoolean(Breakpoint.PROP_ENABLED, b.isEnabled());
        }
    }
}
