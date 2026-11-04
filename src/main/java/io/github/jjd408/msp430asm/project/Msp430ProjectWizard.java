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

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;

/** File &gt; New Project &gt; MSP430 &gt; MSP430 Project: a project with a two-file starter program. */
@TemplateRegistration(
        folder = "Project/MSP430",
        displayName = "#Msp430ProjectWizard_display",
        description = "ProjectDescription.html",
        iconBase = Msp430ProjectFactory.ICON,
        position = 100)
@Messages({
    "Msp430ProjectWizard_display=MSP430 Project",
    "LBL_NameAndLocation=Name and Location"
})
public final class Msp430ProjectWizard implements WizardDescriptor.InstantiatingIterator<WizardDescriptor> {

    static final String PROP_NAME = "msp430.name";
    static final String PROP_DIR = "msp430.dir";

    private WizardDescriptor wizard;
    private NamePanel panel;

    @Override
    public void initialize(WizardDescriptor wizard) {
        this.wizard = wizard;
        panel = new NamePanel();
        // the New Project wizard's own first step ("Choose Project"), then ours
        Object before = wizard.getProperty(WizardDescriptor.PROP_CONTENT_DATA);
        String[] steps = before instanceof String[] && ((String[]) before).length > 0
                ? new String[] {((String[]) before)[0], Bundle.LBL_NameAndLocation()}
                : new String[] {Bundle.LBL_NameAndLocation()};
        JComponent c = (JComponent) panel.getComponent();
        c.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
        c.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, steps.length - 1);
        c.setName(Bundle.LBL_NameAndLocation());
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        panel = null;
    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        String name = (String) wizard.getProperty(PROP_NAME);
        File dir = FileUtil.normalizeFile((File) wizard.getProperty(PROP_DIR));
        ProjectFiles.create(dir.toPath(), name);
        File parent = dir.getParentFile();
        if (parent != null) {
            ProjectChooser.setProjectsFolder(parent);
        }
        FileUtil.refreshFor(dir);
        FileObject fo = FileUtil.toFileObject(dir);
        Set<FileObject> result = new LinkedHashSet<>();
        result.add(fo);
        FileObject main = fo.getFileObject("src/main.S");
        if (main != null) {
            result.add(main);
        }
        return result;
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        return panel;
    }

    @Override
    public String name() {
        return Bundle.LBL_NameAndLocation();
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public void nextPanel() {
        throw new NoSuchElementException();
    }

    @Override
    public void previousPanel() {
        throw new NoSuchElementException();
    }

    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }

    /** The name and location step. */
    private final class NamePanel implements WizardDescriptor.Panel<WizardDescriptor> {
        private final ChangeSupport changes = new ChangeSupport(this);
        private NameAndLocationPanel component;

        @Override
        public Component getComponent() {
            if (component == null) {
                component = new NameAndLocationPanel(changes::fireChange);
            }
            return component;
        }

        @Override
        public HelpCtx getHelp() {
            return HelpCtx.DEFAULT_HELP;
        }

        @Override
        public void readSettings(WizardDescriptor settings) {
        }

        @Override
        public void storeSettings(WizardDescriptor settings) {
            settings.putProperty(PROP_NAME, component.projectName());
            settings.putProperty(PROP_DIR, component.projectFolder());
        }

        @Override
        public boolean isValid() {
            getComponent();
            String problem = component.problem();
            wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, problem);
            return problem == null;
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            changes.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            changes.removeChangeListener(l);
        }
    }
}
