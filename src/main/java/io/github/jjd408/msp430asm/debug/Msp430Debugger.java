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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerInfo;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerAdapter;
import org.netbeans.api.debugger.Watch;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerEngineProvider;
import org.netbeans.spi.debugger.SessionProvider;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.util.RequestProcessor;

/**
 * A debugging session: msp430-elf-gdb connected to PICSimLab's GDB server. It loads the program (so a
 * PICSimLab already running, perhaps with another program, does too), resets the chip, sets the breakpoints
 * of the program's files and keeps them in step, runs, and on each stop shows the line and takes a snapshot of
 * the registers, the peripheral registers and the watches for the debugger's windows. One per session,
 * registered in META-INF/debugger/MSP430Session.
 */
public final class Msp430Debugger {

    public static final String SESSION = "MSP430Session";
    public static final String PROP_STATE = "state";
    public static final String PROP_SNAPSHOT = "snapshot";

    public enum State { STARTING, RUNNING, STOPPED, FINISHED }

    /** What the debugger's windows show at a stop. */
    public static final class Snapshot {
        /** CPU registers by name (pc, sp, sr, r4 ... r15), as gdb names them. */
        public final Map<String, Long> registers;
        /** Memory 0x0100-0x0FFF (the peripheral registers), or null. */
        public final byte[] peripherals;
        /** Watch expression to its value, or to gdb's message. */
        public final Map<String, String> watches;

        Snapshot(Map<String, Long> registers, byte[] peripherals, Map<String, String> watches) {
            this.registers = registers;
            this.peripherals = peripherals;
            this.watches = watches;
        }

        static final Snapshot EMPTY = new Snapshot(Map.of(), null, Map.of());
    }

    static final int PERIPHERALS_FROM = 0x0100;
    static final int PERIPHERALS_TO = 0x1000;

    private static final RequestProcessor RP = new RequestProcessor("MSP430 debugger", 1);

    private final Msp430DebuggerConfig config;
    private final Msp430EngineProvider engine;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final Map<Breakpoint, String> numbers = new ConcurrentHashMap<>();
    private volatile State state = State.STARTING;
    private volatile Snapshot snapshot = Snapshot.EMPTY;
    private volatile Gdb gdb;
    private volatile CurrentLineAnnotation currentLine;
    private volatile CountDownLatch quietStop;   // set while the debugger stops the program for itself
    private List<String> registerNames = List.of();
    private final BreakpointListener breakpoints = new BreakpointListener();

    public Msp430Debugger(ContextProvider context) {
        config = context.lookupFirst(null, Msp430DebuggerConfig.class);
        engine = (Msp430EngineProvider) context.lookupFirst(null, DebuggerEngineProvider.class);
    }

    /** Starts a session for a built program; PICSimLab should be starting, or running, with debugging on. */
    public static void startDebugging(Msp430DebuggerConfig config) {
        SessionProvider session = new SessionProvider() {
            @Override
            public String getSessionName() {
                return config.name;
            }

            @Override
            public String getLocationName() {
                return "PICSimLab " + config.board;
            }

            @Override
            public String getTypeID() {
                return SESSION;
            }

            @Override
            public Object[] getServices() {
                return new Object[] {config};
            }
        };
        DebuggerInfo info = DebuggerInfo.create("MSP430DebuggerInfo", new Object[] {session, config});
        DebuggerManager.getDebuggerManager().startDebugging(info);
    }

