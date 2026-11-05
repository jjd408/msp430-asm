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

import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.spi.debugger.DebuggerEngineProvider;

/**
 * The MSP430 debugger engine of a session; the debugger ends it through its destructor. Registered in
 * META-INF/debugger/MSP430Session (a class, which the registration annotation can't take).
 */
public final class Msp430EngineProvider extends DebuggerEngineProvider {

    private volatile DebuggerEngine.Destructor destructor;

    @Override
    public String[] getLanguages() {
        return new String[] {"MSP430"};
    }

    @Override
    public String getEngineTypeID() {
        return "MSP430Engine";
    }

    @Override
    public Object[] getServices() {
        return new Object[0];
    }

    @Override
    public void setDestructor(DebuggerEngine.Destructor destructor) {
        this.destructor = destructor;
    }

    DebuggerEngine.Destructor getDestructor() {
        return destructor;
    }
}
