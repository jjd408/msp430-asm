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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.debugger.Breakpoint;
import org.junit.jupiter.api.Test;

class BreakpointTest {

    @Test
    void savedBreakpoint() {
        Msp430LineBreakpoint b = Msp430LineBreakpoint.create("file:/home/joe/proj/src/main.S", 12);
        assertEquals("main.S:12", b.getDisplayName());
        assertEquals(12, b.getLineNumber());
        assertTrue(b.isEnabled());
        assertEquals(Breakpoint.VALIDITY.UNKNOWN, b.getValidity());
    }

    @Test
    void enablingTellsListeners() {
        Msp430LineBreakpoint b = Msp430LineBreakpoint.create("file:/x/a.s", 3);
        List<String> events = new ArrayList<>();
        b.addPropertyChangeListener(e -> events.add(e.getPropertyName() + "=" + e.getNewValue()));
        b.disable();
        b.disable();  // no change, no event
        assertFalse(b.isEnabled());
        b.enable();
        assertEquals(List.of("enabled=false", "enabled=true"), events);
    }

    @Test
    void debuggerSetsValidity() {
        Msp430LineBreakpoint b = Msp430LineBreakpoint.create("file:/x/a.s", 3);
        b.setDebuggerValidity(Breakpoint.VALIDITY.INVALID, "No line 3 in a.s");
        assertEquals(Breakpoint.VALIDITY.INVALID, b.getValidity());
        assertEquals("No line 3 in a.s", b.getValidityMessage());
    }
}
