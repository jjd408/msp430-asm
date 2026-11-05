/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.assemble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PreprocessorTest {

    private static final String SOURCE = String.join("\n",
            "; a routine",
            "#include <msp430.h>",
            "        .text",
            "        .global sysinit",
            "sysinit: mov.w #WDTPW|WDTHOLD, &WDTCTL",
            "        ret",
            "");

    @Test
    void findsDirectivesThatWontRun() {
        List<Diagnostic> d = Preprocessor.ignoredDirectives(SOURCE, "system.s");
        assertEquals(1, d.size());
        assertEquals(2, d.get(0).line);
        assertTrue(d.get(0).message.contains("Rename the file system.S"), d.get(0).message);
        String s43 = Preprocessor.ignoredDirectives("#define X 1\n", "system.s43").get(0).message;
        assertTrue(s43.contains("Rename the file system.S43"), s43);
        assertTrue(Preprocessor.ignoredDirectives("; #include in a comment\n mov #1, r4 ; #define\n", "a.s")
                .isEmpty());
        assertTrue(Preprocessor.runsOn("S"));
        assertFalse(Preprocessor.runsOn("s"));
    }

    @Test
    void buildSaysWhyFirst(@TempDir Path dir) throws Exception {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null && tc.include() != null, "MSP430-GCC isn't installed");
        Path main = dir.resolve("main.S");
        Files.writeString(main, "#include <msp430.h>\n .text\n .global main\nmain: call #sysinit\n jmp main\n");
        Path sys = dir.resolve("system.s");
        Files.writeString(sys, SOURCE);
        List<String> log = new ArrayList<>();
        Build.Result r = new Build(tc, "msp430fr2355").buildAll(List.of(main, sys), List.of(dir), "",
                dir.resolve("build"), "t", log::add);
        assertFalse(r.ok);
        assertTrue(log.get(0).startsWith(sys + ":2: error: #include is ignored"), log.get(0));
        assertEquals(2, r.byFile.get(sys).get(0).line);
        assertTrue(r.byFile.get(main).isEmpty());

        Path fixed = dir.resolve("system.S");
        Files.move(sys, fixed);
        log.clear();
        assertTrue(new Build(tc, "msp430fr2355").buildAll(List.of(main, fixed), List.of(dir), "",
                dir.resolve("build"), "t", log::add).ok, String.join("\n", log));
    }
}
