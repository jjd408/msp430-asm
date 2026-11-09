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

import io.github.jjd408.msp430asm.Msp430Asm;
import io.github.jjd408.msp430asm.Settings;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.ExtraLock;
import org.netbeans.modules.editor.indent.spi.ReformatTask;

/**
 * Source &gt; Format (Alt+Shift+F) for MSP430 assembly: lays out the selected lines, or the whole file, with
 * {@link AsmFormatter} and the columns from Tools &gt; Options &gt; Miscellaneous &gt; MSP430.
 */
@MimeRegistration(mimeType = Msp430Asm.MIME_TYPE, service = ReformatTask.Factory.class)
public final class AsmReformatTaskFactory implements ReformatTask.Factory {

    @Override
    public ReformatTask createTask(Context context) {
        return new Task(context);
    }

    private static final class Task implements ReformatTask {

        private final Context context;

        Task(Context context) {
            this.context = context;
        }

        @Override
        public void reformat() throws BadLocationException {
            AsmReformatTaskFactory.reformat(context.document(), context.startOffset(), context.endOffset(), Settings.formatter());
        }

        @Override
        public ExtraLock reformatLock() {
            return null;
        }
    }

    /**
     * Formats the lines of a document from the one holding start to the one holding end (a selection that ends at
     * the start of a line leaves that line out). Only the characters that change are replaced, so positions such as
     * breakpoints stay on their lines. The caller holds the document's write lock.
     */
    static void reformat(Document doc, int start, int end, AsmFormatter formatter) throws BadLocationException {
        // Block comments and continuations span lines, so format from the top and keep the lines asked for
        List<String> formatted = formatter.formatLines(AsmFormatter.splitLines(doc.getText(0, doc.getLength())));
        Element root = doc.getDefaultRootElement();
        int first = root.getElementIndex(start);
        int last = root.getElementIndex(end);
        if (last > first && root.getElement(last).getStartOffset() == end) {
            last--;
        }
        boolean toEnd = last == root.getElementCount() - 1;
        for (int i = last; i >= first; i--) {
            Element line = root.getElement(i);
            int from = line.getStartOffset();
            int to = Math.min(line.getEndOffset() - 1, doc.getLength());  // without the newline
            String old = doc.getText(from, to - from);
            String now = i < formatted.size() ? formatted.get(i) : "";
            replace(doc, from, old, now);
        }
        if (toEnd && doc.getLength() > 0 && !doc.getText(doc.getLength() - 1, 1).equals("\n")) {
            doc.insertString(doc.getLength(), "\n", null);
        }
    }

    /** Replaces old (at offset) with now, touching only the part between their common start and end. */
    private static void replace(Document doc, int offset, String old, String now) throws BadLocationException {
        if (old.equals(now)) {
            return;
        }
        int p = 0;
        while (p < old.length() && p < now.length() && old.charAt(p) == now.charAt(p)) {
            p++;
        }
        int s = 0;
        while (s < old.length() - p && s < now.length() - p
                && old.charAt(old.length() - 1 - s) == now.charAt(now.length() - 1 - s)) {
            s++;
        }
        int removeLength = old.length() - p - s;
        if (removeLength > 0) {
            doc.remove(offset + p, removeLength);
        }
        String insert = now.substring(p, now.length() - s);
        if (!insert.isEmpty()) {
            doc.insertString(offset + p, insert, null);
        }
    }
}
