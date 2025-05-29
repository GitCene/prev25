package compiler.phase.asmgen;
import java.util.Vector;

import compiler.common.report.Report;
import compiler.phase.imcgen.IMC;
import compiler.phase.imclin.LIN;

public class AsmGenerator {

    public Vector<LIN.DataChunk> dataChunks;
    public Vector<LIN.CodeChunk> codeChunks;

    public IMC.LABEL currLabel = null;

    public AsmGenerator(Vector<LIN.DataChunk> dataChunks, Vector<LIN.CodeChunk> codeChunks) {
        this.dataChunks = dataChunks;
        this.codeChunks = codeChunks;
        //this.asm = new Vector<ASM.AsmChunk>();
    }
    
    public void munch() {
        for (LIN.CodeChunk codeChunk : this.codeChunks) {
            // For this function...
            ASM.AsmChunk asmChunk = new ASM.AsmChunk(codeChunk.frame.label.name, codeChunk.frame, codeChunk.entryLabel, codeChunk.exitLabel);
            for (IMC.Stmt stmt : codeChunk.stmts()) {
                topLevelMatch(stmt, asmChunk);
            }
            AsmGen.asm.add(asmChunk);
        }
    }

    public void emitAll() {
        for (ASM.AsmChunk ch : AsmGen.asm) {
            ch.emit();
        }
    }

    /**
     * Matches (linearized) statement trees to appropriate tiles.
     * 
     * @param stmt
     */
    public void topLevelMatch(IMC.Stmt stmt, ASM.AsmChunk asmChunk) {
        if (stmt instanceof IMC.LABEL lab) {
            asmChunk.setLabel(lab);
        } else if (stmt instanceof IMC.JUMP jum) {
            ASM.JMP jmp = new ASM.JMP((IMC.NAME) jum.addr);
            asmChunk.put(jmp);
        } else if (stmt instanceof IMC.CJUMP cj) {
            // Our CJUMP is BNZ to positive label.
            //ASM.BNZ bnz = new ASM.BNZ((IMC.TEMP)cj.cond, (IMC.NAME)cj.posAddr, (IMC.NAME)cj.negAddr);
            ASM.BRANCH bnz = new ASM.BRANCH(ASM.BRANCH.Oper.BNZ, (IMC.TEMP)cj.cond, (IMC.NAME)cj.posAddr, (IMC.NAME)cj.negAddr);
            asmChunk.put(bnz);
        } else if (stmt instanceof IMC.MOVE mov) {
            matchMove(mov, asmChunk);
        } else {
            throw new Report.Error("Internal error: Undefined sequence for tiling: " + stmt);
        }
    }

    public void matchMove(IMC.MOVE move, ASM.AsmChunk asmChunk) {
        if (move.src instanceof IMC.BINOP binop) {
            matchBinop(binop, (IMC.TEMP)move.dst, asmChunk);
        } else if (move.src instanceof IMC.UNOP unop) {
            matchUnop(unop, (IMC.TEMP)move.dst, asmChunk);
        } else if (move.src instanceof IMC.MEM8 oct) {
            matchLoadOcta(oct, (IMC.TEMP)move.dst, asmChunk);
        } else if (move.src instanceof IMC.MEM1 sing) {
            matchLoadSingle(sing, (IMC.TEMP)move.dst, asmChunk);
        } else if (move.src instanceof IMC.CALL call) {
            matchCallAssign(call, (IMC.TEMP)move.dst, asmChunk);
        } else if (move.dst instanceof IMC.MEM8 oct) {
            if (move.src instanceof IMC.CONST con) {
                IMC.TEMP tempDes = new IMC.TEMP();
                matchMovReg(con, tempDes, asmChunk);
                matchStoreOcta(tempDes, oct, asmChunk);
            } else {
                matchStoreOcta((IMC.TEMP)move.src, oct, asmChunk);
            }
        } else if (move.dst instanceof IMC.MEM1 sing) {
            if (move.src instanceof IMC.CONST con) {
                IMC.TEMP tempDes = new IMC.TEMP();
                matchMovReg(con, tempDes, asmChunk);
                matchStoreSingle(tempDes, sing, asmChunk);
            } else {
                matchStoreSingle((IMC.TEMP)move.src, sing, asmChunk);
            } 
        } else if (move.dst instanceof IMC.TEMP temp) {
            matchMovReg(move.src, temp, asmChunk);
        } else {
            throw new Report.Error("Internal error: Undefined move pattern for tiling: " + move);
        }
    }

