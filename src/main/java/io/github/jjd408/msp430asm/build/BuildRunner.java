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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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

    /** One build: saves what it builds, then builds, printing to {@code out}. */
    public interface Job {
        void save() throws IOException;

        /** Builds, or returns null when there was only cleaning to do. */
        Build.Result build(Toolchain toolchain, Consumer<String> out) throws IOException, InterruptedException;

        /** The board to run on. */
        String board();

        /** Called after the build, e.g. to refresh the output folder. */
        void built();
    }

    private static final RequestProcessor RP = new RequestProcessor("MSP430 build", 1);

    private BuildRunner() {
    }

    /** Builds one file into build/&lt;name&gt;.hex beside it. */
    static void buildFile(DataObject dob, boolean run) {
        FileObject fo = dob.getPrimaryFile();
        File file = FileUtil.toFile(fo);
        if (file == null) {
            IOProvider.getDefault().getIO("MSP430 " + fo.getNameExt(), false).getErr()
                    .println(fo.getNameExt() + " isn't on disk, so it can't be built.");
            return;
        }
        start("MSP430 " + fo.getNameExt(), run, new Job() {
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

    /** Builds in the background, printing to a tab, and runs the hex in PICSimLab when {@code run} is set. */
    public static void start(String tab, boolean run, Job job) {
        InputOutput io = IOProvider.getDefault().getIO(tab, false);
        io.select();
        OutputWriter out = io.getOut();
        OutputWriter err = io.getErr();
        try {
            out.reset();
        } catch (IOException e) {
            // a fresh tab
        }
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
                out.close();
                err.close();
            }
        });
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
