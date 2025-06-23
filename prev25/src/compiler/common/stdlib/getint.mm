% name: _getint, entry: gint1, exit: gint9, FP: T0, RV: T1

% Local vars need 56 size and args need 24, so frame is 96

_getint SUB      $0,SP,72
         GET      $1,rJ
         STO      $1,$0,0
         STO      FP,$0,8
         SET      FP,SP
         SUB      SP,SP,104
         JMP      g5
g5       SUB      $2,FP,24
         SET      $1,0
         ADD      $2,$2,$1
         SET      $1,21
         STO      $1,SP,8
         STO      $2,SP,0
         SET      $255,SP
         TRAP     0,Fgets,StdIn
         SUB      $2,FP,40
         SET      $1,0
         STO      $1,$2,0
         SUB      $1,FP,48
         SET      $2,1
         STO      $2,$1,0
         SUB      $3,FP,32
         SUB      $2,FP,24
         SUB      $1,FP,40
         LDO      $1,$1,0
         MUL      $1,$1,1
         ADD      $1,$2,$1
         LDB      $1,$1,0
         STO      $1,$3,0
         SUB      $2,FP,56
         SET      $1,0
         STO      $1,$2,0
g7       SUB      $1,FP,32
         LDO      $1,$1,0
         CMP      $1,$1,32
         ZSP      $1,$1,1
         BNZ      $1,g8
g9       JMP      g10
g8       SUB      $1,FP,56
         SUB      $2,FP,56
         LDO      $2,$2,0
         MUL      $2,$2,10
         STO      $2,$1,0
         SUB      $1,FP,40
         LDO      $1,$1,0
         CMP      $1,$1,0
         ZSZ      $2,$1,1
         SUB      $1,FP,32
         LDO      $1,$1,0
         CMP      $1,$1,45
         ZSZ      $1,$1,1
         AND      $1,$2,$1
         BNZ      $1,g11
g12      SUB      $2,FP,56
         SUB      $1,FP,56
         LDO      $3,$1,0
         SUB      $1,FP,32
         LDO      $1,$1,0
         ADD      $1,$3,$1
         SUB      $1,$1,48
         STO      $1,$2,0
         JMP      g13
g11      SUB      $2,FP,48
         SET      $1,1
         NEG      $1,$1
         SET      $1,$1
         STO      $1,$2,0
         JMP      g13
g13      SUB      $2,FP,40
         SUB      $1,FP,40
         LDO      $1,$1,0
         ADD      $1,$1,1
         STO      $1,$2,0
         SUB      $3,FP,32
         SUB      $1,FP,24
         SUB      $2,FP,40
         LDO      $2,$2,0
         MUL      $2,$2,1
         ADD      $1,$1,$2
         LDB      $1,$1,0
         STO      $1,$3,0
         JMP      g7
g10      SUB      $1,FP,56
         LDO      $2,$1,0
         SUB      $1,FP,48
         LDO      $1,$1,0
         MUL      $0,$2,$1
         SET      $0,$0
         JMP      g6
g6       SET      SP,FP
         SUB      $2,FP,72
         LDO      $1,$2,0
         PUT      rJ,$1
         LDO      FP,$2,8
         POP      1,0