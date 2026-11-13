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

/** HTML help for words in MSP430 assembly, shown in completion's documentation and in hover tooltips. */
public final class HelpText {

    private HelpText() {
    }

    public static String instruction(InstructionHelp.Entry e) {
        StringBuilder b = new StringBuilder("<html><b><tt>").append(esc(e.syntax)).append("</tt></b>");
        b.append(" <small>(").append(e.group).append(')').append("</small>");
        b.append("<p>").append(esc(e.operation)).append("</p>");
        if (e.emulation != null) {
            b.append("<p>Assembles as <tt>").append(esc(e.emulation)).append("</tt>.</p>");
        }
        b.append("<p><table cellspacing=0 cellpadding=1><tr>");
        String[] names = {"V", "N", "Z", "C"};
        for (int i = 0; i < 4; i++) {
            b.append("<td><tt>").append(names[i]).append(' ').append(flag(e.flags.charAt(i))).append("</tt>&nbsp;&nbsp;</td>");
        }
        b.append("</tr></table>");
        if (!e.sizes.isEmpty()) {
            b.append("<p>Sizes: ");
            String sep = "";
            for (char c : e.sizes.toCharArray()) {
                b.append(sep).append(c == 'B' ? "<tt>.b</tt> byte" : c == 'W' ? "<tt>.w</tt> word (the default)"
                        : "<tt>.a</tt> 20-bit address word");
                sep = ", ";
            }
            b.append("</p>");
        }
        return b.toString();
    }

    private static String flag(char c) {
        switch (c) {
            case '*':
                return "set by the result";
            case '0':
                return "cleared";
            case '1':
                return "set";
            default:
                return "unchanged";
        }
    }

    public static String directive(String name, String help) {
        return "<html><b><tt>" + esc(name) + "</tt></b> <small>(directive)</small><p>" + esc(help) + "</p>";
    }

    public static String cpuRegister(String name, String help) {
        return "<html><b><tt>" + esc(name.toUpperCase()) + "</tt></b> <small>(CPU register)</small><p>" + esc(help)
                + "</p>";
    }

    public static String peripheral(DeviceInfo.Register r, DeviceInfo device) {
        StringBuilder b = new StringBuilder("<html><b><tt>").append(esc(r.name)).append("</tt></b> <small>(")
                .append(r.size).append(" register");
        if (r.address != null) {
            b.append(" at ").append(r.address);
        }
        b.append(")</small>");
        if (!r.description.isEmpty()) {
            b.append("<p>").append(esc(r.description)).append("</p>");
        }
        StringBuilder bits = new StringBuilder();
        int n = 0;
        for (DeviceInfo.Constant c : device.constants.values()) {
            // The fields themselves (WDTIS, WDTHOLD), not each value's variant (WDTIS_3, WDTIS__32K)
            boolean variant = c.name.matches(".*_\\d+") || c.name.contains("__");
            if (r.name.equals(c.register) && !c.description.isEmpty() && !variant && n < 24) {
                bits.append("<tr><td><tt>").append(esc(c.name)).append("</tt></td><td><tt>").append(esc(c.value))
                        .append("</tt></td><td>").append(esc(c.description)).append("</td></tr>");
                n++;
            }
        }
        if (n > 0) {
            b.append("<p>Bits:<table cellspacing=0 cellpadding=1>").append(bits).append("</table>");
        }
        return b.toString();
    }

    public static String constant(DeviceInfo.Constant c) {
        StringBuilder b = new StringBuilder("<html><b><tt>").append(esc(c.name)).append(" = ").append(esc(c.value))
                .append("</tt></b> <small>(constant");
        if (c.register != null) {
            b.append(" for ").append(esc(c.register));
        }
        b.append(")</small>");
        if (!c.description.isEmpty()) {
            b.append("<p>").append(esc(c.description)).append("</p>");
        }
        return b.toString();
    }

    public static String section(String name, String what) {
        return "<html><b><tt>" + esc(name) + "</tt></b> <small>(section)</small><p>For " + esc(what) + ".</p>";
    }

    public static String vector(DeviceInfo.Vector v) {
        StringBuilder b = new StringBuilder("<html><b><tt>").append(esc(v.preferred())).append("</tt></b> <small>(interrupt vector ")
                .append(v.number).append(")</small><p>");
        if (v.headerName != null) {
            b.append("The vector the header calls <tt>").append(esc(v.headerName)).append("</tt>");
        } else {
            b.append("Interrupt vector ").append(v.number);
        }
        if (v.address != null) {
            b.append(", at ").append(esc(v.address));
        }
        b.append(". The section holds one word: the address of the routine that handles it.</p><pre>")
                .append("        .section ").append(esc(v.preferred())).append(",\"ax\",@progbits\n")
                .append("        .word   my_isr</pre>");
        if (v.namedSection != null) {
            b.append("<p>Also <tt>").append(esc(v.section)).append("</tt>, by number.</p>");
        }
        return b.toString();
    }

    public static String label(String name, int line) {
        return "<html><b><tt>" + esc(name) + "</tt></b> <small>(label)</small><p>Defined on line " + line
                + ". Ctrl+click to go there.</p>";
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
