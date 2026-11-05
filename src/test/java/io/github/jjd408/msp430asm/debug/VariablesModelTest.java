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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.jjd408.msp430asm.assemble.Toolchain;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VariablesModelTest {

    @Test
    void groupsRegistersByModule() {
        assertEquals("P1", VariablesModel.module("P1OUT"));
        assertEquals("TB0", VariablesModel.module("TB0CCR1"));
        assertEquals("UCB1", VariablesModel.module("UCB1CTLW0"));
        assertEquals("MPY32", VariablesModel.module("RESHI"));
        assertEquals("WDT", VariablesModel.module("WDTCTL"));
        assertEquals("ADC", VariablesModel.module("ADCMEM0"));
    }

    @Test
    void cpuRegisters() {
        Map<String, Long> regs = new LinkedHashMap<>();
        regs.put("pc", 0x800AL);
        regs.put("sr", 0x000DL);   // GIE N C
        regs.put("r12", 50000L);
        List<VariablesModel.Node> rows = VariablesModel.cpu(regs);
        assertEquals("PC", rows.get(0).name);
        assertEquals("0x0800A", rows.get(0).value);
        assertEquals("0x000D  GIE N C", rows.get(1).value);
        assertEquals("0x0C350  (50000)", rows.get(2).value);
    }

    @Test
    void peripheralsFromMemory() {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null && tc.include() != null, "TI's support files aren't installed");
        byte[] mem = new byte[Msp430Debugger.PERIPHERALS_TO - Msp430Debugger.PERIPHERALS_FROM];
        mem[0x0202 - Msp430Debugger.PERIPHERALS_FROM] = 0x05;                 // P1OUT
        mem[0x01CC - Msp430Debugger.PERIPHERALS_FROM] = (byte) 0x80;          // WDTCTL = 0x6980
        mem[0x01CD - Msp430Debugger.PERIPHERALS_FROM] = 0x69;
        List<VariablesModel.Node> groups = VariablesModel.peripherals(mem, tc.include(), "msp430fr2355");
        VariablesModel.Node p1 = groups.stream().filter(g -> g.name.equals("P1")).findFirst().orElseThrow();
        VariablesModel.Node out = p1.children.stream().filter(r -> r.name.equals("P1OUT")).findFirst()
                .orElseThrow();
        assertEquals("0x05  (0000 0101)", out.value);
        assertTrue(out.description.contains("0x0202"), out.description);
        VariablesModel.Node wdt = groups.stream().filter(g -> g.name.equals("WDT")).findFirst().orElseThrow();
        assertEquals("0x6980", wdt.children.get(0).value);
    }
}
