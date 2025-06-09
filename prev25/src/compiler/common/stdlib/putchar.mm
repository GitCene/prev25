% _putchar: locs: 8, args: 16
_putchar STO    FP,SP,16
        SET     FP,SP
        ADD     SP,SP,24 % locs 8 + FP 8 + default 8
        JMP     N0
N0      SUB     $0,FP,8
        LDB     $1,$0,0
        ADD     $255,FP,8
        STO     $1,$255,0
        TRAP    0,Fputs,StdOut
        SET     $0,0
        JMP     N1
N1      SET     SP,FP
        LDO     FP,SP,16
        POP     1,0