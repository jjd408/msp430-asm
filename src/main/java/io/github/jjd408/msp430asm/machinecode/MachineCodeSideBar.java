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

import io.github.jjd408.msp430asm.assemble.Listing;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import org.openide.util.WeakListeners;

/**
 * A column beside the line numbers showing, for each source line, the offset in its section and the machine code
 * words it assembled to. Long data is cut short in the column; the tooltip has all of it.
 */
final class MachineCodeSideBar extends JComponent implements ChangeListener {

    /** Room for the address and three words: "0000  40B2 5A80 01CC". */
    private static final int COLUMNS = 20;
    private static final int PAD = 6;

    private final JTextComponent text;
    private final MachineCode model;

    MachineCodeSideBar(JTextComponent text) {
        this.text = text;
        this.model = MachineCode.get(text.getDocument());
        model.addChangeListener(WeakListeners.change(this, model));
        text.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                revalidate();
                repaint();
            }
        });
        text.addPropertyChangeListener("font", e -> {
            revalidate();
            repaint();
        });
        setToolTipText("");  // turns tooltips on; the text comes from getToolTipText(MouseEvent)
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        repaint();
    }

    private Font columnFont() {
        Font f = text.getFont();
        return f.deriveFont(Math.max(9f, f.getSize2D() - 1f));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(columnFont());
        return new Dimension(fm.charWidth('0') * COLUMNS + 2 * PAD, text.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        Rectangle clip = g.getClipBounds();
        Color bg = text.getBackground();
        g.setColor(mix(bg, Color.GRAY, 0.08f));
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
        g.setColor(mix(bg, Color.GRAY, 0.35f));
        g.drawLine(getWidth() - 1, clip.y, getWidth() - 1, clip.y + clip.height);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(columnFont());
        FontMetrics fm = g.getFontMetrics();
        Color fg = text.getForeground();
        Color addressColor = mix(fg, bg, 0.55f);
        Color codeColor = mix(fg, bg, 0.25f);
        int maxWidth = getWidth() - 2 * PAD;

        text.getDocument().render(() -> {
            try {
                Element root = text.getDocument().getDefaultRootElement();
                int offset = text.viewToModel2D(new Point(0, clip.y));
                double lastY = Double.NaN;
                for (int line = root.getElementIndex(Math.max(0, offset)); line < root.getElementCount(); line++) {
                    Rectangle2D r = text.modelToView2D(root.getElement(line).getStartOffset());
                    if (r == null || r.getY() > clip.y + clip.height) {
                        break;
                    }
                    if (r.getY() == lastY) {
                        continue;  // inside a fold
                    }
                    lastY = r.getY();
                    Listing.Entry e = model.entry(line + 1);
                    if (e == null) {
                        continue;
                    }
                    float y = (float) (r.getY() + (r.getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    String address = (e.address != null ? e.address : "----") + "  ";
                    g.setColor(addressColor);
                    g.drawString(address, PAD, y);
                    int x = PAD + fm.stringWidth(address);
                    g.setColor(codeColor);
                    g.drawString(fit(e.words(), fm, maxWidth - (x - PAD)), x, y);
                }
            } catch (BadLocationException e) {
                // the document changed under us; the next repaint catches up
            }
        });
    }

    /** Cuts text to a width, ending in "…" when it doesn't fit. */
    private static String fit(String s, FontMetrics fm, int width) {
        if (fm.stringWidth(s) <= width) {
            return s;
        }
        String dots = "…";
        int n = s.length();
        while (n > 0 && fm.stringWidth(s.substring(0, n) + dots) > width) {
            n--;
        }
        return s.substring(0, n).stripTrailing() + dots;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int offset = text.viewToModel2D(new Point(0, event.getY()));
        if (offset < 0) {
            return null;
        }
        int line = text.getDocument().getDefaultRootElement().getElementIndex(offset) + 1;
        Listing.Entry e = model.entry(line);
        if (e == null) {
            return null;
        }
        return "<html>Line " + line + ": " + e.byteCount() + (e.byteCount() == 1 ? " byte" : " bytes")
                + (e.address != null ? " at offset 0x" + e.address + " in its section" : "")
                + "<br><tt>" + e.words() + "</tt>"
                + "<br><small>16-bit words as the CPU reads them; fields the linker fills in show as 0000.</small>";
    }

    private static Color mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() * (1 - t) + b.getRed() * t),
                Math.round(a.getGreen() * (1 - t) + b.getGreen() * t),
                Math.round(a.getBlue() * (1 - t) + b.getBlue() * t));
    }
}
