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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.netbeans.api.debugger.Watch;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.ui.Constants;
import org.netbeans.spi.viewmodel.ModelEvent;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.TableModel;
import org.netbeans.spi.viewmodel.TableModelFilter;
import org.netbeans.spi.viewmodel.UnknownTypeException;

/**
 * Watches' values, as gdb evaluates their expressions at each stop: variables of C code, symbols
 * ({@code *(unsigned *)&counter}), memory ({@code *(unsigned char *)0x0202}) or registers ({@code $r12}).
 */
@DebuggerServiceRegistration(path = Msp430Debugger.SESSION + "/WatchesView", types = {TableModelFilter.class})
public final class WatchesModel implements TableModelFilter, PropertyChangeListener {

    private final Msp430Debugger debugger;
    private final List<ModelListener> listeners = new CopyOnWriteArrayList<>();

    public WatchesModel(ContextProvider context) {
        debugger = context.lookupFirst(null, Msp430Debugger.class);
        debugger.addPropertyChangeListener(this);
    }

    @Override
    public Object getValueAt(TableModel original, Object node, String columnID) throws UnknownTypeException {
        if (node instanceof Watch) {
            if (Constants.WATCH_VALUE_COLUMN_ID.equals(columnID)) {
                if (debugger.getState() != Msp430Debugger.State.STOPPED) {
                    return "";
                }
                String v = debugger.getSnapshot().watches.get(((Watch) node).getExpression());
                if (v == null) {
                    debugger.refreshWatches();  // new since the stop
                    return "...";
                }
                return v;
            }
            if (Constants.WATCH_TYPE_COLUMN_ID.equals(columnID)) {
                return "";
            }
        }
        return original.getValueAt(node, columnID);
    }

    @Override
    public boolean isReadOnly(TableModel original, Object node, String columnID) throws UnknownTypeException {
        if (node instanceof Watch && Constants.WATCH_VALUE_COLUMN_ID.equals(columnID)) {
            return true;
        }
        return original.isReadOnly(node, columnID);
    }

    @Override
    public void setValueAt(TableModel original, Object node, String columnID, Object value)
            throws UnknownTypeException {
        original.setValueAt(node, columnID, value);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        for (ModelListener l : listeners) {
            l.modelChanged(new ModelEvent.TreeChanged(this));
        }
    }

    @Override
    public void addModelListener(ModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeModelListener(ModelListener l) {
        listeners.remove(l);
    }
}
