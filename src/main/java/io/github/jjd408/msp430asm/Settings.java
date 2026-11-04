/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm;

import io.github.jjd408.msp430asm.assemble.Toolchain;
import io.github.jjd408.msp430asm.help.DeviceInfo;
import org.openide.util.NbPreferences;

/**
 * The module's settings, kept in its preferences until an Options page arrives: {@code toolchainBin}, the folder
 * holding msp430-elf-as (found automatically when empty), and {@code mcu}, the device (msp430fr2355).
 */
public final class Settings {

    public static final String TOOLCHAIN_BIN = "toolchainBin";
    public static final String MCU = "mcu";
    public static final String DEFAULT_MCU = "msp430fr2355";

    private Settings() {
    }

    public static String mcu() {
        return NbPreferences.forModule(Settings.class).get(MCU, DEFAULT_MCU);
    }

    /** The toolchain, or null when it can't be found. */
    public static Toolchain toolchain() {
        return Toolchain.find(NbPreferences.forModule(Settings.class).get(TOOLCHAIN_BIN, ""));
    }

    /** The device's registers and constants from TI's headers (empty when they aren't installed). */
    public static DeviceInfo device() {
        Toolchain tc = toolchain();
        return tc == null ? DeviceInfo.empty() : DeviceInfo.load(tc.include(), mcu());
    }
}
