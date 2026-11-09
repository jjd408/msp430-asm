/*
 * MSP430 Assembly for Apache NetBeans
 * Copyright (C) 2026 jjd408
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. See the LICENSE file.
 */
package io.github.jjd408.msp430asm.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import org.junit.jupiter.api.Test;

class AsmFormatterTest {

    private final AsmFormatter f = new AsmFormatter();

    private String fmt(String text) {
        return f.format(text);
    }

    @Test
    void columns() {
        assertEquals(""
                + "1:\n"
                + "        bit.w   #UCBUSY, &UCB1STATW                     ; make sure it is idle\n"
                + "        jnz     1b\n"
                + "        mov.w   #8, R15\n"
                + "        call    #delay                                  ; wait for 2 uS\n",
                fmt("1:\n"
                + "        bit.w  #UCBUSY, &UCB1STATW                      ;make sure it is idle\n"
                + "        jnz 1b\n"
                + "        mov.w #8,R15\n"
                + "        call #delay                                      ; wait for 2 uS   \n"));
    }

    @Test
    void labels() {
        assertEquals("1:      dec.w   R15\n", fmt("1: dec.w R15"));
        assertEquals("spi_send:\n", fmt("   spi_send:   "));
        assertEquals("events: .skip   16                                      ; the event queue\n",
                fmt("        events:.skip 16; the event queue"));
        // too long for column 8: the mnemonic follows after one space, on the same line
        assertEquals("spi_deselect: bit.w #UCBUSY, &UCB1STATW\n", fmt("spi_deselect: bit.w #UCBUSY,&UCB1STATW"));
        assertEquals("loop:                                                   ; the main loop\n",
                fmt("loop: ;the main loop"));
        assertEquals("a: b:   nop\n", fmt("a:b: nop"));
    }

    @Test
    void longParts() {
        assertEquals("        .section .bss\n", fmt("\t.section\t.bss"));
        assertEquals("        mov.w   #TBSSEL__ACLK|MC__UP|TBCLR, &TB2CTL     ; start it\n",
                fmt("mov.w #TBSSEL__ACLK|MC__UP|TBCLR, &TB2CTL ;start it"));
        String longCode = "        mov.w   #WDTPW|WDTSSEL__ACLK|WDTIS__8192K|WDTCNTCL, &WDTCTL";
        assertEquals(longCode + " ; kick\n", fmt(longCode + ";kick"));
    }

    @Test
    void operands() {
        assertEquals("        mov.b   bit_mask(R15), R15\n", fmt("mov.b bit_mask(R15) ,R15"));
        assertEquals("        bis.b   #BIT0 | BIT1, &P5DIR\n", fmt("bis.b #BIT0 | BIT1,&P5DIR"));
        assertEquals("        .byte   1, 2, 3\n", fmt(".byte 1,2 , 3"));
        assertEquals("        .ascii  \"a,b;c\", \"d\"\n", fmt(".ascii \"a,b;c\",\"d\""));
        assertEquals("        mov.b   #';, R12                                ; a semicolon\n",
                fmt("mov.b #';,R12 ; a semicolon"));
        assertEquals("        .word   MAX(1,2), 3\n", fmt(".word MAX(1,2),3"));
        assertEquals("        ret\n", fmt("ret"));
    }

    @Test
    void comments() {
        assertEquals("; delay: waits\n", fmt("; delay: waits"));
        assertEquals("        ; display buffer\n", fmt("    ; display buffer"));
        assertEquals(";   in:      none\n", fmt(";   in:      none"));  // the text is kept as it is
        assertEquals("        nop                                             ;----\n", fmt("nop ;----"));
        assertEquals("        nop                                             // x\n", fmt("nop //x"));
    }

    @Test
    void leftAlone() {
        assertEquals("#include <msp430.h>\n", fmt("#include <msp430.h>"));
        assertEquals("#define A (1 | \\\n   2)\n        nop\n", fmt("#define A (1 | \\\n   2)\n nop"));
        assertEquals("  /* a\n   b; c */\n        nop\n", fmt("  /* a\n   b; c */\nnop"));
        assertEquals("x = 5\n", fmt("x = 5"));
        assertEquals("  nop /* hi */\n", fmt("  nop /* hi */"));
        assertEquals("\n\n        nop\n", fmt("  \n\t\nnop"));
    }

    @Test
    void lineEndings() {
        assertEquals("        nop\n        ret\n", fmt("nop\r\nret\r\n"));
        assertEquals("", fmt(""));
    }

    @Test
    void otherColumns() {
        AsmFormatter g = new AsmFormatter(4, 12, 30);
        assertEquals("1:  mov.w   #8, R15           ; x\n", g.format("1: mov.w #8,R15 ;x"));
    }

    @Test
    void reformatsADocumentPart() throws Exception {
        PlainDocument doc = new PlainDocument();
        doc.insertString(0, "a: nop\n mov #1,R12\n ret", null);
        // positions at the start of each line (where breakpoints are kept) stay on their lines
        Position[] lineStarts = new Position[3];
        for (int i = 0; i < 3; i++) {
            lineStarts[i] = doc.createPosition(doc.getDefaultRootElement().getElement(i).getStartOffset());
        }
        // a selection from the second line to the start of the third leaves the third alone
        int start = doc.getText(0, doc.getLength()).indexOf(" mov");
        int end = doc.getText(0, doc.getLength()).indexOf(" ret");
        AsmReformatTaskFactory.reformat(doc, start, end, f);
        assertEquals("a: nop\n        mov     #1, R12\n ret", doc.getText(0, doc.getLength()));
        // the whole file, and a newline at the end
        AsmReformatTaskFactory.reformat(doc, 0, doc.getLength(), f);
        assertEquals("a:      nop\n        mov     #1, R12\n        ret\n", doc.getText(0, doc.getLength()));
        for (int i = 0; i < 3; i++) {
            assertEquals(i, doc.getDefaultRootElement().getElementIndex(lineStarts[i].getOffset()));
        }
    }

    @Test
    void formattingTwiceChangesNothing() {
        String once = fmt("lcd:\n mov.w #UCSWRST,&UCB1CTLW0 ;reset\n; x\n#include \"sol.h\"\nfb: .skip 168*18\n");
        assertEquals(once, fmt(once));
    }
}
