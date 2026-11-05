/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.machinecode;

import io.github.jjd408.msp430asm.Settings;
import io.github.jjd408.msp430asm.assemble.Assembler;
import io.github.jjd408.msp430asm.assemble.Diagnostic;
import io.github.jjd408.msp430asm.assemble.Preprocessor;
import io.github.jjd408.msp430asm.assemble.Listing;
import io.github.jjd408.msp430asm.assemble.Toolchain;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.ChangeSupport;
import org.openide.util.RequestProcessor;

/**
 * The machine code of one MSP430 assembly document: assembled in the background whenever the text has been
 * still for a moment, with the assembler's errors and warnings shown as editor annotations.
 * The toolchain and device come from {@link Settings}.
 */
public final class MachineCode {

    private static final Logger LOG = Logger.getLogger(MachineCode.class.getName());
    private static final RequestProcessor RP = new RequestProcessor("MSP430 assembler", 1);
    private static final int DELAY_MS = 700;
    private static final String HINTS_LAYER = "msp430-elf-as";

    private final Document doc;
    private final RequestProcessor.Task task;
    private final ChangeSupport changes = new ChangeSupport(this);
    private volatile Map<Integer, Listing.Entry> code = Collections.emptyMap();

    /** The machine code model of a document, made on first use. */
    public static synchronized MachineCode get(Document doc) {
        MachineCode mc = (MachineCode) doc.getProperty(MachineCode.class);
        if (mc == null) {
            mc = new MachineCode(doc);
            doc.putProperty(MachineCode.class, mc);
        }
        return mc;
    }

    private MachineCode(Document doc) {
        this.doc = doc;
        task = RP.create(this::assemble);
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                task.schedule(DELAY_MS);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                task.schedule(DELAY_MS);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        task.schedule(0);
    }

    /** Machine code of a source line (1-based), or null. */
    public Listing.Entry entry(int line) {
        return code.get(line);
    }

    public void addChangeListener(ChangeListener l) {
        changes.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        changes.removeChangeListener(l);
    }

    private void assemble() {
        String[] text = new String[1];
        doc.render(() -> {
            try {
                text[0] = doc.getText(0, doc.getLength());
            } catch (BadLocationException e) {
                text[0] = "";
            }
        });
        FileObject fo = fileOf(doc);
        String ext = fo == null ? "s" : fo.getExt();
        boolean preprocess = Preprocessor.runsOn(ext);
        Path dir = null;
        if (fo != null && fo.getParent() != null) {
            File f = FileUtil.toFile(fo.getParent());
            dir = f == null ? null : f.toPath();
        }

        List<ErrorDescription> errors = new ArrayList<>();
        Toolchain tc = Settings.toolchain();
        if (tc == null) {
            code = Collections.emptyMap();
            errors.add(ErrorDescriptionFactory.createErrorDescription(Severity.WARNING,
                    "MSP430-GCC not found (looked in ~/ti/msp430-gcc/bin and on the PATH), so there's no machine "
                    + "code to show", doc, 1));
        } else {
            String mcu = Settings.mcu();
            try {
                Assembler.Result r = new Assembler(tc, mcu).assemble(text[0], preprocess, dir);
                code = r.code;
                int lines = doc.getDefaultRootElement().getElementCount();
                List<Diagnostic> all = new ArrayList<>();
                if (!preprocess) {
                    all.addAll(Preprocessor.ignoredDirectives(text[0], fo == null ? "this file" : fo.getNameExt()));
                }
                all.addAll(r.diagnostics);
                for (Diagnostic d : all) {
                    Severity sev = d.severity == Diagnostic.Severity.ERROR ? Severity.ERROR : Severity.WARNING;
                    errors.add(ErrorDescriptionFactory.createErrorDescription(sev, d.message, doc,
                            Math.max(1, Math.min(d.line, lines))));
                }
            } catch (IOException e) {
                LOG.log(Level.INFO, "Assembling failed", e);
                code = Collections.emptyMap();
                errors.add(ErrorDescriptionFactory.createErrorDescription(Severity.WARNING,
                        "Couldn't run the assembler: " + e.getMessage(), doc, 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        HintsController.setErrors(doc, HINTS_LAYER, errors);
        SwingUtilities.invokeLater(changes::fireChange);
    }

    private static FileObject fileOf(Document doc) {
        Object sd = doc.getProperty(Document.StreamDescriptionProperty);
        if (sd instanceof DataObject) {
            return ((DataObject) sd).getPrimaryFile();
        }
        if (sd instanceof FileObject) {
            return (FileObject) sd;
        }
        return null;
    }
}
