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
import java.util.Set;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.spi.debugger.ActionsProvider;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.netbeans.spi.debugger.ContextProvider;

/** The Debug menu's Continue, Step Over/Into/Out, Pause and Finish Debugger Session, for an MSP430 session. */
@ActionsProvider.Registration(path = Msp430Debugger.SESSION, actions = {"start", "continue", "stepOver", "stepInto",
    "stepOut", "pause", "kill"})
public final class Msp430ActionsProvider extends ActionsProviderSupport implements PropertyChangeListener {

    private static final Set<Object> ACTIONS = Set.of(ActionsManager.ACTION_START, ActionsManager.ACTION_CONTINUE,
            ActionsManager.ACTION_STEP_OVER, ActionsManager.ACTION_STEP_INTO, ActionsManager.ACTION_STEP_OUT,
            ActionsManager.ACTION_PAUSE, ActionsManager.ACTION_KILL);

    private final Msp430Debugger debugger;

    public Msp430ActionsProvider(ContextProvider context) {
        debugger = context.lookupFirst(null, Msp430Debugger.class);
        debugger.addPropertyChangeListener(this);
        update();
    }

    @Override
    public Set getActions() {
        return ACTIONS;
    }

    @Override
    public void doAction(Object action) {
        if (action == ActionsManager.ACTION_START) {
            debugger.start();
        } else if (action == ActionsManager.ACTION_CONTINUE) {
            debugger.resume();
        } else if (action == ActionsManager.ACTION_STEP_OVER) {
            debugger.stepOver();
        } else if (action == ActionsManager.ACTION_STEP_INTO) {
            debugger.stepInto();
        } else if (action == ActionsManager.ACTION_STEP_OUT) {
            debugger.stepOut();
        } else if (action == ActionsManager.ACTION_PAUSE) {
            debugger.pause();
        } else if (action == ActionsManager.ACTION_KILL) {
            debugger.finish();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (Msp430Debugger.PROP_STATE.equals(evt.getPropertyName())) {
            update();
        }
    }

    private void update() {
        Msp430Debugger.State s = debugger.getState();
        boolean stopped = s == Msp430Debugger.State.STOPPED;
        setEnabled(ActionsManager.ACTION_START, true);
        setEnabled(ActionsManager.ACTION_CONTINUE, stopped);
        setEnabled(ActionsManager.ACTION_STEP_OVER, stopped);
        setEnabled(ActionsManager.ACTION_STEP_INTO, stopped);
        setEnabled(ActionsManager.ACTION_STEP_OUT, stopped);
        setEnabled(ActionsManager.ACTION_PAUSE, s == Msp430Debugger.State.RUNNING);
        setEnabled(ActionsManager.ACTION_KILL, s != Msp430Debugger.State.FINISHED);
    }
}
