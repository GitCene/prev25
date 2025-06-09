package compiler.phase.asmgen;
import java.io.FilePermission;
import java.util.HashMap;
import java.util.Vector;

import compiler.common.report.Report;
import compiler.phase.asmgen.ASM.BINOP.Oper;
import compiler.phase.imcgen.IMC;
import compiler.phase.imclin.LIN;
import compiler.phase.memory.MEM;

public class AsmGenerator {

    public Vector<LIN.DataChunk> dataChunks;
    public Vector<LIN.CodeChunk> codeChunks;
    //public HashMap<MEM.Temp, Integer> constraints = new HashMap<MEM.Temp, Integer>();
    public IMC.TEMP FP = new IMC.TEMP();
    public IMC.TEMP SP = new IMC.TEMP();

    public IMC.LABEL currLabel = null;

    public AsmGenerator(Vector<LIN.DataChunk> dataChunks, Vector<LIN.CodeChunk> codeChunks) {
        this.dataChunks = dataChunks;
        this.codeChunks = codeChunks;
        //this.asm = new Vector<ASM.AsmChunk>();
        constrain(this.FP.temp, 253);
        constrain(this.SP.temp, 254);
    }

    public void constrain(MEM.Temp treg, int preg) {
        AsmGen.constraints.put(treg, preg);
    }
    
    public void munch() {
        for (LIN.CodeChunk codeChunk : this.codeChunks) {
            // For this function...
            ASM.AsmChunk asmChunk = new ASM.AsmChunk(codeChunk.frame.label.name, codeChunk.frame, codeChunk.entryLabel, codeChunk.exitLabel);
            for (IMC.Stmt stmt : codeChunk.stmts()) {
                // Every RV should be constrained to $0
                constrain(codeChunk.frame.RV, 0);
                // Constrain FP to frame pointer code
                constrain(codeChunk.frame.FP, 253);
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
                //constraints.put(dest.temp, 1006);
                ASM.GET get = new ASM.GET(dest, new IMC.CONST(6));
                asmChunk.put(div);
                asmChunk.put(get);
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

    /*
     *         
     * 
     *  addNative("malloc", Interpreter::native_malloc);
        addNative("new", Interpreter::native_malloc);
        addNative("free", Interpreter::native_free);
        addNative("del", Interpreter::native_free);
        addNative("die", Interpreter::native_die);
        addNative("exit", Interpreter::native_exit);
        addNative("putint", Interpreter::native_putint);
        addNative("putint_hex", Interpreter::native_putint_hex);
        addNative("putint_bin", Interpreter::native_putint_bin);
        addNative("putuint", Interpreter::native_putuint);
        addNative("putchar", Interpreter::native_putchar);
        addNative("puts", Interpreter::native_puts);
        addNative("getint", Interpreter::native_getint);
        addNative("getchar", Interpreter::native_getchar);
        addNative("gets", Interpreter::native_gets);
     */

    public void matchCallAssign(IMC.CALL call, IMC.TEMP dest, ASM.AsmChunk asmChunk) {
        // FOR NOW: Call will become a PUSHJ.
        IMC.NAME funName = (IMC.NAME)call.addr;
        switch (funName.label.name) {
            case "_puts":

                IMC.Expr arg = call.args.get(1);
                if (arg instanceof IMC.NAME name) {
                    IMC.TEMP sysreg = new IMC.TEMP();
                    ASM.LDA lda = new ASM.LDA(sysreg, name);
                    constrain(sysreg.temp, 255);
                    asmChunk.put(lda);
                } else if (arg instanceof IMC.TEMP t) {
                    // There is a pointer to a string, loaded to a register.
                    // TODO: enforce that that register becomes $255.
                    constrain(t.temp, 255);
                } else throw new Report.Error("Yet undefined puts behaviour.");
                ASM.TRAP trap = new ASM.TRAP("Fputs", "StdOut");
                asmChunk.put(trap);
                return;
        
            case "_putint":
                return;
                

            default:
                // Putting the args on stack
                // TODO: premisli the use of stack pointer: ali je SL na [SP] ali na [SL-8]?
                int offset = 8;
                
                IMC.TEMP stptr = new IMC.TEMP();
                IMC.CONST off = new IMC.CONST(offset);

                if (call.args.size() > 0) {
                    ASM.SET set = new ASM.SET(stptr, this.SP);
                    asmChunk.put(set);
                }
                for (IMC.Expr argm : call.args) {
                    ASM.BINOP sub = new ASM.BINOP(IMC.BINOP.Oper.SUB, stptr, stptr, off);
                    asmChunk.put(sub);
                    ASM.MEMO store;
                    if (argm instanceof IMC.TEMP temp) {
                        store = new ASM.MEMO(true, 8L, temp, stptr, new IMC.CONST(0L));
                    } else if (argm instanceof IMC.CONST cons) {
                        IMC.TEMP temp = new IMC.TEMP();
                        ASM.SET set = new ASM.SET(temp, cons);
                        asmChunk.put(set);
                        store = new ASM.MEMO(true, 8L, temp, stptr, new IMC.CONST(0L));
                    } else if (argm instanceof IMC.NAME name) {
                        IMC.TEMP temp = new IMC.TEMP();
                        ASM.LDA lda = new ASM.LDA(temp, name);
                        asmChunk.put(lda);
                        store = new ASM.MEMO(true, 8L, temp, stptr, new IMC.CONST(0L));
                    } else throw new Report.Error("What did you put in your CALL? " + argm);
                    asmChunk.put(store);
                    offset = offset - 8;
                }

                IMC.TEMP pushjRegister = new IMC.TEMP();
                ASM.PUSHJ pushj = new ASM.PUSHJ(pushjRegister, (IMC.NAME)call.addr);
                //asmChunk.put(localsCount);
                asmChunk.put(pushj);
                // TODO: check If return value is needed...
                // TJ. if 'dest' is live ... otherwise kill it?
                ASM.SET copyRetVal = new ASM.SET(dest, pushjRegister);
                asmChunk.put(copyRetVal);

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
