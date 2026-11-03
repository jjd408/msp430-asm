#include <msp430.h>          /* .S files go through the C preprocessor */

        .text
        .global main
main:   mov.w   #WDTPW|WDTHOLD, &WDTCTL   ; stop the watchdog
        bis.b   #BIT0, &P1DIR
loop:   xor.b   #BIT0, &P1OUT
        mova    @R12+, R13
        pushm.a #4, R10
        mov.b   #'A, R15
        .word   0x1234, 0b1010, 017
1:      dec.w   R15
        jnz     1b
        .ascii  "MSP430"
