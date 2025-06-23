package compiler.phase.asmgen;
import java.io.FilePermission;
import java.util.HashMap;
import java.util.Vector;

import compiler.common.report.Report;
import compiler.phase.asmgen.ASM.AsmChunk;
import compiler.phase.asmgen.ASM.BINOP.Oper;
import compiler.phase.imcgen.IMC;
import compiler.phase.imclin.LIN;
import compiler.phase.memory.MEM;
import compiler.phase.regall.REG;

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
        System.out.printf("Matching move: %s , dst: %s, src: %s\n", move, move.dst, move.src);
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
        /* 
        } else if (move.src instanceof IMC.NAME name) {
            matchMovName(name, (IMC.TEMP)move.dst, asmChunk);
            */ // Moved into matchMovReg.
        } else if (move.dst instanceof IMC.MEM8 oct) {
            if (move.src instanceof IMC.CONST con) {
                IMC.TEMP tempDes = new IMC.TEMP();
                matchMovReg(con, tempDes, asmChunk);
                matchStoreOcta(tempDes, oct, asmChunk);
            } else if (move.src instanceof IMC.NAME name) {
                IMC.TEMP nameHolder = new IMC.TEMP();
                matchMovReg(name, nameHolder, asmChunk);
                matchStoreOcta(nameHolder, oct, asmChunk);
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
            //Moving an expr to a TEMP register.
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
        // TODO: ensure to do smth about NAMES.
        arg1 = fixName(arg1, asmChunk);
        arg2 = fixName(arg2, asmChunk);

        if (arg1 instanceof IMC.CONST) {
            IMC.Expr temp = arg1;
            arg1 = arg2;
            arg2 = temp;
        }
        // Bodge for array multiplying
        if (arg1 instanceof IMC.CONST c1 && arg2 instanceof IMC.CONST c2) {
            if (binop.oper == IMC.BINOP.Oper.MUL) {
                asmChunk.put(new ASM.SET(dest, new IMC.CONST(c1.value*c2.value)));
                return;
            }
        }
        switch (binop.oper) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case AND:
            case OR: 
                try {

                    ASM.BINOP op = new ASM.BINOP(binop.oper, dest, (IMC.TEMP)arg1, arg2);
                    asmChunk.put(op);
                } catch (Exception e) {
                    throw new Report.Error("The binop " + arg1 + binop.oper + arg2 + " caused an error.");
                }
                return;
            case MOD:
                // TODO: Optimize mod with powers of 2 as a shift.
                // Otherwise, on MMIX: mod dest, arg1, arg2 == div temp, arg1, arg2; move dest, regSpecial
                // TODO: make work on mmix later when assigning registers!
                IMC.TEMP tempDest = new IMC.TEMP();
                // where tf does this get arg...?
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
                        cset = new ASM.CSET(ASM.CSET.Oper.SNP, dest, temp, val, true);
                        break;
                    case GEQ:
                        cset = new ASM.CSET(ASM.CSET.Oper.SNN, dest, temp, val, true);
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
            // TODO: fix this and also make boolean NOT work.
                ASM.BITOP not = new ASM.BITOP(ASM.BITOP.Oper.NOR, dest, (IMC.TEMP)fixName(unop.subExpr, asmChunk), nul);
                asmChunk.put(not);
                return;
            case NEG:
            // This is stupid, since MMIX has a NEG instruction.
                IMC.TEMP tempDest = new IMC.TEMP();
                ASM.BITOP nott = new ASM.BITOP(ASM.BITOP.Oper.NOR, tempDest, (IMC.TEMP)fixName(unop.subExpr, asmChunk), nul);
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
        matchMEM(src, mem.addr, 1L, true, asmChunk);
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
        // MMIX builtins are here ! :3
        String switchname = "";
        boolean callAsVar = false;
        IMC.TEMP callVar = null;
        if (call.addr instanceof IMC.NAME name) {
            switchname = name.label.name;
        } else if (call.addr instanceof IMC.TEMP temp) {
            callAsVar = true;
            callVar = temp;
        } else throw new Report.Error("What are you even calling?");
        IMC.Expr arg;
        ASM.TRAP trap;
        switch (switchname) {
            case "_puts":

                arg = call.args.get(1);
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
                trap = new ASM.TRAP("Fputs", "StdOut");
                asmChunk.put(trap);
                return;

            case "_gets": //fun gets(buf: ^char, size: int): int
                //Store the (buffer_addr, buffer_size) on top of the stack.
                IMC.Expr sizeArg = call.args.get(2);
                if (sizeArg instanceof IMC.NAME name) {
                    // Probably useless
                    IMC.TEMP addrHolder = new IMC.TEMP();
                    ASM.LDA lda = new ASM.LDA(addrHolder, name);
                    asmChunk.put(lda);
                    IMC.TEMP valHolder = new IMC.TEMP();
                    ASM.MEMO loadSize = new ASM.MEMO(false, 8L, valHolder, addrHolder, new IMC.CONST(0L));
                    asmChunk.put(loadSize);
                    ASM.MEMO writeSize = new ASM.MEMO(true, 8L, valHolder, SP, new IMC.CONST(8L));
                    asmChunk.put(writeSize);
                } else if (sizeArg instanceof IMC.CONST con) {
                    IMC.TEMP conHolder = new IMC.TEMP();
                    ASM.SET set = new ASM.SET(conHolder, con);
                    asmChunk.put(set);
                    ASM.MEMO writeSize = new ASM.MEMO(true, 8L, conHolder, this.SP, new IMC.CONST(8L));
                    asmChunk.put(writeSize);
                } else if (sizeArg instanceof IMC.TEMP t) {
                    ASM.MEMO writeSize = new ASM.MEMO(true, 8L, t, this.SP, new IMC.CONST(8L));
                    asmChunk.put(writeSize);
                } else throw new Report.Error("Yet undefined gets behaviour.");

                arg = call.args.get(1);
                // This is a char pointer... can be a name or a temp.
                if (arg instanceof IMC.NAME name) {
                    IMC.TEMP addrHolder = new IMC.TEMP();
                    ASM.LDA lda = new ASM.LDA(addrHolder, name);
                    asmChunk.put(lda);
                    ASM.MEMO writePtr = new ASM.MEMO(true, 8L, addrHolder, SP, new IMC.CONST(0L));
                    asmChunk.put(writePtr);
                } else if (arg instanceof IMC.TEMP temp) {
                    ASM.MEMO writePtr = new ASM.MEMO(true, 8L, temp, SP, new IMC.CONST(0L));
                    asmChunk.put(writePtr);
                } else throw new Report.Error("Yet undefined gets behaviour.");

                // The args are stored in memory, now just put SP into $255.
                IMC.TEMP sysreg = new IMC.TEMP();
                ASM.SET sets = new ASM.SET(sysreg, this.SP);
                constrain(sysreg.temp, 255);
                asmChunk.put(sets);
                trap = new ASM.TRAP("Fgets", "StdIn");
                asmChunk.put(trap);
                return;

            default:
                // Writing the args for the calling function in my frame. 
                // [SP + 0] <- SL
                // [SP + 8] <- arg1
                //   ....
                int offset = 8;
                boolean firstArg = true;
                IMC.TEMP stptr = new IMC.TEMP();

                if (call.args.size() > 0) {
                    ASM.SET set = new ASM.SET(stptr, this.SP);
                    asmChunk.put(set);
                }
                for (IMC.Expr argm : call.args) {
                    IMC.CONST off = new IMC.CONST(offset);
                    if (firstArg) {
                        firstArg = false;
                    } else {
                        ASM.BINOP add = new ASM.BINOP(IMC.BINOP.Oper.ADD, stptr, stptr, off);
                        asmChunk.put(add);
                    }
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
                    //offset = offset + 8;
                }

                if (!callAsVar) {
                    IMC.TEMP pushjRegister = new IMC.TEMP();
                    ASM.PUSHJ pushj = new ASM.PUSHJ(pushjRegister, (IMC.NAME)call.addr);
                    //asmChunk.put(localsCount);
                    asmChunk.put(pushj);
                    // TODO: check If return value is needed...
                    // TJ. if 'dest' is live ... otherwise kill it?
                    ASM.SET copyRetVal = new ASM.SET(dest, pushjRegister);
                    asmChunk.put(copyRetVal);
                } else {
                    IMC.TEMP pushgoRegister = new IMC.TEMP();
                    ASM.PUSHGO pushgo = new ASM.PUSHGO(pushgoRegister, callVar);
                    asmChunk.put(pushgo);
                    ASM.SET copyRetVal = new ASM.SET(dest, pushgoRegister);
                    asmChunk.put(copyRetVal);
                }
        }

        // Pushj will need size of stack frame.
        // Ensure that return register is treated properly during register allocation.
        // MMIX special registers.
    }

    public void matchMovReg(IMC.Expr src, IMC.TEMP dest, ASM.AsmChunk asmChunk) {
        if (src instanceof IMC.NAME name) {
            ASM.LDA lda = new ASM.LDA(dest, name);
            asmChunk.put(lda);
        } else {
            ASM.SET set = new ASM.SET(dest, src);
            asmChunk.put(set);
        }
    }

    public void matchMovName(IMC.NAME name, IMC.TEMP dest, ASM.AsmChunk asmChunk) {
        // We want to put the address of this name into the register.
        
    }

    public void fixConsts() {
        // Fix the immediate values so that they always lie in [0, 255].
        for (ASM.AsmChunk codeChunk : AsmGen.asm) {
            Vector<ASM.Instr> newAsm = new Vector<ASM.Instr>();
            ASM.Operand result;
            for (ASM.Instr instr : codeChunk.asm) {
                if (instr instanceof ASM.TriOp triop) {
                    // Z can be immediate. Most common.
                    result = checkConst(triop.Z, newAsm, 256);
                    if (result instanceof ASM.Register r && triop.Z instanceof ASM.Immediate) {
                        triop.use.add(r); 
                        triop.Z = r;
                    }
                    newAsm.add(triop);
                } else if (instr instanceof ASM.BiOp biop) {
                    result = checkConst(biop.X, newAsm, 1<<16);
                    if (result instanceof ASM.Register r && biop.X instanceof ASM.Immediate) {
                        biop.def.add(r); 
                        biop.X = r;
                    }
                    result = checkConst(biop.Z, newAsm, 1<<16);
                    if (result instanceof ASM.Register r && biop.Z instanceof ASM.Immediate) {
                        biop.use.add(r); 
                        biop.Z = r;
                    }
                    newAsm.add(biop);
                } else if (instr instanceof ASM.MEMO memo) {
                    result = checkConst(memo.Z, newAsm, 256);
                    if (result instanceof ASM.Register r && memo.Z instanceof ASM.Immediate) {
                        memo.use.add(r); 
                        memo.Z = r;
                    }
                    newAsm.add(memo);
                } else newAsm.add(instr);
            }
            codeChunk.asm = newAsm;
        }
    }

    public ASM.Operand checkConst(ASM.Operand op, Vector<ASM.Instr> newAsm, int largeness) {
        // Returns the register in which the value will be found.
        // For byte: largeness is 256. For wyde: largeness is 1 << 16;
        boolean isNegative = false;
        boolean isLarge = false;
        int wyde = 1 << 16;
        IMC.TEMP result = new IMC.TEMP();
        if (op instanceof ASM.Immediate imm) {
            Long value = imm.value;
            if (value < 0) {
                isNegative = true;
                value = -value;
            }
            
            if (value >= largeness) isLarge = true;

            if (isLarge || isNegative) {
                if (value < wyde) {
                    ASM.SET set = new ASM.SET(result, new IMC.CONST(value));
                    newAsm.add(set);
                } else if (value < (1<<32)) {
                    ASM.SET setml = new ASM.SET(result, new IMC.CONST(value >> 16), 2);
                    ASM.INC incl = new ASM.INC(result, new IMC.CONST(value % wyde), 1);
                    newAsm.add(setml);
                    newAsm.add(incl);
                } else if (value < (1<<48)) {
                    ASM.SET setmh = new ASM.SET(result, new IMC.CONST(value >> 32), 3);
                    ASM.INC incml = new ASM.INC(result, new IMC.CONST((value >> 16) % wyde), 2);
                    ASM.INC incl = new ASM.INC(result, new IMC.CONST(value % wyde), 1);
                    newAsm.add(setmh);
                    newAsm.add(incml);
                    newAsm.add(incl);
                } else { //Value is up to 2^64 - 1, I suppose.
                    ASM.SET seth = new ASM.SET(result, new IMC.CONST(value >> 48), 4);
                    ASM.INC incmh = new ASM.INC(result, new IMC.CONST((value >> 32) % wyde), 3);
                    ASM.INC incml = new ASM.INC(result, new IMC.CONST((value >> 16) % wyde), 2);
                    ASM.INC incl = new ASM.INC(result, new IMC.CONST(value % wyde), 1);
                    newAsm.add(seth);
                    newAsm.add(incmh);
                    newAsm.add(incml);
                    newAsm.add(incl);
                }
            }

            if (isNegative) {
                ASM.NEG neg = new ASM.NEG(result, result);
                newAsm.add(neg);
            }
            if (!isNegative && !isLarge) return op;
            else return new ASM.Register(result);
        } else return op;
    }

    public IMC.Expr fixName(IMC.Expr expr, ASM.AsmChunk asmChunk) {
        if (expr instanceof IMC.NAME name) {
            IMC.TEMP nameHolder = new IMC.TEMP();
            ASM.LDA lda = new ASM.LDA(nameHolder, name);
            asmChunk.put(lda);
            return nameHolder;
        } else return expr;
    }
}