    public State getState() {
        return state;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public String getMcu() {
        return config.mcu;
    }

    public Path getIncludeDir() {
        return config.toolchain.include();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    private void setState(State s) {
        State old = state;
        state = s;
        pcs.firePropertyChange(PROP_STATE, old, s);
    }

    // --- starting

    void start() {
        RP.post(() -> {
            try {
                connect();
            } catch (IOException e) {
                config.err.accept("Debugging stopped: " + e.getMessage());
                finish();
            }
        });
    }

    private void connect() throws IOException {
        config.out.accept("Debugging " + config.name + " with " + String.join(" ",
                Gdb.command(config.toolchain.bin, config.picsimlab != null ? config.picsimlab.dir : null)));
        gdb = Gdb.start(Gdb.command(config.toolchain.bin, config.picsimlab != null ? config.picsimlab.dir : null),
                config.toolchain.bin, config.elf, this::event, this::console);
        gdb.call("-gdb-set mi-async on");
        gdb.call("-gdb-set confirm off");
        long until = System.currentTimeMillis() + 120_000;  // PICSimLab's first start builds it
        for (;;) {
            try {
                gdb.call("-target-select remote localhost:" + config.port, 10_000);
                break;
            } catch (Gdb.GdbException e) {
                if (System.currentTimeMillis() > until) {
                    throw new IOException("PICSimLab's GDB server didn't answer on port " + config.port + " ("
                            + e.getMessage() + "). Is PICSimLab running with debugging on?");
                }
                sleep(500);
            }
        }
        config.out.accept("Connected to PICSimLab on port " + config.port + "; loading " + config.elf.getFileName());
        gdb.call("-target-download", 60_000);
        gdb.call("-interpreter-exec console \"monitor reset\"");
        gdb.call("-interpreter-exec console \"maintenance flush register-cache\"");
        List<String> names = new ArrayList<>();
        for (Object o : gdb.call("-data-list-register-names").list("register-names")) {
            names.add((String) o);
        }
        registerNames = names;
        DebuggerManager dm = DebuggerManager.getDebuggerManager();
        dm.addDebuggerListener(breakpoints);
        for (Breakpoint b : dm.getBreakpoints()) {
            b.addPropertyChangeListener(breakpoints);
            insert(b);
        }
        config.out.accept("Running. Breakpoints stop it; Pause (in the Debug menu) stops it anywhere.");
        gdb.call("-exec-continue");
        setState(State.RUNNING);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --- events from gdb

    private void console(String text) {
        String t = text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
        if (!t.isBlank()) {
            config.out.accept(t);
        }
    }

    private void event(Mi.Record r) {
        switch (r.name) {
            case "running":
                if (state != State.STARTING && quietStop == null) {
                    clearCurrentLine();
                    setState(State.RUNNING);
                }
                break;
            case "stopped": {
                CountDownLatch q = quietStop;
                if (q != null) {
                    q.countDown();  // the debugger stopped it to change breakpoints: not the user's stop
                    break;
                }
                if (state == State.STARTING || state == State.FINISHED) {
                    break;           // the stop as gdb connects
                }
                RP.post(() -> stopped(r));
                break;
            }
            case "gdb-exited":
                if (state != State.FINISHED) {
                    config.err.accept("gdb exited.");
                    RP.post(this::finish);
                }
                break;
            default:
                break;
        }
    }

    private void stopped(Mi.Record r) {
        Map<String, Object> frame = r.tuple("frame");
        String reason = r.get("reason");
        String file = (String) frame.get("fullname");
        String line = (String) frame.get("line");
        String where;
        if (file != null && line != null) {
            showLine(Path.of(file), Integer.parseInt(line));
            where = Path.of(file).getFileName() + ":" + line;
        } else {
            clearCurrentLine();
            where = frame.get("addr") + (frame.get("func") != null ? " in " + frame.get("func") : "")
                    + " (no source)";
        }
        String why = reason == null ? "Stopped" : switch (reason) {
            case "breakpoint-hit" -> "Breakpoint";
            case "end-stepping-range" -> "Stepped";
            case "function-finished" -> "Returned";
            case "signal-received" -> switch (String.valueOf(r.get("signal-name"))) {
                case "SIGINT" -> "Paused";
                case "SIGILL" -> "Stopped: an invalid instruction or address";
                default -> "Stopped (" + r.get("signal-meaning") + ")";
            };
            case "watchpoint-trigger", "read-watchpoint-trigger", "access-watchpoint-trigger" -> "Watchpoint";
            default -> "Stopped (" + reason + ")";
        };
        StatusDisplayer.getDefault().setStatusText(config.name + ": " + why + " at " + where);
        takeSnapshot();
        setState(State.STOPPED);
    }

    private void showLine(Path file, int number) {
        FileObject fo = FileUtil.toFileObject(file.toFile());
        clearCurrentLine();
        if (fo == null) {
            return;
        }
        try {
            LineCookie lc = DataObject.find(fo).getLookup().lookup(LineCookie.class);
            if (lc == null) {
                return;
            }
            Line l = lc.getLineSet().getCurrent(number - 1);
            currentLine = new CurrentLineAnnotation(l);
            java.awt.EventQueue.invokeLater(() -> l.show(Line.ShowOpenType.REUSE, Line.ShowVisibilityType.FOCUS));
        } catch (IOException | IndexOutOfBoundsException e) {
            // the file changed since the build
        }
    }

    private void clearCurrentLine() {
        CurrentLineAnnotation a = currentLine;
        currentLine = null;
        if (a != null) {
            a.detach();
        }
    }

    // --- snapshot

    private void takeSnapshot() {
        Map<String, Long> regs = new LinkedHashMap<>();
        byte[] periph = null;
        try {
            for (Object o : gdb.call("-data-list-register-values x").list("register-values")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> v = (Map<String, Object>) o;
                int n = Integer.parseInt((String) v.get("number"));
                String name = n < registerNames.size() ? registerNames.get(n) : "";
                if (!name.isEmpty() && !name.equals("cg")) {
                    regs.put(name, Long.decode((String) v.get("value")));
                }
            }
            Mi.Record mem = gdb.call("-data-read-memory-bytes " + PERIPHERALS_FROM + " "
                    + (PERIPHERALS_TO - PERIPHERALS_FROM));
            if (!mem.list("memory").isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> block = (Map<String, Object>) mem.list("memory").get(0);
                String hex = (String) block.get("contents");
                periph = new byte[hex.length() / 2];
                for (int i = 0; i < periph.length; i++) {
                    periph[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
                }
            }
        } catch (IOException | RuntimeException e) {
            config.err.accept("Couldn't read the registers: " + e.getMessage());
        }
        snapshot = new Snapshot(regs, periph, evaluateWatches());
        pcs.firePropertyChange(PROP_SNAPSHOT, null, snapshot);
    }

    private Map<String, String> evaluateWatches() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Watch w : DebuggerManager.getDebuggerManager().getWatches()) {
            String expr = w.getExpression();
            if (expr == null || expr.isBlank() || out.containsKey(expr)) {
                continue;
            }
            try {
                out.put(expr, gdb.call("-data-evaluate-expression " + Mi.quote(expr)).get("value"));
            } catch (IOException e) {
                out.put(expr, e.getMessage());
            }
        }
        return out;
    }

    /** Evaluates the watches again (one was added or changed while stopped). */
    void refreshWatches() {
        RP.post(() -> {
            if (state == State.STOPPED) {
                Snapshot s = snapshot;
                snapshot = new Snapshot(s.registers, s.peripherals, evaluateWatches());
                pcs.firePropertyChange(PROP_SNAPSHOT, null, snapshot);
            }
        });
    }

    // --- actions

    void resume() {
        run("-exec-continue");
    }

    void stepOver() {
        run("-exec-next");
    }

    void stepInto() {
        run("-exec-step");
    }

    void stepOut() {
        run("-exec-finish");
    }

    private void run(String command) {
        RP.post(() -> {
            if (state != State.STOPPED) {
                return;
            }
            try {
                gdb.call(command);
            } catch (IOException e) {
                config.err.accept(e.getMessage());
                StatusDisplayer.getDefault().setStatusText(config.name + ": " + e.getMessage());
            }
        });
    }

    void pause() {
        RP.post(() -> {
            if (state == State.RUNNING) {
                try {
                    gdb.call("-exec-interrupt");
                } catch (IOException e) {
                    config.err.accept(e.getMessage());
                }
            }
        });
    }

    /** Ends the session: the program runs on in PICSimLab without the debugger. */
    void finish() {
        RP.post(() -> {
            if (state == State.FINISHED) {
                return;
            }
            DebuggerManager dm = DebuggerManager.getDebuggerManager();
            dm.removeDebuggerListener(breakpoints);
            for (Breakpoint b : dm.getBreakpoints()) {
                b.removePropertyChangeListener(breakpoints);
            }
            Gdb g = gdb;
            if (g != null && g.isAlive()) {
                try {
                    if (state == State.RUNNING) {
                        stopQuietly();
                    }
                    g.call("-target-detach", 5000);
                } catch (IOException e) {
                    // closing anyway
                }
                g.close();
            }
            clearCurrentLine();
            for (Breakpoint b : numbers.keySet()) {
                setValidity(b, Breakpoint.VALIDITY.UNKNOWN, null);
            }
            numbers.clear();
            setState(State.FINISHED);
            config.out.accept("Debugging finished; the program runs on in PICSimLab.");
            DebuggerEngine.Destructor d = engine != null ? engine.getDestructor() : null;
            if (d != null) {
                d.killEngine();
            }
        });
    }

    // --- breakpoints

    /** Stops the running program without telling anyone, so breakpoints can be changed (gdb won't while it runs). */
    private boolean stopQuietly() throws IOException {
        if (state != State.RUNNING) {
            return false;
        }
        CountDownLatch latch = new CountDownLatch(1);
        quietStop = latch;
        gdb.call("-exec-interrupt");
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    private void runOnQuietly(boolean stopped) throws IOException {
        if (stopped) {
            gdb.call("-exec-continue");
            quietStop = null;
        }
    }

    /** "file:line" for a breakpoint in one of the program's files, or null. */
    private String location(Breakpoint b) {
        File file = null;
        int line = 0;
        if (b instanceof Msp430LineBreakpoint) {
            file = ((Msp430LineBreakpoint) b).getFile();
            line = ((Msp430LineBreakpoint) b).getLineNumber();
        } else if (b.getClass().getName().endsWith(".CPPLiteBreakpoint")) {
            try {  // C files' breakpoints come from NetBeans' C/C++ support
                Method path = b.getClass().getMethod("getFilePath");
                Method number = b.getClass().getMethod("getLineNumber");
                file = new File((String) path.invoke(b));
                line = (Integer) number.invoke(b);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        if (file == null || line < 1) {
            return null;
        }
        Path p = file.toPath().toAbsolutePath().normalize();
        for (Path s : config.sources) {
            if (s.toAbsolutePath().normalize().equals(p)) {
                return p + ":" + line;
            }
        }
        return null;
    }

    private void insert(Breakpoint b) {
        String where = location(b);
        if (where == null || gdb == null) {
            return;
        }
        try {
            Mi.Record r = gdb.call("-break-insert -f " + Mi.quote(where));
            String n = (String) r.tuple("bkpt").get("number");
            numbers.put(b, n);
            if (!b.isEnabled()) {
                gdb.call("-break-disable " + n);
            }
            setValidity(b, Breakpoint.VALIDITY.VALID, null);
        } catch (IOException e) {
            setValidity(b, Breakpoint.VALIDITY.INVALID, e.getMessage());
        }
    }

    private static void setValidity(Breakpoint b, Breakpoint.VALIDITY v, String reason) {
        if (b instanceof Msp430LineBreakpoint) {
            ((Msp430LineBreakpoint) b).setDebuggerValidity(v, reason);
        } else {
            try {
                b.getClass().getMethod("setCPPValidity", Breakpoint.VALIDITY.class, String.class).invoke(b, v, reason);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // not a breakpoint we can mark
            }
        }
    }

    /** Keeps gdb's breakpoints in step with the IDE's while the session runs. */
    private final class BreakpointListener extends DebuggerManagerAdapter implements PropertyChangeListener {

        @Override
        public String[] getProperties() {
            return new String[] {DebuggerManager.PROP_BREAKPOINTS, DebuggerManager.PROP_WATCHES};
        }

        @Override
        public void breakpointAdded(Breakpoint b) {
            b.addPropertyChangeListener(this);
            change(() -> insert(b));
        }

        @Override
        public void breakpointRemoved(Breakpoint b) {
            b.removePropertyChangeListener(this);
            change(() -> {
                String n = numbers.remove(b);
                if (n != null) {
                    try {
                        gdb.call("-break-delete " + n);
                    } catch (IOException e) {
                        // gone already
                    }
                }
            });
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (Breakpoint.PROP_ENABLED.equals(evt.getPropertyName())) {
                Breakpoint b = (Breakpoint) evt.getSource();
                change(() -> {
                    String n = numbers.get(b);
                    if (n != null) {
                        try {
                            gdb.call((b.isEnabled() ? "-break-enable " : "-break-disable ") + n);
                        } catch (IOException e) {
                            config.err.accept(e.getMessage());
                        }
                    } else if (b.isEnabled()) {
                        insert(b);
                    }
                });
            }
        }

        @Override
        public void watchAdded(Watch w) {
            refreshWatches();
        }

        @Override
        public void watchRemoved(Watch w) {
            refreshWatches();
        }

        private void change(Runnable r) {
            RP.post(() -> {
                if (state == State.FINISHED || state == State.STARTING) {
                    return;
                }
                try {
                    boolean stopped = stopQuietly();
                    r.run();
                    runOnQuietly(stopped);
                } catch (IOException e) {
                    config.err.accept(e.getMessage());
                }
            });
        }
    }
}
