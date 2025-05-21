package compiler.phase.livean;

import java.util.HashMap;

import compiler.phase.Phase;
import compiler.phase.asmgen.ASM;

/**
 * Liveness analysis.
 */
public class LiveAn extends Phase {
    
    
    public static final HashMap<ASM.AsmChunk, LIV.AsmGraph> graphMap = new HashMap<ASM.AsmChunk, LIV.AsmGraph>();
    /**
     * Constructs a new phase for liveness analysis.
     */
    public LiveAn() {
        super("livean");
    }
}
