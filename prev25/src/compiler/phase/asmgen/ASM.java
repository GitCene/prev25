package compiler.phase.asmgen;

import java.util.HashMap;
import java.util.Vector;

import compiler.common.report.Report;
import compiler.phase.imcgen.IMC;
import compiler.phase.memory.MEM;

/**
 * Assembly instructions for MMIX.
 */
// TODO: change the IMC.TEMPS to MEM.Temps.
public class ASM {
    
    /**
     * Corresponds to a line of assembly.
     */
    public static abstract class Instr {
        public IMC.LABEL label;
        public static HashMap<IMC.LABEL, ASM.Instr> labelMap = new HashMap<IMC.LABEL, ASM.Instr>();
        public Vector<IMC.TEMP> use = new Vector<IMC.TEMP>();
        public Vector<IMC.TEMP> def = new Vector<IMC.TEMP>();
        //public Vector<ASM.Instr> pred = new Vector<ASM.Instr>();
        public Vector<ASM.Instr> succ = new Vector<ASM.Instr>();

        public void setLabel(IMC.LABEL lab) {
            this.label = lab;
            Instr.labelMap.put(lab, this);
        }

        public String labelText() {
            return (this.label == null ? "    " : this.label.label.name);
        }

        public void addSucc(ASM.Instr instr) {
            this.succ.add(instr);
        }
        
        // Return string representation of instruction according to a physical register mapping.
        public String mapped(HashMap<IMC.TEMP, String> mapping) {
            return this.toString();
        }

    }

    /**
     * Copy a G.P. register's value to another G.P. register.
     */
    public static class SET extends Instr {
        public IMC.TEMP reg1;
        public IMC.TEMP reg2;
        public IMC.CONST imm2;
        public boolean isImmediate;
        
        public SET(IMC.TEMP reg1, IMC.Expr arg2) {
            this.reg1 = reg1;
            this.def.add(reg1);
            if (arg2 instanceof IMC.CONST c) {
                this.imm2 = c;
                this.isImmediate = true;
            } else {
                this.reg2 = (IMC.TEMP) arg2;
                this.isImmediate = false;
                this.use.add(reg2);
            }
        }

        @Override
        public String toString() {
            return this.labelText() + " SET " + this.reg1.temp + "," + (this.isImmediate ? this.imm2.value : this.reg2.temp);
        }

        @Override
        public String mapped(HashMap<IMC.TEMP, String> mapping) {
            return this.labelText() + " SET " + mapping.get(this.reg1) + "," + (this.isImmediate ? this.imm2.value : mapping.get(this.reg2));
        }
    }

    /**
     * Instructions that can redirect execution.
     */
    public static abstract class Jump extends Instr {
        public Vector<IMC.NAME> jumpsTo;

        public Jump() {
            this.jumpsTo = new Vector<IMC.NAME>();
        }
    }

    /**
     * MMIX's JMP to a label.
     * JMP XYZ : jump to label XYZ.
     */
    public static class JMP extends Jump {
        public IMC.NAME dest;

        public JMP(IMC.NAME name) {
            this.dest = name;
            this.jumpsTo.add(name);
        }

        public String toString() {
            return this.labelText() + " JMP " + this.dest.label.name;
        }
    }
     
    /**
     * MMIX's Branches: 
     * BZ $X,YZ	    PBZ $X,YZ	(Probable) Branch if zero
     * BNZ $X,YZ	PBNZ $X,YZ	(Probable) Branch if nonzero
     * BN $X,YZ	    PBN $X,YZ	(Probable) Branch if negative
     * BNN $X,YZ	PBNN $X,YZ	(Probable) Branch if nonnegative
     * BP $X,YZ	    PBP $X,YZ	(Probable) Branch if positive
     * BNP $X,YZ	PBNP $X,YZ	(Probable) Branch if nonpositive
     * BOD $X,YZ	PBOD $X,YZ	(Probable) Branch if odd
     * BEV $X,YZ	PBEV $X,YZ	(Probable) Branch if even
     * If cond($X) jump to YZ.
     */
    public static class BRANCH extends Jump {
        public enum Oper {
            BZ, BNZ, BN, BNN, BP, BNP, BOD, BEV
        }
        public IMC.TEMP cond;
        public IMC.NAME dest;
        public Oper op;
        public boolean probable = false;

        public BRANCH(Oper op, IMC.TEMP cond, IMC.NAME pos, IMC.NAME neg) {
            this.op = op;
            this.cond = cond;
            this.dest = pos;
            this.jumpsTo.add(pos);
            this.jumpsTo.add(neg);
            this.use.add(cond);
        }

