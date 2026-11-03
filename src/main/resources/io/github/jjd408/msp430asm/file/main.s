; MSP430FR2355 assembly (GNU as, msp430-elf-as)

        .text
        .global main

main:   mov.w   #0x5A80, &0x01CC        ; WDTCTL = WDTPW | WDTHOLD: stop the watchdog
        bic.w   #1, &0x0130             ; PM5CTL0: clear LOCKLPM5 so the pins take effect
        bis.b   #0x01, &0x0204          ; P1DIR: P1.0 output
loop:   xor.b   #0x01, &0x0202          ; P1OUT: toggle P1.0
        mov.w   #50000, r15
1:      dec.w   r15
        jnz     1b
        jmp     loop
