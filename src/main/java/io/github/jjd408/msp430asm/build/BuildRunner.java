/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.build;

import io.github.jjd408.msp430asm.Settings;
import io.github.jjd408.msp430asm.assemble.Build;
import io.github.jjd408.msp430asm.assemble.PicSimLab;
import io.github.jjd408.msp430asm.assemble.Toolchain;
import io.github.jjd408.msp430asm.debug.Msp430Debugger;
import io.github.jjd408.msp430asm.debug.Msp430DebuggerConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

/**
 * Runs MSP430 builds (of one file or a project) and, if asked, the result in PICSimLab, reporting in an Output
 * window tab where lines naming a file and a line number open it there.
 */
public final class BuildRunner {

    /** What to do after building. */
    public enum Mode { BUILD, RUN, DEBUG }

    /** One build: saves what it builds, then builds, printing to {@code out}. */
    public interface Job {
        void save() throws IOException;

        /** Builds, or returns null when there was only cleaning to do. */
        Build.Result build(Toolchain toolchain, Consumer<String> out) throws IOException, InterruptedException;

        /** The board to run on. */
        String board();

        /** Called after the build, e.g. to refresh the output folder. */
        void built();

        /** The program's name, for a debugging session. */
        default String name() {
            return "MSP430";
        }

        /** The source files, for a debugging session's breakpoints. */
        default List<Path> sources() {
            return List.of();
        }

        /** The device. */
        default String mcu() {
            return Settings.mcu();
        }
    }

    private static final RequestProcessor RP = new RequestProcessor("MSP430 build", 1);

    private BuildRunner() {
    }

    /** Builds one file into build/&lt;name&gt;.hex beside it. */
    static void buildFile(DataObject dob, Mode mode) {
        FileObject fo = dob.getPrimaryFile();
        File file = FileUtil.toFile(fo);
        if (file == null) {
            IOProvider.getDefault().getIO("MSP430 " + fo.getNameExt(), false).getErr()
                    .println(fo.getNameExt() + " isn't on disk, so it can't be built.");
            return;
        }
        start("MSP430 " + fo.getNameExt(), mode, new Job() {
            @Override
            public String name() {
                return fo.getNameExt();
            }

            @Override
            public List<Path> sources() {
                return List.of(file.toPath());
            }

            @Override
            public void save() throws IOException {
                org.openide.cookies.SaveCookie s = dob.getLookup().lookup(org.openide.cookies.SaveCookie.class);
                if (s != null) {
                    s.save();
                }
            }

            @Override
            public Build.Result build(Toolchain tc, Consumer<String> out) throws IOException, InterruptedException {
                return new Build(tc, Settings.mcu()).build(file.toPath(), file.toPath().getParent().resolve("build"),
                        out);
            }

            @Override
            public String board() {
                return Settings.board();
            }

            @Override
            public void built() {
                fo.getParent().refresh();
            }
        });
    }

