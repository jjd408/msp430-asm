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

import java.io.IOException;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/** Recognizes a folder with {@code msp430-project.properties} as an MSP430 project. */
@ServiceProvider(service = ProjectFactory.class)
public final class Msp430ProjectFactory implements ProjectFactory2 {

    static final String ICON = "io/github/jjd408/msp430asm/project/project.png";

    static Icon icon() {
        return ImageUtilities.loadImageIcon(ICON, false);
    }

    @Override
    public ProjectManager.Result isProject2(FileObject dir) {
        return isProject(dir) ? new ProjectManager.Result(icon()) : null;
    }

    @Override
    public boolean isProject(FileObject dir) {
        FileObject marker = dir.getFileObject(ProjectFiles.MARKER);
        return marker != null && marker.isData();
    }

    @Override
    public Project loadProject(FileObject dir, ProjectState state) throws IOException {
        return isProject(dir) ? new Msp430Project(dir) : null;
    }

    @Override
    public void saveProject(Project project) {
        // the project keeps nothing unsaved: its settings are the properties file, edited as a file
    }
}
