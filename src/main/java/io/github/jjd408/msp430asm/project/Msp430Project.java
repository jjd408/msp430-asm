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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Path;
import java.util.Properties;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.support.GenericSources;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.RecommendedTemplates;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/** An MSP430 project: the sources under src/, built together into one program. */
public final class Msp430Project implements Project {

    /** Template categories: MSP430 sources, and plain files. */
    static final String CATEGORY = "msp430";

    private final FileObject dir;
    private final Lookup lookup;

    Msp430Project(FileObject dir) {
        this.dir = dir;
        this.lookup = Lookups.fixed(this, new Info(), new Msp430ActionProvider(this), new Msp430LogicalView(this),
                GenericSources.genericOnly(this),
                (RecommendedTemplates) () -> new String[] {CATEGORY, "simple-files"},
                (PrivilegedTemplates) () -> new String[] {"Templates/Other/main.s", "Templates/Other/Folder",
                    "Templates/Other/file"});
    }

    @Override
    public FileObject getProjectDirectory() {
        return dir;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    /** The project folder on disk (a project is only recognized there). */
    Path path() {
        File f = FileUtil.toFile(dir);
        return f.toPath();
    }

    /** The settings, read afresh, since the properties file may have been edited. */
    Properties properties() {
        return ProjectFiles.load(path());
    }

    String name() {
        return ProjectFiles.name(path(), properties());
    }

    private final class Info implements ProjectInformation {
        @Override
        public String getName() {
            return dir.getNameExt();
        }

        @Override
        public String getDisplayName() {
            return name();
        }

        @Override
        public Icon getIcon() {
            return Msp430ProjectFactory.icon();
        }

        @Override
        public Project getProject() {
            return Msp430Project.this;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {
        }
    }
}
