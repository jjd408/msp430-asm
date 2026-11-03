/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.file;

import io.github.jjd408.msp430asm.Msp430Asm;
import java.io.IOException;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/** An MSP430 assembly source file: .s, .S (preprocessed by cpp first), .s43 and .asm. */
@Messages({
    "LBL_Msp430Asm_LOADER=MSP430 Assembly",
    "LBL_Msp430Asm_EDITOR=Source"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_Msp430Asm_LOADER",
        mimeType = Msp430Asm.MIME_TYPE,
        extension = {"s", "S", "s43", "S43", "asm", "ASM"},
        position = 280)
@DataObject.Registration(
        mimeType = Msp430Asm.MIME_TYPE,
        iconBase = "io/github/jjd408/msp430asm/file/msp430.png",
        displayName = "#LBL_Msp430Asm_LOADER",
        position = 300)
@ActionReferences({
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"), position = 100,
            separatorAfter = 200),
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"), position = 300),
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"), position = 400,
            separatorAfter = 500),
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"), position = 600),
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"), position = 700,
            separatorAfter = 800),
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"), position = 900,
            separatorAfter = 1000),
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"), position = 1100,
            separatorAfter = 1200),
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"), position = 1300),
    @ActionReference(path = "Loaders/text/x-msp430-asm/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"), position = 1400)
})
public class Msp430AsmDataObject extends MultiDataObject {

    public Msp430AsmDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor(Msp430Asm.MIME_TYPE, true);
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @MultiViewElement.Registration(
            displayName = "#LBL_Msp430Asm_EDITOR",
            iconBase = "io/github/jjd408/msp430asm/file/msp430.png",
            mimeType = Msp430Asm.MIME_TYPE,
            persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
            preferredID = "Msp430Asm",
            position = 1000)
    public static MultiViewEditorElement createEditor(Lookup lkp) {
        return new MultiViewEditorElement(lkp);
    }
}
