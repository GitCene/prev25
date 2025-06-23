package compiler.phase.asmgen;

import java.util.HashMap;
import java.util.Vector;

import compiler.common.report.Report;
import compiler.phase.abstr.AST.BinExpr.Oper;
import compiler.phase.imcgen.IMC;
import compiler.phase.memory.MEM;
import compiler.phase.memory.MEM.Temp;

/**
 * Assembly instructions for MMIX.
 */
public class ASM {

    /**
     * An operand passed to an assembly instruction.
     */
    public static abstract class Operand {
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            return -1;
        }
    }

    /**
     * The operand is a register.
     */
    public static class Register extends Operand {
        public final MEM.Temp virtual;
        public Integer physical;
        public boolean colored = false;
        
        public Register(IMC.TEMP temp) {
            this.virtual = temp.temp;
        }

        public Register(MEM.Temp temp) {
            this.virtual = temp;
        }

        @Override
        public String toString() {
            if (this.colored)
                switch (this.physical) {
                    case null:
                        return "$0";
                        //return "NUL(" + this.virtual + ")";
                    // TODO: poenoti mapping global registrov.
                    case 253:
                        return "FP";
                    case 254:
                        return "SP";
                    default:
                        return String.format("$%d", this.physical);
                }
            else
                return this.virtual.toString();
        }

        @Override
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            this.physical = coloring.get(this.virtual);
            // TODO: Let's assume this for now;
            //if (this.physical == null) this.physical = 0;
            this.colored = true;
            return this.physical == null ? 0 : this.physical;
        }
    }

    /**
     * The operand is an immediate value.
     */
    public static class Immediate extends Operand {
        //TODO: Think about what kind of values will be here.
        public Long value;

        public Immediate(Long value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Long.toString(this.value);
        }
    }
    /**
     * An assembly instruction.
     */
    public static abstract class Instr {
        public static HashMap<MEM.Label, ASM.Instr> labelMap = new HashMap<MEM.Label, ASM.Instr>();
        
        public MEM.Label label;
        public Vector<Register> use = new Vector<Register>();
        public Vector<Register> def = new Vector<Register>();
        //public Vector<ASM.Instr> pred = new Vector<ASM.Instr>();
        public Vector<ASM.Instr> succ = new Vector<ASM.Instr>();

        public void setLabel(MEM.Label lab) {
            this.label = lab;
            Instr.labelMap.put(lab, this);
        }

        public String labelText() {
            return this.label == null ? "" : this.label.name;
        }

        public void addSucc(ASM.Instr instr) {
            this.succ.add(instr);
        }
        
        // Return string representation of instruction according to a physical register mapping.
        // public String mapped(HashMap<MEM.Temp, Integer> mapping) {
        //    return this.toString();
        //}
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            return -1;
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
        public Register X;
        public Register Y;
        public Operand Z;

        public TriOp(IMC.TEMP a1, IMC.TEMP a2, IMC.Expr a3) {
            this.X = new Register(a1);
            this.def.add(this.X);
            this.Y = new Register(a2);
            this.use.add(this.Y);
            if (a3 instanceof IMC.CONST c) {
                this.Z = new Immediate(c.value);
            } else if (a3 instanceof IMC.TEMP t) {
                Register rz = new Register(t);
                this.Z = rz;
                this.use.add(rz);
            } else throw new Report.InternalError();
        }

        public abstract String mnem();

        public String toString() {
            return String.format("%-8s %-8s %s,%s,%s", this.labelText(), this.mnem(), this.X, this.Y, this.Z);
        }

        @Override
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            int x = this.X.color(coloring);
            int y = this.Y.color(coloring);
            int z = this.Z.color(coloring);
            return Integer.max(x, Integer.max(y, z));
        }
    }

    /**
     * An instruction which takes two operands.
     */
    public abstract static class BiOp extends Instr {
        public Operand X;
        public Operand Z;

        public BiOp(IMC.Expr a1, IMC.Expr a2) {
            if (a1 instanceof IMC.CONST c1) {
                this.X = new Immediate(c1.value);
            } else if (a1 instanceof IMC.TEMP t1) {
                Register rx = new Register(t1);
                this.X = rx;
                this.def.add(rx);
            }
            if (a2 instanceof IMC.CONST c2) {
                this.Z = new Immediate(c2.value);
            } else if (a2 instanceof IMC.TEMP t2) {
                Register rz = new Register(t2);
                this.Z = rz;
                this.use.add(rz);
            }
        }

        public abstract String mnem();

        public String toString() {
            return String.format("%-8s %-8s %s,%s", this.labelText(), this.mnem(), this.X, this.Z);
        }

        @Override
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            int x = this.X.color(coloring);
            int z = this.Z.color(coloring);
            return Integer.max(x, z);
        }
    }
    /**
     * Copy a G.P. register's value to another G.P. register, or set it to a constant.
     */
    public static class SET extends BiOp {
        public int kind;   
        // 0: SET
        // 1: SETL
        // 2: SETML
        // 3: SETMH
        // 4: SETH

        public SET(IMC.TEMP a1, IMC.Expr a2) {
            super(a1, a2);
            this.kind = 0;
        }

        public SET(IMC.TEMP a1, IMC.Expr a2, int kind) {
            super(a1, a2);
            this.kind = kind;
        }

        @Override
        public String mnem() {
            switch (this.kind) {
                case 0:
                    return "SET";
                case 1:
                    return "SETL";
                case 2:
                    return "SETML";
                case 3:
                    return "SETMH";
                case 4:
                    return "SETH";
                default:
                    return "SET";
            }
        }
    }

    /*
     * Increase by high wyde / mh wyde / ml wyde / low wyde.
     * To put large constants into registers.
     */
    public static class INC extends BiOp {
        public int kind;
        public INC(IMC.TEMP a1, IMC.CONST a2, int kind) {
            super(a1, a2);
            this.kind = kind;
        }

        @Override
        public String mnem() {
            switch (this.kind) {
                case 1:
                    return "INCL";
                case 2:
                    return "INCML";
                case 3:
                    return "INCMH";
                case 4:
                    return "INCH";
                default:
                    return "INCL";
            }
        }
        
    }

    /**
     * Copy a special register's value to a G.P. register.
     */
    public static class GET extends BiOp {
        public GET(IMC.TEMP a1, IMC.Expr a2) {
            super(a1, a2);
        }

        @Override
        public String mnem() {
            return "GET";
        }
    }

    /*
     * Negate a register's integer value.
     */
    public static class NEG extends BiOp {
        public NEG (IMC.TEMP a1, IMC.TEMP a2) {
            super(a1, a2);
        }

        @Override
        public String mnem() {
            return "NEG";
        }
    }

    /**
     * Instructions that can redirect execution.
     */
    public static abstract class Jump extends Instr {
        public Vector<MEM.Label> jumpsTo;

        public Jump() {
            this.jumpsTo = new Vector<MEM.Label>();
        }
    }

    /**
     * MMIX's JMP to a label.
     * JMP XYZ : jump to label XYZ.
     */
    public static class JMP extends Jump {
        public MEM.Label dest;

        public JMP(IMC.NAME name) {
            this.dest = name.label;
            this.jumpsTo.add(name.label);
        }

        public String toString() {
            return String.format("%-8s %-8s %s", this.labelText(), "JMP", this.dest.name);
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
        public Register cond;
        public MEM.Label dest;
        public Oper op;
        public boolean probable = false;

        public BRANCH(Oper op, IMC.TEMP cond, IMC.NAME pos, IMC.NAME neg) {
            this.op = op;
            this.cond = new Register(cond);
            this.dest = pos.label;
            this.jumpsTo.add(pos.label);
            this.jumpsTo.add(neg.label);
            this.use.add(this.cond);
        }

        public String mnem() {
            String p = this.probable ? "P" : "";
            return p + this.op;
            /*
             * 
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
                */
        }

        public String toString() {
            return String.format("%-8s %-8s %s,%s", this.labelText(), this.mnem(), this.cond, this.dest.name);
        }

        @Override
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            return this.cond.color(coloring);
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
            return this.op.toString();
            /*
             * 
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
            */
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
            return c + this.op;
            /*
             * 
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
                */
        }
    }

    /**
     * MMIX's load and store instructions.
     * Support byte and octa.
     */
    public static class MEMO extends Instr {
        public boolean isStore;
        public Long size;

        public Register X;
        public Register Y;
        public Operand Z;

        public MEMO(boolean isStore, Long size, IMC.TEMP arg1, IMC.TEMP arg2, IMC.Expr arg3) {
            this.X = new Register(arg1);
            this.Y = new Register(arg2);
            if (!isStore)
                this.def.add(this.X);
            else
                this.use.add(this.X);
            this.use.add(this.Y);
            if (arg3 instanceof IMC.CONST c) {
                this.Z = new Immediate(c.value);
            } else if (arg3 instanceof IMC.TEMP t) {
                Register rz = new Register(t);
                this.use.add(rz);
                this.Z = rz;
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
            return String.format("%-8s %-8s %s,%s,%s", this.labelText(), this.mnem(), this.X, this.Y, this.Z);
        }

        @Override
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            int x = this.X.color(coloring);
            int y = this.Y.color(coloring);
            int z = this.Z.color(coloring);
            return Integer.max(x, Integer.max(y, z));
        }
    }

    /**
     * MMIX's Load Address.
     */
    public static class LDA extends Instr {
        public Register dest;
        public MEM.Label label;

        public LDA(IMC.TEMP dest, IMC.NAME name) {
            this.dest = new Register(dest);
            this.label = name.label;
            
            this.def.add(this.dest);
        }

        public String toString() {
            return String.format("%-8s %-8s %s,%s", this.labelText(), "LDA", this.dest, this.label.name);
        }

        @Override
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            return this.dest.color(coloring);
        }
    }

    /**
     * MMIX's way to call subroutines.
     * TODO.
     */
    public static class PUSHJ extends Instr {
        public Register callreg;
        public MEM.Label callee;
        
        public PUSHJ(IMC.TEMP callreg, IMC.NAME callee) {
            this.callreg = new Register(callreg);
            this.callee = callee.label;
            this.def.add(this.callreg);
        }

        public String toString() {
            return String.format("%-8s %-8s %s,%s", this.labelText(), "PUSHJ", this.callreg, this.callee.name);
        }

        @Override
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            return this.callreg.color(coloring);
        }
    }

    /*
     * MMIX's wilder way to call subroutines.
     */
    public static class PUSHGO extends Instr {
        public Register callreg;
        public Register callee;
        
        public PUSHGO(IMC.TEMP callreg, IMC.TEMP callee) {
            this.callreg = new Register(callreg);
            this.callee = new Register(callee);
            this.def.add(this.callreg);
            this.use.add(this.callee);
        }

        public String toString() {
            return String.format("%-8s %-8s %s,%s,%d", this.labelText(), "PUSHGO", this.callreg, this.callee, 0);
        }

        @Override
        public int color(HashMap<MEM.Temp, Integer> coloring) {
            int x = this.callreg.color(coloring);
            int y = this.callee.color(coloring);
            return Integer.max(x, y);
        }
    }

    /**
     * MMIX's system call.
     */
    public static class TRAP extends Instr {
        public String Y;
        public String Z;

        public TRAP(String Y, String Z) {
            this.Y = Y;
            this.Z = Z;
        }

        public String toString() {
            return String.format("%-8s %-8s 0,%s,%s", this.labelText(), "TRAP", this.Y, this.Z);
        }
    }
    /*
     * An assembly chunk for a code chunk.
     */
    public static class AsmChunk {
        public Vector<Instr> asm;
        public IMC.LABEL currLabel;
        public String name;
        public MEM.Frame frame;
        public MEM.Label entryLabel;
        public MEM.Label exitLabel;

        // If needed:
        //public HashMap<ASM.Jump, IMC.NAME> jumpsDict;
        //public HashMap<ASM.Instr, IMC.TEMP> readsDict;
        //public HashMap<ASM.Instr, IMC.TEMP> writesDict;

        // For register allocation phase - subject to change
        public HashMap<MEM.Temp, Integer> coloring;


        public AsmChunk(String name, MEM.Frame frame, MEM.Label entryLabel, MEM.Label exitLabel) {
            this.asm = new Vector<Instr>();
            this.frame = frame;
            this.name = name;
            this.entryLabel = entryLabel;
            this.exitLabel = exitLabel;
        }

        public void setLabel(IMC.LABEL lab) {
            this.currLabel = lab;
        }

        public void put(Instr instr) {
            if (this.currLabel != null) {
                instr.setLabel(this.currLabel.label);
                this.currLabel = null;
            }
            this.asm.add(instr);
        }

        public void emit() {
            System.out.println("##### ASM : " + this.name + " #####" );
            for (Instr instr : this.asm)
                System.out.println(instr);
        }
        
        public void emitPhysicalVerbose() {
            System.out.println("% " + this.name);
            for (Instr instr : this.asm) {
                System.out.printf("%-30s    ---->    ", instr.toString());                
                //System.out.println(instr.mapped(this.coloring));
                System.out.println(instr);
            }
        }

        public void emitPhysical() {
            //System.out.printf("%% name: %s, entry: %s, exit: %s, FP: %s, RV: %s\n", this.name, this.entryLabel.name, this.exitLabel.name, this.frame.FP, this.frame.RV);
            for (Instr instr : this.asm) {                
                //System.out.println(instr.mapped(this.coloring));
                System.out.println(instr);
            }
        }

        public Vector<String> getPhysical() {
            Vector<String> vec = new Vector<String>();
            for (Instr instr : this.asm) {
                vec.add(instr.toString());
            }
            return vec;
        }
    }


}
