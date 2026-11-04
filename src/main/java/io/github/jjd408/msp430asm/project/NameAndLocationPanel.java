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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.util.NbBundle.Messages;

/** Project name, the folder it goes in, and the project folder that makes. */
@Messages({
    "LBL_ProjectName=Project &Name:",
    "LBL_ProjectLocation=Project &Location:",
    "LBL_ProjectFolder=Project &Folder:",
    "LBL_Browse=Br&owse...",
    "ERR_NoName=Enter a project name.",
    "ERR_BadName=The name can't contain / or \\\\.",
    "ERR_NoLocation=The project location must be an existing folder.",
    "ERR_NotEmpty=The project folder already exists and isn't empty."
})
final class NameAndLocationPanel extends JPanel {

    private final JTextField name = new JTextField("msp430-project", 25);
    private final JTextField location = new JTextField(ProjectChooser.getProjectsFolder().getAbsolutePath(), 25);
    private final JTextField folder = new JTextField(25);
    private final Runnable changed;

    NameAndLocationPanel(Runnable changed) {
        this.changed = changed;
        setLayout(new GridBagLayout());
        folder.setEditable(false);
        JButton browse = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(browse, Bundle.LBL_Browse());
        browse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(location.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                location.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        row(0, Bundle.LBL_ProjectName(), name, null);
        row(1, Bundle.LBL_ProjectLocation(), location, browse);
        row(2, Bundle.LBL_ProjectFolder(), folder, null);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = 3;
        filler.weighty = 1;
        add(new JPanel(), filler);
        DocumentListener l = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        };
        name.getDocument().addDocumentListener(l);
        location.getDocument().addDocumentListener(l);
        update();
    }

    private void row(int y, String label, JTextField field, JButton button) {
        JLabel l = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(l, label);
        l.setLabelFor(field);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 12);
        add(l, c);
        c = new GridBagConstraints();
        c.gridy = y;
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 6, 6);
        add(field, c);
        if (button != null) {
            c = new GridBagConstraints();
            c.gridy = y;
            c.gridx = 2;
            c.insets = new Insets(0, 0, 6, 0);
            add(button, c);
        }
    }

    private void update() {
        folder.setText(projectFolder().getPath());
        changed.run();
    }

    String projectName() {
        return name.getText().trim();
    }

    File projectFolder() {
        return new File(location.getText().trim(), projectName());
    }

    /** What's wrong with the entries, or null. */
    String problem() {
        String n = projectName();
        if (n.isEmpty()) {
            return Bundle.ERR_NoName();
        }
        if (n.contains("/") || n.contains("\\")) {
            return Bundle.ERR_BadName();
        }
        if (!new File(location.getText().trim()).isDirectory()) {
            return Bundle.ERR_NoLocation();
        }
        File f = projectFolder();
        String[] inside = f.list();
        if (f.exists() && (inside == null || inside.length > 0)) {
            return Bundle.ERR_NotEmpty();
        }
        return null;
    }
}
