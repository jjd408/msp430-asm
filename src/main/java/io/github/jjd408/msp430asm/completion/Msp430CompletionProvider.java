/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.completion;

import io.github.jjd408.msp430asm.Msp430Asm;
import io.github.jjd408.msp430asm.Settings;
import io.github.jjd408.msp430asm.help.DeviceInfo;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;

/**
 * Code completion for MSP430 assembly (Ctrl+Space; also opens by itself after typing '&amp;' or '#'): see
 * {@link Choices} for what's offered where.
 */
@MimeRegistration(mimeType = Msp430Asm.MIME_TYPE, service = CompletionProvider.class, position = 100)
public final class Msp430CompletionProvider implements CompletionProvider {

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        if (queryType != COMPLETION_QUERY_TYPE) {
            return null;
        }
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet resultSet, Document doc, int caret) {
                String[] lineAndText = new String[2];
                doc.render(() -> {
                    try {
                        Element root = doc.getDefaultRootElement();
                        int lineStart = root.getElement(root.getElementIndex(caret)).getStartOffset();
                        lineAndText[0] = doc.getText(lineStart, caret - lineStart);
                        lineAndText[1] = doc.getText(0, doc.getLength());
                    } catch (BadLocationException e) {
                        lineAndText[0] = null;
                    }
                });
                if (lineAndText[0] != null) {
                    Context ctx = Context.of(lineAndText[0]);
                    DeviceInfo device = Settings.device();
                    int start = caret - ctx.prefix.length();
                    for (Choices.Choice c : Choices.of(ctx, lineAndText[1], device)) {
                        resultSet.addItem(new Msp430CompletionItem(c, start));
                    }
                }
                resultSet.finish();
            }
        }, component);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return typedText.endsWith("&") || typedText.endsWith("#") ? COMPLETION_QUERY_TYPE : 0;
    }
}
