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

import io.github.jjd408.msp430asm.help.DeviceInfo;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.ui.Constants;
import org.netbeans.spi.viewmodel.ModelEvent;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.NodeModel;
import org.netbeans.spi.viewmodel.TableModel;
import org.netbeans.spi.viewmodel.TreeExpansionModel;
import org.netbeans.spi.viewmodel.TreeModel;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.util.NbBundle.Messages;

/**
 * The Variables window while stopped: the CPU registers (SR's flags spelled out), and every peripheral register
 * of the device by name, grouped by module (P1, TB0, UCB1...), with their descriptions from TI's header.
 */
@DebuggerServiceRegistration(path = Msp430Debugger.SESSION + "/LocalsView",
        types = {TreeModel.class, NodeModel.class, TableModel.class, TreeExpansionModel.class})
@Messages({
    "LBL_Cpu=CPU registers",
    "LBL_Peripherals=Peripheral registers",
    "TTP_Cpu=R0 (PC) to R15 as the program left them",
    "TTP_Peripherals=The device's peripheral registers, read without side effects"
})
public final class VariablesModel implements TreeModel, NodeModel, TableModel, TreeExpansionModel,
        PropertyChangeListener {

    /** A row: a group of registers, or a register. */
    static final class Node {
        final String name;
        final String value;
        final String description;
        final List<Node> children;

        Node(String name, String value, String description, List<Node> children) {
            this.name = name;
            this.value = value;
            this.description = description;
            this.children = children;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String ICONS = "org/netbeans/modules/debugger/resources/localsView/";
    private static final String[] SR_FLAGS = {"C", "Z", "N", "GIE", "CPUOFF", "OSCOFF", "SCG0", "SCG1", "V"};
    // Module of a register, from its name: ports, timers, eUSCIs, smart analog combos, comparators, others
    private static final Pattern MODULE = Pattern.compile(
            "^(P[1-6J]|TB[0-3]|UC[AB][01]|SAC[0-3]|CP[01]|ADC|CRC|RTC|CS|WDT|SFR|SYS|PMM|PM5|FRCTL|GC|BAK|ICC|"
            + "MPY|MAC|OP2|RES|SUMEXT)");
    private static final Map<String, String> MODULE_NAMES = Map.of("MAC", "MPY32", "OP2", "MPY32", "RES", "MPY32",
            "SUMEXT", "MPY32", "MPY", "MPY32", "PM5", "PMM", "GC", "FRCTL");
    private static volatile DeviceInfo device;
    private static volatile String deviceKey;

    private final Msp430Debugger debugger;
    private final List<ModelListener> listeners = new CopyOnWriteArrayList<>();
    private final Set<String> expanded = new HashSet<>(Set.of(Bundle.LBL_Cpu()));
    private volatile List<Node> top = List.of();

    public VariablesModel(ContextProvider context) {
        debugger = context.lookupFirst(null, Msp430Debugger.class);
        debugger.addPropertyChangeListener(this);
        rebuild();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        rebuild();
        for (ModelListener l : listeners) {
            l.modelChanged(new ModelEvent.TreeChanged(this));
        }
    }

    private void rebuild() {
        if (debugger.getState() != Msp430Debugger.State.STOPPED) {
            top = List.of();
            return;
        }
        Msp430Debugger.Snapshot s = debugger.getSnapshot();
        top = List.of(new Node(Bundle.LBL_Cpu(), null, Bundle.TTP_Cpu(), cpu(s.registers)),
                new Node(Bundle.LBL_Peripherals(), null, Bundle.TTP_Peripherals(),
                        peripherals(s.peripherals, debugger.getIncludeDir(), debugger.getMcu())));
    }

    static List<Node> cpu(Map<String, Long> regs) {
        List<Node> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : regs.entrySet()) {
            String name = e.getKey().toUpperCase();
            long v = e.getValue();
            String value;
            if (name.equals("SR")) {
                StringBuilder flags = new StringBuilder();
                for (int b = SR_FLAGS.length - 1; b >= 0; b--) {
                    if ((v & (1L << b)) != 0) {
                        flags.append(flags.length() > 0 ? " " : "").append(SR_FLAGS[b]);
                    }
                }
                value = String.format("0x%04X  %s", v, flags);
            } else if (name.equals("PC") || name.equals("SP")) {
                value = String.format("0x%05X", v);
            } else {
                value = String.format("0x%05X  (%d)", v, v);
            }
            String desc = name.equals("PC") ? "R0, program counter" : name.equals("SP") ? "R1, stack pointer"
                    : name.equals("SR") ? "R2, status register" : name;
            out.add(new Node(name, value, desc, null));
        }
        return out;
    }

    static String module(String register) {
        Matcher m = MODULE.matcher(register);
        if (!m.find()) {
            return "Other";
        }
        return MODULE_NAMES.getOrDefault(m.group(1), m.group(1));
    }

    static List<Node> peripherals(byte[] mem, Path include, String mcu) {
        DeviceInfo d = device(include, mcu);
        Map<String, List<DeviceInfo.Register>> groups = new TreeMap<>();
        for (DeviceInfo.Register r : d.registers.values()) {
            if (r.address == null) {
                continue;
            }
            int a = Integer.decode(r.address);
            if (a >= Msp430Debugger.PERIPHERALS_FROM && a < Msp430Debugger.PERIPHERALS_TO) {
                groups.computeIfAbsent(module(r.name), k -> new ArrayList<>()).add(r);
            }
        }
        List<Node> out = new ArrayList<>();
        for (Map.Entry<String, List<DeviceInfo.Register>> g : groups.entrySet()) {
            List<Node> regs = new ArrayList<>();
            g.getValue().sort(Comparator.comparingInt(r -> Integer.decode(r.address)));
            for (DeviceInfo.Register r : g.getValue()) {
                regs.add(new Node(r.name, value(mem, r), r.description + " (" + r.address + ")", null));
            }
            out.add(new Node(g.getKey(), null, null, regs));
        }
        return out;
    }

    private static String value(byte[] mem, DeviceInfo.Register r) {
        if (mem == null) {
            return "?";
        }
        int at = Integer.decode(r.address) - Msp430Debugger.PERIPHERALS_FROM;
        int bytes = "byte".equals(r.size) ? 1 : "address".equals(r.size) ? 4 : 2;
        if (at < 0 || at + bytes > mem.length) {
            return "?";
        }
        long v = 0;
        for (int i = bytes - 1; i >= 0; i--) {
            v = (v << 8) | (mem[at + i] & 0xFF);
        }
        if (bytes == 1) {
            String bin = String.format("%8s", Long.toBinaryString(v)).replace(' ', '0');
            return String.format("0x%02X  (%s %s)", v, bin.substring(0, 4), bin.substring(4));
        }
        return bytes == 2 ? String.format("0x%04X", v) : String.format("0x%05X", v & 0xFFFFF);
    }

    private static DeviceInfo device(Path include, String mcu) {
        String key = include + "|" + mcu;
        if (!key.equals(deviceKey)) {
            device = include == null ? DeviceInfo.empty() : DeviceInfo.load(include, mcu);
            deviceKey = key;
        }
        return device;
    }

    // --- TreeModel

    @Override
    public Object getRoot() {
        return ROOT;
    }

    @Override
    public Object[] getChildren(Object parent, int from, int to) throws UnknownTypeException {
        List<Node> c = children(parent);
        return c.subList(Math.min(from, c.size()), Math.min(to, c.size())).toArray();
    }

    private List<Node> children(Object parent) throws UnknownTypeException {
        if (parent == ROOT) {
            return top;
        }
        if (parent instanceof Node) {
            List<Node> c = ((Node) parent).children;
            return c == null ? List.of() : c;
        }
        throw new UnknownTypeException(parent);
    }

    @Override
    public boolean isLeaf(Object node) throws UnknownTypeException {
        if (node == ROOT) {
            return false;
        }
        if (node instanceof Node) {
            return ((Node) node).children == null;
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public int getChildrenCount(Object node) throws UnknownTypeException {
        return children(node).size();
    }

    // --- NodeModel

    @Override
    public String getDisplayName(Object node) throws UnknownTypeException {
        if (node == ROOT) {
            return "";
        }
        if (node instanceof Node) {
            return ((Node) node).name;
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public String getIconBase(Object node) throws UnknownTypeException {
        if (node == ROOT) {
            return null;
        }
        if (node instanceof Node) {
            return ICONS + (((Node) node).children != null ? "LocalVariablesGroup" : "LocalVariable");
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public String getShortDescription(Object node) throws UnknownTypeException {
        if (node == ROOT) {
            return "";
        }
        if (node instanceof Node) {
            return ((Node) node).description;
        }
        throw new UnknownTypeException(node);
    }

    // --- TableModel

    @Override
    public Object getValueAt(Object node, String columnID) throws UnknownTypeException {
        if (node instanceof Node) {
            Node n = (Node) node;
            if (Constants.LOCALS_VALUE_COLUMN_ID.equals(columnID) && n.value != null) {
                return n.value;
            }
            return "";
        }
        if (node == ROOT) {
            return "";
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public boolean isReadOnly(Object node, String columnID) {
        return true;
    }

    @Override
    public void setValueAt(Object node, String columnID, Object value) {
    }

    // --- TreeExpansionModel

    @Override
    public boolean isExpanded(Object node) {
        return node instanceof Node && expanded.contains(((Node) node).name);
    }

    @Override
    public void nodeExpanded(Object node) {
        if (node instanceof Node) {
            expanded.add(((Node) node).name);
        }
    }

    @Override
    public void nodeCollapsed(Object node) {
        if (node instanceof Node) {
            expanded.remove(((Node) node).name);
        }
    }

    // --- listeners

    @Override
    public void addModelListener(ModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeModelListener(ModelListener l) {
        listeners.remove(l);
    }
}
