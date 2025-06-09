package compiler.phase.asmgen;

import java.util.HashMap;
import java.util.Vector;

import compiler.phase.Phase;
import compiler.phase.memory.MEM;

/**
 * Generation of assembly code.
 */
public class AsmGen extends Phase {
    
    public final static Vector<ASM.AsmChunk> asm = new Vector<ASM.AsmChunk>();
    public final static HashMap<MEM.Temp, Integer> constraints = new HashMap<MEM.Temp, Integer>();
    
    /**
     * Constructs a new phase for the generation of assembly code.
     */
    public AsmGen() {
        super("asmgen");
    }
}
