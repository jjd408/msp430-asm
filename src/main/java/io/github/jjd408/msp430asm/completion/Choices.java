/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.completion;

import io.github.jjd408.msp430asm.help.DeviceInfo;
import io.github.jjd408.msp430asm.help.HelpText;
import io.github.jjd408.msp430asm.help.InstructionHelp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Builds the completion choices for a context (without any NetBeans UI, so it can be tested on its own).
 * At a mnemonic: instructions in each size they take, or directives after a dot. In operands: after '&amp;'
 * peripheral registers and labels; after '#' constants and labels; after '@' registers; otherwise registers and
 * labels, plus peripheral registers and constants once a few letters narrow them down.
 */
public final class Choices {

    /** One completion choice. */
    public static final class Choice {
        public enum Kind { INSTRUCTION, DIRECTIVE, REGISTER, LABEL, PERIPHERAL, CONSTANT }

        public final Kind kind;
        public final String text;
        /** A short note shown on the right of the list. */
        public final String note;
        public final Supplier<String> help;

        Choice(Kind kind, String text, String note, Supplier<String> help) {
            this.kind = kind;
            this.text = text;
            this.note = note;
            this.help = help;
        }

        @Override
        public String toString() {
            return kind + " " + text;
        }
    }

    /** Peripheral registers and constants are offered without a mark only after this many letters. */
    private static final int MIN_PREFIX_UNMARKED = 2;

    private Choices() {
    }

    public static List<Choice> of(Context ctx, String text, DeviceInfo device) {
        List<Choice> out = new ArrayList<>();
        if (ctx.kind == Context.Kind.NONE) {
            return out;
        }
        String prefix = ctx.prefix.toLowerCase(Locale.ROOT);
        boolean upper = !ctx.prefix.isEmpty() && Character.isUpperCase(ctx.prefix.charAt(0));

        if (ctx.kind == Context.Kind.MNEMONIC) {
            if (prefix.startsWith(".")) {
                for (Map.Entry<String, String> d : InstructionHelp.directives().entrySet()) {
                    if (d.getKey().startsWith(prefix)) {
                        out.add(new Choice(Choice.Kind.DIRECTIVE, d.getKey(), "directive",
                                () -> HelpText.directive(d.getKey(), d.getValue())));
                    }
                }
                return out;
            }
            for (InstructionHelp.Entry e : InstructionHelp.instructions().values()) {
                List<String> forms = new ArrayList<>();
                forms.add(e.mnemonic);
                for (char size : e.sizes.toCharArray()) {
                    forms.add(e.mnemonic + "." + Character.toLowerCase(size));
                }
                for (String form : forms) {
                    if (form.startsWith(prefix)) {
                        String shown = upper ? form.toUpperCase(Locale.ROOT) : form;
                        out.add(new Choice(Choice.Kind.INSTRUCTION, shown, e.group, () -> HelpText.instruction(e)));
                    }
                }
            }
            return out;
        }

        // Operands
        boolean registers = ctx.mark == 0 || ctx.mark == '@';
        boolean labels = ctx.mark != '@';
        boolean peripherals = ctx.mark == '&' || ctx.mark == 0 && prefix.length() >= MIN_PREFIX_UNMARKED;
        boolean constants = ctx.mark == '#' || ctx.mark == 0 && prefix.length() >= MIN_PREFIX_UNMARKED;

        if (registers) {
            for (String r : List.of("R4", "R5", "R6", "R7", "R8", "R9", "R10", "R11", "R12", "R13", "R14", "R15",
                    "SP", "PC", "SR", "R0", "R1", "R2", "R3")) {
                if (r.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    String shown = upper || prefix.isEmpty() ? r : r.toLowerCase(Locale.ROOT);
                    String help = InstructionHelp.register(r);
                    out.add(new Choice(Choice.Kind.REGISTER, shown, "register", () -> HelpText.cpuRegister(r, help)));
                }
            }
        }
        if (labels) {
            for (Map.Entry<String, Integer> l : Labels.scan(text).entrySet()) {
                if (l.getKey().toLowerCase(Locale.ROOT).startsWith(prefix) && !l.getKey().equals(ctx.prefix)) {
                    out.add(new Choice(Choice.Kind.LABEL, l.getKey(), "label, line " + l.getValue(),
                            () -> HelpText.label(l.getKey(), l.getValue())));
                }
            }
        }
        if (peripherals) {
            for (DeviceInfo.Register r : device.registers.values()) {
                if (r.name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(new Choice(Choice.Kind.PERIPHERAL, r.name,
                            r.address != null ? r.size + " at " + r.address : r.size,
                            () -> HelpText.peripheral(r, device)));
                }
            }
        }
        if (constants) {
            for (DeviceInfo.Constant c : device.constants.values()) {
                if (c.name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(new Choice(Choice.Kind.CONSTANT, c.name,
                            "= " + c.value + (c.register != null ? "  " + c.register : ""), () -> HelpText.constant(c)));
                }
            }
        }
        return out;
    }

    /** Help for the word under the caret in this context, or null: what hover shows. */
    public static String helpFor(Context ctx, String word, String text, DeviceInfo device) {
        Map<String, Integer> labels = Labels.scan(text);
        if (ctx.kind == Context.Kind.MNEMONIC) {
            if (labels.containsKey(word)) {
                return HelpText.label(word, labels.get(word));
            }
            InstructionHelp.Entry e = InstructionHelp.instruction(word);
            if (e != null) {
                return HelpText.instruction(e);
            }
            String d = InstructionHelp.directive(word);
            return d == null ? null : HelpText.directive(word.toLowerCase(Locale.ROOT), d);
        }
        if (ctx.kind == Context.Kind.OPERAND) {
            String r = InstructionHelp.register(word);
            if (r != null) {
                return HelpText.cpuRegister(word, r);
            }
            if (labels.containsKey(word)) {
                return HelpText.label(word, labels.get(word));
            }
            DeviceInfo.Register p = device.registers.get(word);
            if (p != null) {
                return HelpText.peripheral(p, device);
            }
            DeviceInfo.Constant c = device.constants.get(word);
            if (c != null) {
                return HelpText.constant(c);
            }
        }
        return null;
    }
}
