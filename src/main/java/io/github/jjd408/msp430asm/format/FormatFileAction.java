/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.format;

import io.github.jjd408.msp430asm.Settings;
import io.github.jjd408.msp430asm.file.Msp430AsmDataObject;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.text.NbDocument;
import org.openide.util.NbBundle.Messages;

/**
 * Format, on assembly files in the Projects and Files windows: formats each selected file as Source &gt; Format would,
 * without opening it in the editor. A file with no unsaved changes is saved afterwards; one with unsaved changes is
 * left unsaved, as you were still working on it. Edit &gt; Undo in the file's editor takes it back.
 */
@ActionID(category = "Source", id = "io.github.jjd408.msp430asm.format.FormatFileAction")
@ActionRegistration(displayName = "#CTL_FormatFileAction")
@ActionReference(path = "Loaders/text/x-msp430-asm/Actions", position = 160)
@Messages({
    "CTL_FormatFileAction=Format",
    "# {0} - file name",
    "# {1} - error",
    "MSG_FormatFailed=Couldn''t format {0}: {1}"
})
public final class FormatFileAction implements ActionListener {

    private final List<Msp430AsmDataObject> files;

    public FormatFileAction(List<Msp430AsmDataObject> files) {
        this.files = files;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AsmFormatter formatter = Settings.formatter();
        for (Msp430AsmDataObject file : files) {
            try {
                format(file, formatter);
            } catch (IOException | BadLocationException ex) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
                        Bundle.MSG_FormatFailed(file.getPrimaryFile().getNameExt(), ex.getMessage()),
                        NotifyDescriptor.WARNING_MESSAGE));
            }
        }
    }

    private static void format(Msp430AsmDataObject file, AsmFormatter formatter)
            throws IOException, BadLocationException {
        EditorCookie editor = file.getLookup().lookup(EditorCookie.class);
        if (editor == null) {
            return;
        }
        boolean wasModified = file.isModified();
        StyledDocument doc = editor.openDocument();
        BadLocationException[] failed = {null};
        NbDocument.runAtomicAsUser(doc, () -> {
            try {
                AsmReformatTaskFactory.reformat(doc, 0, doc.getLength(), formatter);
            } catch (BadLocationException ex) {
                failed[0] = ex;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        if (!wasModified) {
            editor.saveDocument();
        }
    }
}
