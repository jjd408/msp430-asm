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

import java.util.List;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.ui.EngineComponentsProvider;

/** The windows an MSP430 session opens: Variables (registers and peripherals), Watches and Breakpoints. */
@DebuggerServiceRegistration(path = Msp430Debugger.SESSION, types = {EngineComponentsProvider.class})
public final class Msp430EngineComponents implements EngineComponentsProvider {

    @Override
    public List<ComponentInfo> getComponents() {
        return List.of(ComponentInfo.create("localsView", true, false),
                ComponentInfo.create("watchesView", true, false),
                ComponentInfo.create("breakpointsView", true, false));
    }

    @Override
    public void willCloseNotify(List<ComponentInfo> components) {
    }
}
