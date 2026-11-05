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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.jjd408.msp430asm.assemble.Build;
import io.github.jjd408.msp430asm.assemble.PicSimLab;
import io.github.jjd408.msp430asm.assemble.Toolchain;
import io.github.jjd408.msp430asm.project.ProjectFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** GDB's MI against the simulator's GDB server (msp430_run --gdb), with the starter project's program. */
class GdbTest {

    @Test
    void parsesRecords() {
        Mi.Record r = Mi.parse("12^done,bkpt={number=\"1\",file=\"delay.S\",line=\"7\"},list=[a=\"1\",b=\"2\"]");
        assertEquals('^', r.kind);
        assertEquals(12, r.token);
        assertEquals("done", r.name);
        assertEquals("7", r.tuple("bkpt").get("line"));
        assertEquals(List.of("1", "2"), r.list("list"));
        Mi.Record s = Mi.parse("~\"Hello\\n\\\"x\\\"\"");
        assertEquals("Hello\n\"x\"", s.name);
        assertEquals("stopped", Mi.parse("*stopped,reason=\"breakpoint-hit\"").name);
    }

    @Test
    void debugsInTheSimulator(@TempDir Path dir) throws Exception {
        Toolchain tc = Toolchain.find(null);
        assumeTrue(tc != null && tc.include() != null, "MSP430-GCC isn't installed");
        PicSimLab sim = PicSimLab.find(null);
        assumeTrue(sim != null, "picsimlab-msp430 isn't here");
        Path runner = sim.dir.resolve("src/sim_backend/msp430/tests/msp430_run");
        assumeTrue(Files.isExecutable(runner), "msp430_run isn't built (run its run_tests.sh)");

        ProjectFiles.create(dir, "starter");
        Build.Result built = new Build(tc, "msp430fr2355").buildAll(ProjectFiles.sources(dir),
                List.of(dir.resolve("src")), "", dir.resolve("build"), "starter", s -> { });
        assertTrue(built.ok);
        int port = 2400 + (int) (ProcessHandle.current().pid() % 500);
        Process sim430 = new ProcessBuilder(runner.toString(), built.hex.toString(), "--seconds", "5", "--tick",
                "1e6", "--gdb", Integer.toString(port)).redirectErrorStream(true).start();
        BlockingQueue<Mi.Record> stops = new LinkedBlockingQueue<>();
        try (Gdb gdb = Gdb.start(Gdb.command(tc.bin, sim.dir), tc.bin, built.elf, r -> {
            if (r.name.equals("stopped")) {
                stops.add(r);
            }
        }, s -> { })) {
            gdb.call("-gdb-set mi-async on");
            connect(gdb, port);
            next(stops, null);  // the program stops as gdb connects
            Path delay = dir.resolve("src/delay.S");
            Mi.Record bp = gdb.call("-break-insert -f " + Mi.quote(delay + ":7"));
            assertEquals("7", bp.tuple("bkpt").get("line"));

            gdb.call("-exec-continue");
            Map<String, Object> frame = next(stops, "breakpoint-hit").tuple("frame");
            assertTrue(((String) frame.get("fullname")).endsWith("/delay.S"), frame.toString());
            assertEquals("7", frame.get("line"));
            Mi.Record r12 = gdb.call("-data-evaluate-expression $r12");
            assertEquals("50000", r12.get("value"));

            gdb.call("-exec-next");
            assertEquals("8", next(stops, "end-stepping-range").tuple("frame").get("line"));

            // P1OUT was toggled once before the call
            Mi.Record mem = gdb.call("-data-read-memory-bytes 0x0202 1");
            @SuppressWarnings("unchecked")
            Map<String, Object> block = (Map<String, Object>) mem.list("memory").get(0);
            assertEquals("01", block.get("contents"));

            gdb.call("-break-delete");
            gdb.call("-exec-continue");
            Thread.sleep(300);
            gdb.call("-exec-interrupt");
            assertEquals("signal-received", next(stops, null).get("reason"));
            gdb.call("-target-detach");
        } finally {
            sim430.destroy();
        }
    }

    static void connect(Gdb gdb, int port) throws Exception {
        for (int i = 0; ; i++) {
            try {
                gdb.call("-target-select remote localhost:" + port, 5000);
                return;
            } catch (Gdb.GdbException e) {
                if (i > 30) {
                    throw e;
                }
                Thread.sleep(200);
            }
        }
    }

    static Mi.Record next(BlockingQueue<Mi.Record> stops, String reason) throws Exception {
        Mi.Record r = stops.poll(10, TimeUnit.SECONDS);
        assertTrue(r != null, "no stop");
        if (reason != null) {
            assertEquals(reason, r.get("reason"), r.toString());
        }
        return r;
    }
}
