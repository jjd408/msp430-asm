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

import io.github.jjd408.msp430asm.assemble.PicSimLab;
import io.github.jjd408.msp430asm.assemble.Toolchain;
import io.github.jjd408.msp430asm.help.DeviceInfo;
import org.openide.util.NbPreferences;

/**
 * The module's settings (Tools &gt; Options &gt; Miscellaneous &gt; MSP430), kept in its preferences: the folder
 * holding msp430-elf-as (found automatically when empty), the device, and the PICSimLab to run builds in and its
 * board.
 */
public final class Settings {

    public static final String TOOLCHAIN_BIN = "toolchainBin";
    public static final String MCU = "mcu";
    public static final String DEFAULT_MCU = "msp430fr2355";
    public static final String PICSIMLAB_DIR = "picsimlabDir";
    public static final String BOARD = "picsimlabBoard";
    public static final String DEFAULT_BOARD = "Solitaire";

    private Settings() {
    }

    public static String mcu() {
        return get(MCU, DEFAULT_MCU);
    }

    /** The toolchain, or null when it can't be found. */
    public static Toolchain toolchain() {
        return Toolchain.find(get(TOOLCHAIN_BIN, ""));
    }

    /** PICSimLab with the MSP430 backend, or null when it can't be found. */
    public static PicSimLab picsimlab() {
        return PicSimLab.find(get(PICSIMLAB_DIR, ""));
    }

    /** The PICSimLab board to run builds on: Solitaire (the handheld) or MSP430_Board. */
    public static String board() {
        return get(BOARD, DEFAULT_BOARD);
    }

    public static String get(String key, String def) {
        return NbPreferences.forModule(Settings.class).get(key, def);
    }

    public static void put(String key, String value) {
        NbPreferences.forModule(Settings.class).put(key, value);
    }

    /** The device's registers and constants from TI's headers (empty when they aren't installed). */
    public static DeviceInfo device() {
        Toolchain tc = toolchain();
        return tc == null ? DeviceInfo.empty() : DeviceInfo.load(tc.include(), mcu());
    }
}