        public String mnem() {
            String p = this.probable ? "P" : "";
            switch(this.op){
                case BZ:
                    return p + "BZ";
                case BNZ:
                    return p + "BNZ";
                case BN:
                    return p + "BN";
                case BNN:
                    return p + "BNN";
                case BP:
                    return p + "BP";
                case BNP:
                    return p + "BNP";
                case BOD:
                    return p + "BOD";
                case BEV:
                    return p + "BEV";
                default:
                    throw new Report.InternalError();
            }
        }

        public String toString() {
            return this.labelText() + " " + this.mnem() + " " + this.cond.temp + "," + this.dest.label.name;
        }

        @Override
        public String mapped(HashMap<IMC.TEMP, String> mapping) {
            return this.labelText() + " " + this.mnem() + " " + mapping.get(this.cond) + "," + this.dest.label.name;
        }
    }

    /**
     * An instruction which takes three operands.
     * The last one can be immediate or not.
     * It is assumed that the instruction writes to reg1,
     * and reads from reg2 (and possibly reg3).
     * OP $1,$2,$3 or OP $1,$2,3
     */
    public abstract static class TriOp extends Instr {
        public IMC.TEMP reg1;
        public IMC.TEMP reg2;
        public IMC.TEMP reg3;
        public IMC.CONST imm3;
        public boolean isImmediate;

        public TriOp(IMC.TEMP reg1, IMC.TEMP reg2, IMC.Expr arg3) {
            this.reg1 = reg1;
            this.def.add(reg1);
            this.reg2 = reg2;
            this.use.add(reg2);
            if (arg3 instanceof IMC.CONST c) {
                this.imm3 = c;
                isImmediate = true;
            } else {
                this.reg3 = (IMC.TEMP) arg3;
                isImmediate = false;
                this.use.add(reg3);
            }
        }

        public abstract String mnem();

        public String toString() {
            return this.labelText() + " " + this.mnem() + " " + this.reg1.temp +  "," + this.reg2.temp + "," + (isImmediate ? this.imm3.value : this.reg3.temp);
        }

        @Override
        public String mapped(HashMap<IMC.TEMP, String> mapping) {
            return this.labelText() + " " + this.mnem() + " " + mapping.get(this.reg1) +  "," + mapping.get(this.reg2) + "," + (isImmediate ? this.imm3.value : mapping.get(this.reg3));
        }
    }

    /**
     * Correspond to IMC binary operations.
     */
    public static class BINOP extends TriOp {
        // ADD, SUB, MUL, DIV, AND, OR
        public enum Oper {
            ADD, SUB, MUL, DIV, AND, OR
        }
        
        public Oper op;
        //public IMC.BINOP.Oper oper;
        
        private Oper operMap(IMC.BINOP.Oper oper) {
            switch(oper) {
                case IMC.BINOP.Oper.ADD:
                    return Oper.ADD;
                case IMC.BINOP.Oper.SUB:
                    return Oper.SUB;
                case IMC.BINOP.Oper.MUL:
                    return Oper.MUL;
                case IMC.BINOP.Oper.DIV:
                    return Oper.DIV;
                case IMC.BINOP.Oper.AND:
                    return Oper.AND;
                case IMC.BINOP.Oper.OR:
                    return Oper.OR;
                default:
                    throw new Report.InternalError();
            }
        }

        public BINOP(IMC.BINOP.Oper oper, IMC.TEMP dest, IMC.TEMP arg1, IMC.Expr arg2) {
            super(dest, arg1, arg2);
            this.op = this.operMap(oper);
        }

        @Override
        public String mnem() {
            switch(this.op) {
                case ADD:
                    return "ADD";
                case SUB:
                    return "SUB";
                case MUL:
                    return "MUL";
                case DIV:
                    return "DIV";
                case AND:
                    return "AND";
                case OR:
                    return "OR";
                default:
                    throw new Report.InternalError();
            }
        }

    }

    /**
     * TODO: restructure these instruction more properly.
     */
    public static class BITOP extends TriOp {
        public enum Oper {
            NOR,
        } // TODO: fill

        public Oper op;

        public BITOP(Oper op, IMC.TEMP dest, IMC.TEMP arg1, IMC.Expr arg2) {
            super(dest, arg1, arg2);
            this.op = op;
        }

        public String mnem() {
            switch(this.op) {
                case NOR:
                    return "NOR";
                default:
                    throw new Report.InternalError();
            }
        }
    }

    /**
     * MMIX's compare instruction.
     */
    public static class CMP extends TriOp {

