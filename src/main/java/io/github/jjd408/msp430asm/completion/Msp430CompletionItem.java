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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.net.URL;
import javax.swing.Action;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.Exceptions;

/** A completion choice in NetBeans' list, with its help in the documentation pane. */
final class Msp430CompletionItem implements CompletionItem {

    private final Choices.Choice choice;
    private final int start;  // where the word being completed starts

    Msp430CompletionItem(Choices.Choice choice, int start) {
        this.choice = choice;
        this.start = start;
    }

    private String left() {
        return "<b>" + choice.text + "</b>";
    }

    private String right() {
        return "<font color=#808080>" + choice.note + "</font>";
    }

    @Override
    public void defaultAction(JTextComponent component) {
        Document doc = component.getDocument();
        int caret = component.getCaretPosition();
        try {
            doc.remove(start, caret - start);
            doc.insertString(start, choice.insert, null);
        } catch (BadLocationException e) {
            Exceptions.printStackTrace(e);
        }
        Completion.get().hideAll();
    }

    @Override
    public void processKeyEvent(KeyEvent evt) {
    }

    @Override
    public int getPreferredWidth(Graphics g, Font defaultFont) {
        return CompletionUtilities.getPreferredWidth(left(), right(), g, defaultFont);
    }

    @Override
    public void render(Graphics g, Font defaultFont, Color defaultColor, Color backgroundColor, int width,
            int height, boolean selected) {
        CompletionUtilities.renderHtml(null, left(), right(), g, defaultFont, defaultColor, width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
                String html = choice.help.get();
                resultSet.setDocumentation(new CompletionDocumentation() {
                    @Override
                    public String getText() {
                        return html;
                    }

                    @Override
                    public URL getURL() {
                        return null;
                    }

                    @Override
                    public CompletionDocumentation resolveLink(String link) {
                        return null;
                    }

                    @Override
                    public Action getGotoSourceAction() {
                        return null;
                    }
                });
                resultSet.finish();
            }
        });
    }

    @Override
    public CompletionTask createToolTipTask() {
        return null;
    }

    @Override
    public boolean instantSubstitution(JTextComponent component) {
        return false;
    }

    @Override
    public int getSortPriority() {
        return choice.kind.ordinal();
    }

    @Override
    public CharSequence getSortText() {
        return choice.text;
    }

    @Override
    public CharSequence getInsertPrefix() {
        return choice.text;
    }
}
