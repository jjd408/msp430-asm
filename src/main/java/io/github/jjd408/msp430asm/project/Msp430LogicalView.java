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

import java.awt.Image;
import javax.swing.Action;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/** The project's node in the Projects window: its folder, with the project's actions. */
final class Msp430LogicalView implements LogicalViewProvider {

    private final Msp430Project project;

    Msp430LogicalView(Msp430Project project) {
        this.project = project;
    }

    @Override
    public Node createLogicalView() {
        Node folder = DataFolder.findFolder(project.getProjectDirectory()).getNodeDelegate();
        return new ProjectNode(folder, project);
    }

    @Override
    public Node findPath(Node root, Object target) {
        FileObject fo = target instanceof FileObject ? (FileObject) target
                : target instanceof DataObject ? ((DataObject) target).getPrimaryFile() : null;
        if (fo == null) {
            return null;
        }
        String rel = FileUtil.getRelativePath(project.getProjectDirectory(), fo);
        if (rel == null) {
            return null;
        }
        Node n = root;
        FileObject at = project.getProjectDirectory();
        for (String part : rel.split("/")) {
            if (part.isEmpty()) {
                continue;
            }
            at = at.getFileObject(part);
            Node next = null;
            for (Node child : n.getChildren().getNodes(true)) {
                DataObject d = child.getLookup().lookup(DataObject.class);
                if (d != null && d.getPrimaryFile().equals(at)) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                return null;
            }
            n = next;
        }
        return n;
    }

    @Messages({
        "LBL_Build=Build",
        "LBL_Rebuild=Clean and Build",
        "LBL_Clean=Clean",
        "LBL_Run=Run in PICSimLab",
        "LBL_Debug=Debug in PICSimLab"
    })
    private static final class ProjectNode extends FilterNode {
        private final Msp430Project project;

        ProjectNode(Node folder, Msp430Project project) {
            super(folder, new FilterNode.Children(folder),
                    new ProxyLookup(Lookups.singleton(project), folder.getLookup()));
            this.project = project;
        }

        @Override
        public String getDisplayName() {
            return project.name();
        }

        @Override
        public String getHtmlDisplayName() {
            return null;
        }

        @Override
        public Image getIcon(int type) {
            return ImageUtilities.loadImage(Msp430ProjectFactory.ICON);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public boolean canRename() {
            return false;
        }

        @Override
        public boolean canDestroy() {
            return false;
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[] {
                CommonProjectActions.newFileAction(),
                null,
                ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_BUILD, Bundle.LBL_Build(), null),
                ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_REBUILD, Bundle.LBL_Rebuild(),
                        null),
                ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_CLEAN, Bundle.LBL_Clean(), null),
                ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_RUN, Bundle.LBL_Run(), null),
                ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_DEBUG, Bundle.LBL_Debug(), null),
                null,
                CommonProjectActions.setAsMainProjectAction(),
                CommonProjectActions.closeProjectAction(),
                null,
                CommonProjectActions.deleteProjectAction()
            };
        }
    }
}
