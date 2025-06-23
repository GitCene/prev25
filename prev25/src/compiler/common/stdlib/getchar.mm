% name: _getchar, entry: L1, exit: L2, FP: T0, RV: T1

% Local vars need 8 size and args need 24, so frame is 48

_getchar SUB      $0,SP,24
         GET      $1,rJ
         STO      $1,$0,0
         STO      FP,$0,8
         SET      FP,SP
         SUB      SP,SP,56
         JMP      gtc1
gtc1       SUB      $1,FP,8
         SET      $2,0
         ADD      $2,$1,$2
         SET      $1,2
         STO      $1,SP,8
         STO      $2,SP,0
         SET      $255,SP
         TRAP     0,Fgets,StdIn
         SUB      $1,FP,8
         SET      $2,0
         ADD      $0,$1,$2
         LDB      $0,$0,0
         SET      $0,$0
         JMP      gtc2
gtc2       SET      SP,FP
         SUB      $2,FP,24
         LDO      $1,$2,0
         PUT      rJ,$1
         LDO      FP,$2,8
         POP      1,0