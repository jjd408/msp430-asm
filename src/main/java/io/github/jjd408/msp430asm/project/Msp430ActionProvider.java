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

import io.github.jjd408.msp430asm.Settings;
import io.github.jjd408.msp430asm.assemble.Build;
import io.github.jjd408.msp430asm.assemble.Toolchain;
import io.github.jjd408.msp430asm.build.BuildRunner;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import org.netbeans.spi.project.ActionProvider;
import org.openide.LifecycleManager;
import org.openide.util.Lookup;

/** Build, Clean, Clean and Build, and Run for an MSP430 project. */
final class Msp430ActionProvider implements ActionProvider {

    private static final String[] COMMANDS = {COMMAND_BUILD, COMMAND_CLEAN, COMMAND_REBUILD, COMMAND_RUN};

    private final Msp430Project project;

    Msp430ActionProvider(Msp430Project project) {
        this.project = project;
    }

    @Override
    public String[] getSupportedActions() {
        return COMMANDS.clone();
    }

    @Override
    public boolean isActionEnabled(String command, Lookup context) {
        return List.of(COMMANDS).contains(command);
    }

    @Override
    public void invokeAction(String command, Lookup context) {
        String tab = "MSP430 " + project.name();
        Path dir = project.path();
        boolean clean = COMMAND_CLEAN.equals(command) || COMMAND_REBUILD.equals(command);
        boolean build = !COMMAND_CLEAN.equals(command);
        BuildRunner.start(tab, COMMAND_RUN.equals(command), new BuildRunner.Job() {
            @Override
            public void save() {
                LifecycleManager.getDefault().saveAll();
            }

            @Override
            public Build.Result build(Toolchain tc, Consumer<String> out) throws IOException, InterruptedException {
                if (clean) {
                    ProjectFiles.clean(dir, out);
                }
                if (!build) {
                    return null;
                }
                Properties p = ProjectFiles.load(dir);
                List<Path> sources = ProjectFiles.sources(dir);
                if (sources.isEmpty()) {
                    throw new IOException("there are no sources (.s .S .s43 .asm .c) in " + dir.resolve(
                            ProjectFiles.SRC));
                }
                String mcu = ProjectFiles.get(p, ProjectFiles.MCU, Settings.mcu());
                String cflags = p.getProperty(ProjectFiles.CFLAGS, "");
                return new Build(tc, mcu).buildAll(sources, List.of(dir.resolve(ProjectFiles.SRC)), cflags,
                        dir.resolve(ProjectFiles.BUILD), ProjectFiles.outputName(ProjectFiles.name(dir, p)), out);
            }

            @Override
            public String board() {
                return ProjectFiles.get(ProjectFiles.load(dir), ProjectFiles.BOARD, Settings.board());
            }

            @Override
            public void built() {
                project.getProjectDirectory().refresh();
            }
        });
    }
}