    public void matchBinop(IMC.BINOP binop, IMC.TEMP dest, ASM.AsmChunk asmChunk) {
        // TODO: ensure that two immediate values cannot appear in
        // ADD,
        IMC.Expr arg1 = binop.fstExpr;
        IMC.Expr arg2 = binop.sndExpr;
        if (arg1 instanceof IMC.CONST) {
            IMC.Expr temp = arg1;
            arg1 = arg2;
            arg2 = temp;
        }
        switch (binop.oper) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case AND:
            case OR: 
                ASM.BINOP op = new ASM.BINOP(binop.oper, dest, (IMC.TEMP)arg1, arg2);
                asmChunk.put(op);
                return;
            case MOD:
                // TODO: Optimize mod with powers of 2 as a shift.
                // Otherwise, on MMIX: mod dest, arg1, arg2 == div temp, arg1, arg2; move dest, regSpecial
                // TODO: make work on mmix later when assigning registers!
                IMC.TEMP tempDest = new IMC.TEMP();
                ASM.BINOP div = new ASM.BINOP(IMC.BINOP.Oper.DIV, tempDest, (IMC.TEMP)arg1, arg2);
                // TODO: Ensure that $dest is the special modulo look register!
                asmChunk.put(div);
                return;
            // TODO: optimize the if statements with this.
            // This is very bad code that generates very bad code.
            case EQU:
            case NEQ:
            case LEQ:
            case GEQ:
            case LTH:
            case GTH:
                IMC.TEMP temp = new IMC.TEMP();
                ASM.CMP cmp = new ASM.CMP(temp, (IMC.TEMP) arg1, arg2);
                asmChunk.put(cmp);
                // temp is 0 -> arg1 == arg2. temp is -1 -> arg1 < arg2. temp is 1 -> arg1 > arg2.
                // ugly code
                ASM.CSET cset;
                IMC.CONST val = new IMC.CONST(1L);
                switch (binop.oper) {
                    case EQU:
                        cset = new ASM.CSET(ASM.CSET.Oper.SZ, dest, temp, val, true);
                        break;
                    case NEQ:
                        cset = new ASM.CSET(ASM.CSET.Oper.SNZ, dest, temp, val, true);
                        break;
                    case LEQ:
                        cset = new ASM.CSET(ASM.CSET.Oper.SNN, dest, temp, val, true);
                        break;
                    case GEQ:
                        cset = new ASM.CSET(ASM.CSET.Oper.SNP, dest, temp, val, true);
                        break;
                    case LTH:
                        cset = new ASM.CSET(ASM.CSET.Oper.SN, dest, temp, val, true);
                        break;
                    case GTH:
                        cset = new ASM.CSET(ASM.CSET.Oper.SP, dest, temp, val, true);
                        break;
                    default:
                        throw new Report.InternalError();
                }
                asmChunk.put(cset);
                return;
        }
    }

    public void matchUnop(IMC.UNOP unop, IMC.TEMP dest, ASM.AsmChunk asmChunk) {
        IMC.CONST nul = new IMC.CONST(0L);
        switch(unop.oper) {
            case NOT:
                ASM.BITOP not = new ASM.BITOP(ASM.BITOP.Oper.NOR, dest, (IMC.TEMP)unop.subExpr, nul);
                asmChunk.put(not);
                return;
            case NEG:
                IMC.TEMP tempDest = new IMC.TEMP();
                ASM.BITOP nott = new ASM.BITOP(ASM.BITOP.Oper.NOR, tempDest, (IMC.TEMP)unop.subExpr, nul);
                ASM.BINOP add = new ASM.BINOP(IMC.BINOP.Oper.ADD, dest, tempDest, new IMC.CONST(1L));
                asmChunk.put(nott);                
                asmChunk.put(add);
                return;                
        }
    }

    public void matchLoadOcta(IMC.MEM8 mem, IMC.TEMP dest, ASM.AsmChunk asmChunk) {
        matchMEM(dest, mem.addr, 8L, false, asmChunk);
    }
    
    public void matchLoadSingle(IMC.MEM1 mem, IMC.TEMP dest, ASM.AsmChunk asmChunk) {
        matchMEM(dest, mem.addr, 1L, false, asmChunk);
    }
    
    public void matchStoreOcta(IMC.TEMP src, IMC.MEM8 mem, ASM.AsmChunk asmChunk) {
        matchMEM(src, mem.addr, 8L, true, asmChunk);
    }
    
    public void matchStoreSingle(IMC.TEMP src, IMC.MEM1 mem, ASM.AsmChunk asmChunk) {
        matchMEM(src, mem.addr, 8L, true, asmChunk);
    }

    public void matchMEM(IMC.TEMP srcdest, IMC.Expr memaddr, Long size, boolean isStore, ASM.AsmChunk asmChunk) {
        ASM.MEMO mem; 
        if (memaddr instanceof IMC.TEMP temp)
            mem = new ASM.MEMO(isStore, size, srcdest, temp, new IMC.CONST(0L));
        else if (memaddr instanceof IMC.NAME name) {
            IMC.TEMP addrDest = new IMC.TEMP();
            ASM.LDA lda = new ASM.LDA(addrDest, name);
            asmChunk.put(lda);
            mem = new ASM.MEMO(isStore, size, srcdest, addrDest, new IMC.CONST(0L));
        }
        else throw new Report.Error("Weird types in matchMEM.");
        asmChunk.put(mem);
    }

    public void matchCallAssign(IMC.CALL call, IMC.TEMP dest, ASM.AsmChunk asmChunk) {
        // FOR NOW: Call will become a PUSHJ.
        IMC.NAME funName = (IMC.NAME)call.addr;
        switch (funName.label.name) {
            case "_puts":
                IMC.Expr arg = call.args.get(1);
                if (arg instanceof IMC.NAME name) {
                    ASM.LDA lda = new ASM.LDA(new IMC.TEMP(), name);
                    asmChunk.put(lda);
                    ASM.TRAP trap = new ASM.TRAP("Fputs", "Stdout");
                    asmChunk.put(trap);
                } else throw new Report.Error("Yet undefined puts behaviour.");
                return;

            default:
                ASM.PUSHJ pushj = new ASM.PUSHJ(new IMC.TEMP(), (IMC.NAME)call.addr);
                asmChunk.put(pushj);
        }

        // Pushj will need size of stack frame.
        // Ensure that return register is treated properly during register allocation.
        // MMIX special registers.
    }

    public void matchMovReg(IMC.Expr src, IMC.TEMP dest, ASM.AsmChunk asmChunk) {
        //IMC.TEMP zeroRegister = new IMC.TEMP();
        // TODO: hardwire this one to MMIX's r0, which is always 0.
        //ASM.BINOP add = new ASM.BINOP(IMC.BINOP.Oper.ADD, dest, zeroRegister, src);
        ASM.SET set = new ASM.SET(dest, src);
        asmChunk.put(set);
    }
}