    /** Builds in the background, printing to a tab; then runs or debugs the program in PICSimLab if asked. */
    public static void start(String tab, Mode mode, Job job) {
        boolean run = mode == Mode.RUN;
        InputOutput io = IOProvider.getDefault().getIO(tab, false);
        io.select();
        OutputWriter out = io.getOut();
        OutputWriter err = io.getErr();
        try {
            out.reset();
        } catch (IOException e) {
            // a fresh tab
        }
        boolean[] keepOpen = {false};  // a debugging session goes on writing to the tab
        RP.post(() -> {
            try {
                job.save();
                Toolchain tc = Settings.toolchain();
                if (tc == null) {
                    err.println("MSP430-GCC wasn't found. Set its bin folder in Tools > Options > Miscellaneous > "
                            + "MSP430.");
                    return;
                }
                Build.Result r = job.build(tc, line -> print(out, err, line));
                job.built();
                if (r != null && r.ok && mode == Mode.DEBUG) {
                    keepOpen[0] = debug(job, tc, r, out, err);
                    return;
                }
                if (r == null || !r.ok || !run) {
                    return;
                }
                PicSimLab sim = Settings.picsimlab();
                if (sim == null) {
                    err.println("PICSimLab with the MSP430 backend wasn't found. Set its folder (the one with run.sh) "
                            + "in Tools > Options > Miscellaneous > MSP430.");
                    return;
                }
                String board = job.board();
                out.println("$ " + String.join(" ", sim.command(board, r.hex)));
                out.println("Starting PICSimLab on the " + board + " board; its first start takes a few seconds.");
                Process p = sim.launch(board, r.hex);
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(p.getInputStream(),
                        StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = rd.readLine()) != null) {
                        out.println(line);
                    }
                }
                out.println("PICSimLab exited (" + p.waitFor() + ").");
            } catch (IOException e) {
                err.println("Build failed: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (!keepOpen[0]) {
                    out.close();
                    err.close();
                }
            }
        });
    }

    private static final RequestProcessor PICSIMLAB_OUTPUT = new RequestProcessor("PICSimLab output", 4);

    /**
     * Starts a debugging session on a built program: in the PICSimLab already debugging on its port if there is
     * one (the session loads the program into it), else in a new one started with debugging on.
     */
    private static boolean debug(Job job, Toolchain tc, Build.Result r, OutputWriter out, OutputWriter err)
            throws IOException {
        PicSimLab sim = Settings.picsimlab();
        if (sim == null) {
            err.println("PICSimLab with the MSP430 backend wasn't found. Set its folder (the one with run.sh) "
                    + "in Tools > Options > Miscellaneous > MSP430.");
            return false;
        }
        String board = job.board();
        int port = PicSimLab.DEBUG_PORT;
        if (PicSimLab.listening(port)) {
            out.println("PICSimLab is already debugging on port " + port + "; the program is loaded into it.");
        } else {
            out.println("$ " + String.join(" ", sim.debugCommand(board, r.hex)));
            out.println("Starting PICSimLab on the " + board + " board with debugging on; its first start takes "
                    + "a few seconds.");
            Process p = sim.launchDebug(board, r.hex);
            PICSIMLAB_OUTPUT.post(() -> {
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(p.getInputStream(),
                        StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = rd.readLine()) != null) {
                        out.println(line);
                    }
                    out.println("PICSimLab exited (" + p.waitFor() + ").");
                } catch (IOException e) {
                    // the tab was closed
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        Msp430Debugger.startDebugging(new Msp430DebuggerConfig(job.name(), r.elf, job.sources(), tc, sim, board,
                job.mcu(), port, out::println, line -> print(out, err, line)));
        return true;
    }

    private static final Pattern AT_LINE = Pattern.compile("^(/[^:]+):(\\d+):");

    /** Prints a line of build output; one naming a file and a line opens it there when clicked. */
    private static void print(OutputWriter out, OutputWriter err, String line) {
        String shown = line.replaceFirst("^.*?/bin/ld: ", "ld: ");
        Matcher m = AT_LINE.matcher(shown);
        if (m.find()) {
            FileObject fo = FileUtil.toFileObject(new File(m.group(1)));
            if (fo != null) {
                try {
                    err.println(shown, new GoToLine(DataObject.find(fo), Integer.parseInt(m.group(2))), true);
                    return;
                } catch (IOException e) {
                    // print it plainly
                }
            }
        }
        PrintWriter w = shown.contains("rror") || shown.contains("undefined reference") ? err : out;
        w.println(shown);
    }

    /** Opens a file at a line. */
    private static final class GoToLine implements OutputListener {
        private final DataObject dob;
        private final int line;

        GoToLine(DataObject dob, int line) {
            this.dob = dob;
            this.line = line;
        }

        @Override
        public void outputLineAction(OutputEvent ev) {
            LineCookie lc = dob.getLookup().lookup(LineCookie.class);
            if (lc == null) {
                return;
            }
            try {
                Line l = lc.getLineSet().getOriginal(line - 1);
                l.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
            } catch (IndexOutOfBoundsException e) {
                // the file has changed since
            }
        }

        @Override
        public void outputLineSelected(OutputEvent ev) {
        }

        @Override
        public void outputLineCleared(OutputEvent ev) {
        }
    }
}
