/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.options;

import io.github.jjd408.msp430asm.Settings;
import io.github.jjd408.msp430asm.assemble.PicSimLab;
import io.github.jjd408.msp430asm.assemble.Toolchain;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** The MSP430 Options page: toolchain, device, PICSimLab and board. */
final class Msp430OptionsPanel extends JPanel {

    private final Msp430OptionsPanelController controller;
    private final JTextField toolchain = new JTextField(30);
    private final JLabel toolchainFound = new JLabel();
    private final JComboBox<String> mcu = new JComboBox<>(new String[] {"msp430fr2355", "msp430fr2353",
        "msp430fr2155", "msp430fr2153"});
    private final JTextField picsimlab = new JTextField(30);
    private final JLabel picsimlabFound = new JLabel();
    private final JComboBox<String> board = new JComboBox<>(new String[] {"Solitaire", "MSP430_Board"});

    Msp430OptionsPanel(Msp430OptionsPanelController controller) {
        this.controller = controller;
        mcu.setEditable(true);
        setLayout(new GridBagLayout());
        int row = 0;
        row = field(row, "MSP430-GCC bin folder:", toolchain, browse(toolchain), toolchainFound,
                "Leave empty to look in ~/ti/msp430-gcc/bin and on the PATH.");
        row = field(row, "Device (-mmcu):", mcu, null, null,
                "Picks TI's linker script and the headers completion reads.");
        row = field(row, "PICSimLab folder:", picsimlab, browse(picsimlab), picsimlabFound,
                "The picsimlab-msp430 checkout, with run.sh. Leave empty to look in "
                + "~/projects/solitaire/sim/picsimlab-msp430.");
        row = field(row, "PICSimLab board:", board, null, null,
                "Solitaire: the handheld's display and buttons. MSP430_Board: a bare chip for your own parts.");
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        add(new JLabel(), filler);

        DocumentListener changed = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        };
        toolchain.getDocument().addDocumentListener(changed);
        picsimlab.getDocument().addDocumentListener(changed);
        mcu.addActionListener(e -> changed());
        board.addActionListener(e -> changed());
    }

    private int field(int row, String label, java.awt.Component input, JButton button, JLabel status, String hint) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 4, 2, 6);
        add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(input, c);
        if (button != null) {
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            add(button, c);
        }
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row + 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 4, 0, 6);
        JLabel h = new JLabel("<html><small>" + hint + "</small>");
        if (status != null) {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints pc = new GridBagConstraints();
            pc.anchor = GridBagConstraints.WEST;
            pc.gridy = 0;
            p.add(status, pc);
            pc.gridy = 1;
            p.add(h, pc);
            add(p, c);
        } else {
            add(h, c);
        }
        return row + 2;
    }

    private JButton browse(JTextField target) {
        JButton b = new JButton("Browse...");
        b.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(target.getText().isBlank() ? null : new File(target.getText()));
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                target.setText(fc.getSelectedFile().getPath());
            }
        });
        return b;
    }

    private void changed() {
        Toolchain tc = Toolchain.find(toolchain.getText());
        toolchainFound.setText(tc == null ? "<html><font color=#c0392b>msp430-elf-as not found</font>"
                : "<html>Using <b>" + tc.bin + "</b>" + (tc.include() == null ? " (TI's headers missing beside it)" : ""));
        PicSimLab sim = PicSimLab.find(picsimlab.getText());
        picsimlabFound.setText(sim == null ? "<html><font color=#c0392b>run.sh not found</font>"
                : "<html>Using <b>" + sim.dir + "</b>");
        controller.changed();
    }

    void load() {
        toolchain.setText(Settings.get(Settings.TOOLCHAIN_BIN, ""));
        mcu.setSelectedItem(Settings.mcu());
        picsimlab.setText(Settings.get(Settings.PICSIMLAB_DIR, ""));
        board.setSelectedItem(Settings.board());
        changed();
    }

    void store() {
        Settings.put(Settings.TOOLCHAIN_BIN, toolchain.getText().trim());
        Settings.put(Settings.MCU, String.valueOf(mcu.getSelectedItem()).trim());
        Settings.put(Settings.PICSIMLAB_DIR, picsimlab.getText().trim());
        Settings.put(Settings.BOARD, String.valueOf(board.getSelectedItem()));
    }

    boolean valid() {
        return !String.valueOf(mcu.getSelectedItem()).isBlank();
    }
}
