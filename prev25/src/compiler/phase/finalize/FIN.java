package compiler.phase.finalize;

import java.util.Vector;

import compiler.phase.asmgen.ASM;
import compiler.phase.imcgen.IMC;
import compiler.phase.memory.MEM;

public class FIN {
    
    public static Vector<String> getInit() {
        Vector<String> rawAsm = new Vector<String>();

        rawAsm.add(String.format("%-8s %-8s %s,%s,%s", "Main", "SWYM", "42", "42", "42"));
        rawAsm.add(String.format("%-8s %-8s %s", "SP", "GREG", "0"));
        rawAsm.add(String.format("%-8s %-8s %s", "FP", "GREG", "0"));
        rawAsm.add(String.format("%-8s %-8s %s,%s", "", "LDA", "SP", "Stack_Segment"));
        rawAsm.add(String.format("%-8s %-8s %s,%s", "", "SET", "FP", "SP"));
        rawAsm.add(String.format("%-8s %-8s %s,%s", "", "PUSHJ", "$0", "_main"));
        rawAsm.add(String.format("%-8s %-8s %s,%s", "", "SET", "$255", "$0"));
        rawAsm.add(String.format("%-8s %-8s %s,%s,%s", "", "TRAP", "0", "Halt", "0"));
        return rawAsm;
    }

    public static Vector<String> getEntry(ASM.AsmChunk chunk) {
        Vector<String> rawAsm = new Vector<String>();
        // Modify this adding depending on the frame you have.
        rawAsm.add(String.format("%% Local vars need %d size and args need %d\n", chunk.frame.locsSize, chunk.frame.argsSize));
        rawAsm.add(String.format("%-8s %-8s %s,%s,%d", chunk.name, "STO", "FP", "SP", chunk.frame.locsSize));
        rawAsm.add(String.format("%-8s %-8s %s,%s", "", "SET", "FP", "SP"));
        rawAsm.add(String.format("%-8s %-8s %s,%s,%d", "", "ADD", "SP", "SP", chunk.frame.locsSize + 8));
        //Here take care of any more things, needed to put on.
        rawAsm.add(String.format("%-8s %-8s %s", "", "JMP", chunk.entryLabel.name));    
        return rawAsm;
    }
    
    public static Vector<String> getExit(ASM.AsmChunk chunk) {
        Vector<String> rawAsm = new Vector<String>();
        // TODO: put the return value in the correct register

        rawAsm.add(String.format("%-8s %-8s %s,%s", chunk.exitLabel.name, "SET", "SP", "FP"));
        rawAsm.add(String.format("%-8s %-8s %s,%s,%d", "", "LDO", "FP", "SP", chunk.frame.locsSize));
        rawAsm.add(String.format("%-8s %-8s %s,%s", "", "POP", "1", "0"));
        return rawAsm;
    }
}
