/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.debug;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.netbeans.api.debugger.Breakpoint;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.text.Line;

/**
 * A breakpoint on a line of an MSP430 assembly file. It follows its line as the file is edited (through the
 * editor's {@link Line}), and a debugger sets it with GDB's {@code file:line}, which the build's line
 * information turns into an address.
 */
public final class Msp430LineBreakpoint extends Breakpoint {

    private final String url;
    private volatile int lineNumber;  // 1-based, as last known
    private volatile boolean enabled = true;
    private volatile Line line;       // once known: moves with edits while the file is open

    private Msp430LineBreakpoint(String url, int lineNumber, Line line) {
        this.url = url;
        this.lineNumber = lineNumber;
        this.line = line;
    }

    /** A breakpoint on an editor line. */
    public static Msp430LineBreakpoint create(Line line) {
        FileObject fo = line.getLookup().lookup(FileObject.class);
        return new Msp430LineBreakpoint(fo.toURL().toString(), line.getLineNumber() + 1, line);
    }

    /** A breakpoint on a file's line (1-based), as saved. */
    public static Msp430LineBreakpoint create(String url, int lineNumber) {
        return new Msp430LineBreakpoint(url, lineNumber, null);
    }

    public String getURL() {
        return url;
    }

    public FileObject getFileObject() {
        try {
            return URLMapper.findFileObject(new URL(url));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /** The file on disk, or null. */
    public File getFile() {
        FileObject fo = getFileObject();
        return fo == null ? null : FileUtil.toFile(fo);
    }

    /** Its line, 1-based, where the line is now. */
    public int getLineNumber() {
        Line l = line;
        if (l != null) {
            lineNumber = l.getLineNumber() + 1;
        }
        return lineNumber;
    }

    /** Its editor line, found (without opening an editor) if not known yet; null if the file is gone. */
    public Line getLine() {
        Line l = line;
        if (l == null) {
            FileObject fo = getFileObject();
            if (fo == null) {
                return null;
            }
            try {
                LineCookie lc = DataObject.find(fo).getLookup().lookup(LineCookie.class);
                if (lc == null) {
                    return null;
                }
                l = lc.getLineSet().getCurrent(lineNumber - 1);
                line = l;
            } catch (IOException | IndexOutOfBoundsException e) {
                return null;
            }
        }
        return l;
    }

    /** Ties it to a line of an editor's line set (when its file is opened). */
    void setLine(Line l) {
        line = l;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void disable() {
        if (enabled) {
            enabled = false;
            firePropertyChange(PROP_ENABLED, Boolean.TRUE, Boolean.FALSE);
        }
    }

    @Override
    public void enable() {
        if (!enabled) {
            enabled = true;
            firePropertyChange(PROP_ENABLED, Boolean.FALSE, Boolean.TRUE);
        }
    }

    /** For a debugger: whether GDB could place it, and why not. */
    public void setDebuggerValidity(VALIDITY validity, String reason) {
        setValidity(validity, reason);
    }

    /** "main.S:12" */
    public String getDisplayName() {
        String name = url.substring(url.lastIndexOf('/') + 1);
        return name + ":" + getLineNumber();
    }

    @Override
    public String toString() {
        return "Msp430LineBreakpoint " + url + ":" + getLineNumber();
    }
}
