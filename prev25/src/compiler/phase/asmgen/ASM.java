package compiler.phase.asmgen;

import java.util.Vector;

import org.antlr.v4.parse.ANTLRParser.parserRule_return;

import compiler.common.report.Report;
import compiler.phase.imcgen.IMC;
import compiler.phase.memory.MEM;

/**
 * Assembly instructions for MMIX.
 */
public class ASM {
    
    /**
     * Corresponds to a line of assembly.
     */
    public static abstract class Instr {
        public IMC.LABEL label;
        
        public void setLabel(IMC.LABEL lab) {
            this.label = lab;
        }

        public String labelText() {
            return (this.label == null ? "    " : this.label.label.name);
        }
    }

    /**
     * MMIX's JMP to a label.
     */
    public static class JMP extends Instr {
        public IMC.NAME dest;

        public JMP(IMC.NAME name) {
            this.dest = name;
        }

        public String toString() {
            return this.labelText() + " JMP " + this.dest.label.name;
        }
    }
     
    public static class BNZ extends Instr {
        public IMC.TEMP cond;
        public IMC.NAME pos;
        public IMC.NAME neg;

        public BNZ(IMC.TEMP cond, IMC.NAME pos, IMC.NAME neg) {
            this.pos = pos;
            this.neg = neg;
            this.cond = cond;
        }

        public String toString() {
            return this.labelText() + " BNZ " + this.cond.temp.toString() + "," + this.pos.label.name;
        }
    }

    public abstract static class TriOperand extends Instr {
        public IMC.TEMP dest;
        public IMC.TEMP arg1;
        public IMC.TEMP reg2;
        public IMC.CONST imm2;
        public boolean isImmediate;
        public TriOperand(IMC.TEMP dest, IMC.TEMP arg1, IMC.Expr arg2) {
            this.dest = dest;
            this.arg1 = arg1;
            if (arg2 instanceof IMC.CONST c) {
                this.imm2 = c;
                isImmediate = true;
            } else {
                this.reg2 = (IMC.TEMP) arg2;
                isImmediate = false;
            }
        }
    }

    public static class BINOP extends TriOperand {
        // ADD, SUB, MUL, DIV, AND, OR
        public IMC.BINOP.Oper oper;

        
        public BINOP(IMC.BINOP.Oper oper, IMC.TEMP dest, IMC.TEMP arg1, IMC.Expr arg2) {
            super(dest, arg1, arg2);
            this.oper = oper;
        }

        private String operString() {
            switch(this.oper) {
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

        public String toString() {
            return this.labelText() + " " + this.operString() + " " + this.dest.temp.toString() + "," + this.arg1.temp.toString() + "," + (isImmediate ? this.imm2.value : this.reg2.temp.toString());
        }

    }

    public static class NOR extends TriOperand {
        public NOR(IMC.TEMP dest, IMC.TEMP arg1) {
            super(dest, arg1, new IMC.CONST(0L));
        }

        public String toString() {
            return this.labelText() + " NOR " + dest.temp.toString() + "," + arg1.temp.toString() + ",0";
        }
    }

    public static class CMP extends TriOperand {

        public CMP(IMC.TEMP dest, IMC.TEMP arg1, IMC.Expr arg2) {
            super(dest, arg1, arg2);
        }

        public String toString() {
            return this.labelText() + " CMP " + this.dest.temp.toString() + "," + this.arg1.temp.toString() + "," + (isImmediate ? this.imm2 : this.reg2);
        }
    }

    public static class CSET extends Instr {
        // Only support these partly
        public enum Oper {
            ZSZ, ZSNZ, ZSN, ZSNN, ZSP, ZSNP,
        }

        public Oper op;
        public IMC.TEMP dest;
        public IMC.TEMP cond;

        public CSET(Oper op, IMC.TEMP dest, IMC.TEMP cond) {
            this.op = op;
            this.dest = dest;
            this.cond = cond;
        }

        private String operString() {
            switch(this.op) {
                case ZSZ:
                    return "ZSZ";
                case ZSNZ:
                    return "ZSNZ";
                case ZSN:
                    return "ZSN";
                case ZSNN:
                    return "ZSNN";
                case ZSP:
                    return "ZSP";
                case ZSNP:
                    return "ZSNP";
                default:
                    throw new Report.InternalError();
            }
        }

        public String toString() {
            return this.labelText() + " " + this.operString() + " " + this.dest.temp.toString() + "," + this.cond.temp.toString() + ",1";
        }
    }

    // TODO: can be optimized.
    public static class LOAD extends TriOperand {
        // For now, we are using without offset.
        public Long size;
        public LOAD(IMC.TEMP dest, IMC.TEMP arg1, Long size) {
            super(dest, arg1, new IMC.CONST(0L));
            this.size = size;
        }

        public String toString() {
            return this.labelText() + " LD" + (this.size == 1 ? "B " : "O ") + this.dest.temp.toString() + "," + this.arg1.temp.toString() + ",0"; 
        }
    }

    public static class STORE extends TriOperand {
        public Long size;
        public STORE(IMC.TEMP dest, IMC.TEMP arg1, Long size) {
            super(dest, arg1, new IMC.CONST(0L));
            this.size = size;
        }

        public String toString() {
            return this.labelText() + " ST" + (this.size == 1 ? "B " : "O ") + this.dest.temp.toString() + "," + this.arg1.temp.toString() + ",0"; 
        }
    }

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
        private Vector<Instr> asm;
        public IMC.LABEL currLabel;
        public String name;

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
    }


}
