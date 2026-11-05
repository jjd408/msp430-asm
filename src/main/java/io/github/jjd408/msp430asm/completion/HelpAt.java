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

import io.github.jjd408.msp430asm.help.DeviceInfo;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * The help for what's at a place in an MSP430 assembly document: the word there (an instruction, directive,
 * register, peripheral register, constant or label), as Ctrl+hover and the documentation window show it.
 */
public final class HelpAt {

    /** The word at an offset and what's around it. */
    public static final class Word {
        public int start;
        public int end;
        public String word;
        Context context;
        String text;
    }

    private HelpAt() {
    }

    /** The word at an offset, or null if the offset isn't in or next to one. */
    public static Word wordAt(Document doc, int offset) {
        Word[] out = new Word[1];
        doc.render(() -> {
            try {
                Element root = doc.getDefaultRootElement();
                Element line = root.getElement(root.getElementIndex(offset));
                int ls = line.getStartOffset();
                String lineText = doc.getText(ls, line.getEndOffset() - ls);
                int at = Math.min(offset - ls, lineText.length());
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

    /** The help for the word at an offset, or null. */
    public static String help(Document doc, int offset, DeviceInfo device) {
        Word w = wordAt(doc, offset);
        return w == null ? null : Choices.helpFor(w.context, w.word, w.text, device);
    }

    /**
     * The help for the instruction (or directive) on the line holding an offset: its first word after any
     * label, outside comments; null if the line has none.
     */
    public static String lineInstruction(Document doc, int offset, DeviceInfo device) {
        int[] at = {-1};
        doc.render(() -> {
            try {
                Element root = doc.getDefaultRootElement();
                Element line = root.getElement(root.getElementIndex(offset));
                int ls = line.getStartOffset();
                String t = doc.getText(ls, line.getEndOffset() - ls);
                int i = 0;
                // skip a label ("loop:") and the spaces around it
                int colon = t.indexOf(':');
                int semi = t.indexOf(';');
                if (colon > 0 && (semi < 0 || colon < semi) && t.substring(0, colon).trim().matches("[\\w.$]+")) {
                    i = colon + 1;
                }
                while (i < t.length() && Character.isWhitespace(t.charAt(i))) {
                    i++;
                }
                if (i < t.length() && Context.isWordChar(t.charAt(i))) {
                    at[0] = ls + i;
                }
            } catch (BadLocationException e) {
                at[0] = -1;
            }
        });
        return at[0] < 0 ? null : help(doc, at[0], device);
    }
}
