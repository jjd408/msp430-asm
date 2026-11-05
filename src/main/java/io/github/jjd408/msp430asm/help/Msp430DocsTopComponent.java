/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.help;

import io.github.jjd408.msp430asm.Msp430Asm;
import io.github.jjd408.msp430asm.Settings;
import io.github.jjd408.msp430asm.completion.HelpAt;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.event.CaretListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.HtmlBrowser;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.cookies.EditorCookie;
import org.openide.windows.Mode;
import org.openide.windows.OnShowing;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * A window beside the editor that explains what's under the cursor in an MSP430 assembly file: the instruction
 * (its operation, flags and sizes), or the directive, CPU register, peripheral register (with its bits) or
 * constant the cursor is on; elsewhere on an instruction's line, that instruction.
 */
@ConvertAsProperties(dtd = "-//io.github.jjd408.msp430asm.help//Msp430Docs//EN", autostore = false)
@TopComponent.Description(preferredID = Msp430DocsTopComponent.ID,
        iconBase = "io/github/jjd408/msp430asm/file/msp430.png",
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED)
@TopComponent.Registration(mode = "properties", openAtStartup = false, position = 300)
@ActionID(category = "Window", id = "io.github.jjd408.msp430asm.help.Msp430DocsTopComponent")
@ActionReferences({
    @ActionReference(path = "Menu/Window", position = 345),
    @ActionReference(path = "Editors/" + Msp430Asm.MIME_TYPE + "/Popup", position = 1460)
})
@TopComponent.OpenActionRegistration(displayName = "#CTL_Msp430DocsAction", preferredID = Msp430DocsTopComponent.ID)
@Messages({
    "CTL_Msp430DocsAction=MSP430 Documentation",
    "CTL_Msp430DocsTopComponent=MSP430 Documentation",
    "HINT_Msp430DocsTopComponent=What's under the cursor in an MSP430 assembly file",
    "MSG_Start=Put the cursor on an instruction, directive or register in an MSP430 assembly file, and this "
            + "window explains it.",
    "MSG_Nothing=Nothing to explain here. Put the cursor on an instruction, directive or register.",
    "LNK_FamilyGuide=More in the MSP430FR4xx/FR2xx family user's guide (SLAU445)"
})
public final class Msp430DocsTopComponent extends TopComponent {

    static final String ID = "Msp430DocsTopComponent";
    private static final String AUTO_OPENED = "docsWindowAutoOpened";

    private final JEditorPane pane = new JEditorPane();
    private final Timer timer = new Timer(120, e -> refresh());
    private final CaretListener caret = e -> timer.restart();
    private final PropertyChangeListener registry = e -> {
        attach();
        timer.restart();
    };
    private JTextComponent attached;
    private String shown;

    public Msp430DocsTopComponent() {
        setName(Bundle.CTL_Msp430DocsTopComponent());
        setToolTipText(Bundle.HINT_Msp430DocsTopComponent());
        setLayout(new BorderLayout());
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    HtmlBrowser.URLDisplayer.getDefault().showURLExternal(new URL(e.getDescription()));
                } catch (java.net.MalformedURLException ex) {
                    // not a link we made
                }
            }
        });
        add(new JScrollPane(pane), BorderLayout.CENTER);
        timer.setRepeats(false);
        show(Bundle.MSG_Start(), false);
    }

    @Override
    protected void componentOpened() {
        EditorRegistry.addPropertyChangeListener(registry);
        TopComponent.getRegistry().addPropertyChangeListener(registry);
        attach();
        refresh();
    }

    @Override
    protected void componentClosed() {
        EditorRegistry.removePropertyChangeListener(registry);
        TopComponent.getRegistry().removePropertyChangeListener(registry);
        detach();
        timer.stop();
    }

    private static boolean isAssembly(JTextComponent c) {
        return c != null && Msp430Asm.MIME_TYPE.equals(c.getDocument().getProperty("mimeType"));
    }

    /**
     * Follows the caret of the last MSP430 assembly editor used, or else of the one selected in the editor area;
     * other editors leave the window as it is.
     */
    private void attach() {
        JTextComponent c = EditorRegistry.lastFocusedComponent();
        if (!isAssembly(c)) {
            c = selectedEditor();
        }
        if (c == attached || !isAssembly(c)) {
            return;
        }
        detach();
        c.addCaretListener(caret);
        attached = c;
    }

    private static JTextComponent selectedEditor() {
        Mode editors = WindowManager.getDefault().findMode("editor");
        TopComponent tc = editors != null ? editors.getSelectedTopComponent() : null;
        EditorCookie ec = tc != null ? tc.getLookup().lookup(EditorCookie.class) : null;
        JEditorPane[] panes = ec != null ? ec.getOpenedPanes() : null;
        return panes != null && panes.length > 0 ? panes[0] : null;
    }

    private void detach() {
        if (attached != null) {
            attached.removeCaretListener(caret);
            attached = null;
        }
    }

    private void refresh() {
        JTextComponent c = attached;
        if (c == null) {
            return;
        }
        Document doc = c.getDocument();
        int dot = c.getCaretPosition();
        DeviceInfo device = Settings.device();
        String html = HelpAt.help(doc, dot, device);
        if (html == null) {
            html = HelpAt.lineInstruction(doc, dot, device);
        }
        if (html == null) {
            show(Bundle.MSG_Nothing(), false);
        } else {
            show(html.startsWith("<html>") ? html.substring(6) : html, true);
        }
    }

    private void show(String body, boolean link) {
        String page = "<html><body>" + body + (link ? "<p><a href=\"" + Documentation.FAMILY_GUIDE + "\">"
                + Bundle.LNK_FamilyGuide() + "</a></p>" : "") + "</body></html>";
        if (!page.equals(shown)) {
            shown = page;
            pane.setText(page);
            pane.setCaretPosition(0);
        }
    }

    void writeProperties(Properties p) {
        p.setProperty("version", "1.0");
    }

    void readProperties(Properties p) {
    }

    /** Opens the window beside the editor the first time an MSP430 assembly file is edited. */
    @OnShowing
    public static final class AutoOpen implements Runnable {
        @Override
        public void run() {
            Preferences prefs = NbPreferences.forModule(Msp430DocsTopComponent.class);
            if (prefs.getBoolean(AUTO_OPENED, false)) {
                return;
            }
            PropertyChangeListener[] l = new PropertyChangeListener[1];
            l[0] = e -> {
                if (isAssembly(EditorRegistry.lastFocusedComponent())) {
                    EditorRegistry.removePropertyChangeListener(l[0]);
                    prefs.putBoolean(AUTO_OPENED, true);
                    EventQueue.invokeLater(() -> {
                        TopComponent tc = WindowManager.getDefault().findTopComponent(ID);
                        if (tc != null && !tc.isOpened()) {
                            tc.open();  // beside the editor, without taking focus from it
                        }
                    });
                }
            };
            EditorRegistry.addPropertyChangeListener(l[0]);
        }
    }
}
