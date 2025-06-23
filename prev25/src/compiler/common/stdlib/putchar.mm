% _putchar: locs: 8, args: 16
_putchar SUB    $0,SP,24
        GET     $1,rJ
        STO     $1,$0,0
        STO     FP,$0,8
        SET     FP,SP
        SUB     SP,SP,48
        JMP     ptc0
ptc0    ADD     $0,FP,8
        LDO     $1,$0,0
        SL      $1,$1,56
        SUB     $255,FP,8
        STO     $1,$255,0
        TRAP    0,Fputs,StdOut
        SET     $0,0
        JMP     ptc1
ptc1    SET     SP,FP
        SUB     $2,FP,24
        LDO     $1,$2,0
        PUT     rJ,$1
        LDO     FP,$2,8
        POP     1,0