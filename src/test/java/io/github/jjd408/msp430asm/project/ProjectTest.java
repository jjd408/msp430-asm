/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.jjd408.msp430asm.assemble.Build;
import io.github.jjd408.msp430asm.assemble.Toolchain;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileUtil;

class ProjectTest {

    @Test
    void createsAProject(@TempDir Path dir) throws Exception {
        Path p = dir.resolve("Blinky");
        ProjectFiles.create(p, "Blinky");
        assertTrue(ProjectFiles.isProject(p));
        Properties props = ProjectFiles.load(p);
        assertEquals("Blinky", ProjectFiles.name(p, props));
        assertEquals("msp430fr2355", ProjectFiles.get(props, ProjectFiles.MCU, "msp430fr2355"));
        assertEquals(List.of(p.resolve("src/delay.S"), p.resolve("src/main.S")), ProjectFiles.sources(p));
    }

    @Test
    void factoryRecognizesTheMarker() throws Exception {
        org.openide.filesystems.FileObject root = FileUtil.createMemoryFileSystem().getRoot();
        org.openide.filesystems.FileObject proj = root.createFolder("proj");
        org.openide.filesystems.FileObject other = root.createFolder("other");
        proj.createData(ProjectFiles.MARKER);
        other.createFolder(ProjectFiles.MARKER);  // a folder of that name doesn't count
        assertTrue(new Msp430ProjectFactory().isProject(proj));
        assertFalse(new Msp430ProjectFactory().isProject(other));
        assertFalse(new Msp430ProjectFactory().isProject(root));
    }

    @Test
    void picksSources(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("src/lib"));
        for (String f : List.of("a.s", "b.asm", "c.c", "d.h", "e.S43", "lib/f.s43", "notes.txt")) {
            Files.writeString(dir.resolve("src").resolve(f), "");
        }
        assertEquals(List.of("a.s", "b.asm", "c.c", "e.S43", "lib/f.s43"), ProjectFiles.sources(dir).stream()
                .map(s -> dir.resolve("src").relativize(s).toString()).collect(java.util.stream.Collectors.toList()));
        assertEquals("My_Project_1", ProjectFiles.outputName("My Project 1"));
    }

    @Test
    void buildsTheStarterWithACFile(@TempDir Path dir) throws Exception {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null && tc.include() != null, "MSP430-GCC with TI's support files isn't installed");
        ProjectFiles.create(dir, "starter");
        // a C function called from assembly, beside the two starter files
        Files.writeString(dir.resolve("src/twice.c"), "int twice(int x) { return 2 * x; }\n");
        List<String> log = new ArrayList<>();
        Build.Result r = new Build(tc, "msp430fr2355").buildAll(ProjectFiles.sources(dir),
                List.of(dir.resolve("src")), ProjectFiles.DEFAULT_CFLAGS, dir.resolve("build"), "starter", log::add);
        assertTrue(r.ok, String.join("\n", log));
        assertTrue(Files.size(dir.resolve("build/starter.hex")) > 0);
        ProjectFiles.clean(dir, log::add);
        assertFalse(Files.exists(dir.resolve("build")));
    }

    @Test
    void reportsErrorsInTheRightFile(@TempDir Path dir) throws Exception {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null && tc.include() != null, "MSP430-GCC with TI's support files isn't installed");
        ProjectFiles.create(dir, "broken");
        Path delay = dir.resolve("src/delay.S");
        Files.writeString(delay, Files.readString(delay).replace("dec.w   R12", "dec.w   R99"));
        Build.Result r = new Build(tc, "msp430fr2355").buildAll(ProjectFiles.sources(dir),
                List.of(dir.resolve("src")), "", dir.resolve("build"), "broken", s -> { });
        assertFalse(r.ok);
        assertEquals(7, r.byFile.get(delay).get(0).line);
        assertTrue(r.byFile.get(dir.resolve("src/main.S")).isEmpty());
    }
}
