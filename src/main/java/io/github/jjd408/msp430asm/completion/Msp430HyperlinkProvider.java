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
import java.util.EnumSet;
import java.util.Set;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;

/**
 * Hover help with Ctrl held, for instructions, directives, registers, peripheral registers, constants and labels;
 * Ctrl+click on a label goes to where it's defined.
 */
@MimeRegistration(mimeType = Msp430Asm.MIME_TYPE, service = HyperlinkProviderExt.class, position = 100)
public final class Msp430HyperlinkProvider implements HyperlinkProviderExt {

    /** The word at an offset and what's around it. */
    private static final class Word {
        int start;
        int end;
        String word;
        Context context;
        String text;
    }

    private static Word wordAt(Document doc, int offset) {
        Word[] out = new Word[1];
        doc.render(() -> {
            try {
                Element root = doc.getDefaultRootElement();
                Element line = root.getElement(root.getElementIndex(offset));
                int ls = line.getStartOffset();
                String lineText = doc.getText(ls, line.getEndOffset() - ls);
                int at = offset - ls;
                int s = at;
                int e = at;
                while (s > 0 && Context.isWordChar(lineText.charAt(s - 1))) {
                    s--;
                }
                while (e < lineText.length() && Context.isWordChar(lineText.charAt(e))) {
                    e++;
                }
                if (s == e) {
                    return;
                }
                Word w = new Word();
                w.start = ls + s;
                w.end = ls + e;
                w.word = lineText.substring(s, e);
                w.context = Context.of(lineText.substring(0, s));
                w.text = doc.getText(0, doc.getLength());
                out[0] = w;
            } catch (BadLocationException e) {
                out[0] = null;
            }
        });
        return out[0];
    }

    private static String help(Document doc, int offset) {
        Word w = wordAt(doc, offset);
        return w == null ? null : Choices.helpFor(w.context, w.word, w.text, Settings.device());
    }

    @Override
    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION);
    }

    @Override
    public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
        return help(doc, offset) != null;
    }

    @Override
    public int[] getHyperlinkSpan(Document doc, int offset, HyperlinkType type) {
        Word w = wordAt(doc, offset);
        return w == null ? null : new int[] {w.start, w.end};
    }

    @Override
    public void performClickAction(Document doc, int offset, HyperlinkType type) {
        Word w = wordAt(doc, offset);
        if (w == null) {
            return;
        }
        Integer line = Labels.scan(w.text).get(w.word);
        JTextComponent c = EditorRegistry.lastFocusedComponent();
        if (line == null || c == null || c.getDocument() != doc) {
            return;
        }
        Element root = doc.getDefaultRootElement();
        c.setCaretPosition(root.getElement(line - 1).getStartOffset());
        c.requestFocusInWindow();
    }

    @Override
    public String getTooltipText(Document doc, int offset, HyperlinkType type) {
        return help(doc, offset);
    }
}