        public CMP(IMC.TEMP dest, IMC.TEMP arg1, IMC.Expr arg2) {
            super(dest, arg1, arg2);
        }

        @Override
        public String mnem() {
            return "CMP";
        }
    }

    /**
     * MMIX's conditional assignment.
     */
    public static class CSET extends TriOp {
        public enum Oper {
            SZ, SNZ, SN, SNN, SP, SNP, SOD, SEV
        }

        public Oper op;
        public boolean zeroSet;

        public CSET(Oper op, IMC.TEMP dest, IMC.TEMP cond, IMC.Expr val, boolean zeroSet) {
            super(dest, cond, val);
            this.op = op;
            this.zeroSet = zeroSet;
        }

        @Override
        public String mnem() {
            String c = this.zeroSet ? "Z" : "C";
            switch(this.op) {
                case SZ:
                    return c + "SZ";
                case SNZ:
                    return c + "SNZ";
                case SN:
                    return c + "SN";
                case SNN:
                    return c + "SNN";
                case SP:
                    return c + "SP";
                case SNP:
                    return c + "SNP";
                default:
                    throw new Report.InternalError();
            }
        }
    }

    /**
     * MMIX's load and store instructions.
     * Support byte and octa.
     */
    public static class MEM extends Instr {
        public boolean isStore;
        public Long size;

        public IMC.TEMP reg1;
        public IMC.TEMP reg2;
        public IMC.TEMP reg3;
        public IMC.CONST imm3;
        public boolean isImmediate;

        public MEM(boolean isStore, Long size, IMC.TEMP arg1, IMC.TEMP arg2, IMC.Expr arg3) {
            this.reg1 = arg1;
            this.reg2 = arg2;
            if (!isStore)
                this.def.add(reg1);
            else
                this.use.add(reg1);
            this.use.add(reg2);
            if (arg3 instanceof IMC.CONST c) {
                this.imm3 = c;
                isImmediate = true;
            } else {
                this.reg3 = (IMC.TEMP) reg3;
                isImmediate = false;
                this.use.add(reg3);
            }
            this.isStore = isStore;
            assert size == 8L || size == 1L;
            this.size = size;
        }

        public String mnem() {
            String o = this.isStore ? "ST" : "LD";
            String s = this.size == 8L ? "O" : "B";
            return o + s;
        }

        public String toString() {
            return this.labelText() + " " + this.mnem() + " " + this.reg1.temp +  "," + this.reg2.temp + "," + (isImmediate ? this.imm3.value : this.reg3.temp);
        }
        
        @Override
        public String mapped(HashMap<IMC.TEMP, String> mapping) {
            return this.labelText() + " " + this.mnem() + " " + mapping.get(this.reg1) +  "," + mapping.get(this.reg2) + "," + (isImmediate ? this.imm3.value : mapping.get(this.reg3));
        }
    }

    /**
     * MMIX's way to call subroutines.
     * TODO.
     */
    public static class PUSHJ extends Instr {
        IMC.TEMP framesize;
        IMC.NAME callee;
        
        public PUSHJ(IMC.TEMP framesize, IMC.NAME callee) {
            this.framesize = framesize;
            this.callee = callee;
        }

        public String toString() {
            return this.labelText() + " PUSHJ " + framesize.temp.toString() + "," + callee.label.name;
        }
    }
    /*
     * An assembly chunk for a code chunk.
     */
    public static class AsmChunk {
        public Vector<Instr> asm;
        public IMC.LABEL currLabel;
        public String name;

        // If needed:
        //public HashMap<ASM.Jump, IMC.NAME> jumpsDict;
        //public HashMap<ASM.Instr, IMC.TEMP> readsDict;
        //public HashMap<ASM.Instr, IMC.TEMP> writesDict;

        // For register allocation phase - subject to change
        public HashMap<IMC.TEMP, String> physicalRegisters;


        public AsmChunk(String name) {
            this.asm = new Vector<Instr>();
            this.name = name;
        }

        public void setLabel(IMC.LABEL lab) {
            this.currLabel = lab;
        }

        public void put(Instr instr) {
            if (this.currLabel != null) {
                instr.setLabel(this.currLabel);
                this.currLabel = null;
            }
            this.asm.add(instr);
        }

        public void emit() {
            System.out.println("##### ASM : " + this.name + " #####" );
            for (Instr instr : this.asm)
                System.out.println(instr);
        }
        
        public void emitPhysical() {
            System.out.println("##### ASM : " + this.name + " #####" );
            for (Instr instr : this.asm)
                System.out.println(instr.mapped(this.physicalRegisters));
            // For now, will not work if certain registers stay unmapped.
        }
    }


}
