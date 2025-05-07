package compiler.phase.asmgen;

import compiler.phase.Phase;
import compiler.phase.imcgen.IMC;
import compiler.phase.seman.SemAn;

/**
 * Generation of assembly code.
 */
public class AsmGen extends Phase {

    /**
     * Constructs a new phase for the generation of assembly code.
    */
	public AsmGen() {
		super("asmgen");
	}
}
