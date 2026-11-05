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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A running {@code msp430-elf-gdb --interpreter=mi2}: commands go in with a token, their results come back
 * matched to it, and exec records ({@code *stopped}, {@code *running}) and console text go to listeners.
 */
public final class Gdb implements AutoCloseable {

    /** An {@code ^error} from GDB. */
    public static final class GdbException extends IOException {
        GdbException(String message) {
            super(message);
        }
    }

    /** Every command and record at FINE (-J-Dio.github.jjd408.msp430asm.debug.Gdb.level=FINE). */
    private static final Logger LOG = Logger.getLogger(Gdb.class.getName());

    private final Process process;
    private final Writer in;
    private final Map<Integer, CompletableFuture<Mi.Record>> pending = new ConcurrentHashMap<>();
    private final Consumer<Mi.Record> events;
    private final Consumer<String> console;
    private int token;

    private Gdb(Process process, Consumer<Mi.Record> events, Consumer<String> console) {
        this.process = process;
        this.in = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
        this.events = events;
        this.console = console;
        Thread reader = new Thread(this::read, "MSP430 gdb reader");
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * The command that runs TI's gdb: through picsimlab-msp430's {@code tools/msp430-gdb} when there is one
     * (it lets TI's gdb run on systems with only ncurses 6), else the toolchain's own.
     */
    public static List<String> command(Path toolchainBin, Path picsimlabDir) {
        if (picsimlabDir != null) {
            Path wrapper = picsimlabDir.resolve("tools").resolve("msp430-gdb");
            if (Files.isExecutable(wrapper)) {
                return List.of(wrapper.toString());
            }
        }
        return List.of(toolchainBin.resolve("msp430-elf-gdb").toString());
    }

    /**
     * Starts gdb on a program.
     *
     * @param events exec, status and notify records ({@code *stopped} and so on)
     * @param console what gdb and the target print
     */
    public static Gdb start(List<String> command, Path toolchainBin, Path elf, Consumer<Mi.Record> events,
            Consumer<String> console) throws IOException {
        List<String> cmd = new ArrayList<>(command);
        cmd.addAll(List.of("--interpreter=mi2", "-nx", "-q", elf.toString()));
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        Path root = toolchainBin.getParent();
        if (root != null) {
            pb.environment().put("MSP430_GCC", root.toString());  // for the wrapper
        }
        return new Gdb(pb.start(), events, console);
    }

    private void read() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                LOG.log(Level.FINE, "< {0}", line);
                Mi.Record rec = Mi.parse(line);
                switch (rec.kind) {
                    case '^': {
                        CompletableFuture<Mi.Record> f = pending.remove(rec.token);
                        if (f != null) {
                            f.complete(rec);
                        }
                        break;
                    }
                    case '*':
                    case '+':
                    case '=':
                        events.accept(rec);
                        break;
                    case '~':
                    case '@':
                    case '&':
                        console.accept(rec.name);
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            // gdb went away
        }
        GdbException gone = new GdbException("gdb exited");
        pending.values().forEach(f -> f.completeExceptionally(gone));
        pending.clear();
        events.accept(new Mi.Record('*', -1, "gdb-exited", Map.of()));
    }

    /** Sends a command; the future completes with its result record. */
    public synchronized CompletableFuture<Mi.Record> send(String command) {
        int t = ++token;
        CompletableFuture<Mi.Record> f = new CompletableFuture<>();
        pending.put(t, f);
        try {
            LOG.log(Level.FINE, "> {0}{1}", new Object[] {t, command});
            in.write(t + command + "\n");
            in.flush();
        } catch (IOException e) {
            pending.remove(t);
            f.completeExceptionally(e);
        }
        return f;
    }

    /** Sends a command and waits for its result; an {@code ^error} becomes a GdbException with gdb's message. */
    public Mi.Record call(String command, long timeoutMs) throws IOException {
        try {
            Mi.Record r = send(command).get(timeoutMs, TimeUnit.MILLISECONDS);
            if ("error".equals(r.name)) {
                throw new GdbException(r.get("msg"));
            }
            return r;
        } catch (TimeoutException e) {
            throw new GdbException("gdb didn't answer " + command);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof IOException ? (IOException) e.getCause() : new IOException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GdbException("interrupted");
        }
    }

    public Mi.Record call(String command) throws IOException {
        return call(command, 10_000);
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public void close() {
        if (process.isAlive()) {
            send("-gdb-exit");
            try {
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroy();
                }
            } catch (InterruptedException e) {
                process.destroy();
                Thread.currentThread().interrupt();
            }
        }
    }
}
