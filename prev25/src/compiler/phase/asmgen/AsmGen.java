package compiler.phase.asmgen;

import java.util.Vector;

import compiler.phase.Phase;

/**
 * Generation of assembly code.
 */
public class AsmGen extends Phase {

    /**
     * Constructs a new phase for the generation of assembly code.
    */
    public final static Vector<ASM.AsmChunk> asm = new Vector<ASM.AsmChunk>();
	
    public AsmGen() {
		super("asmgen");
	}
}
