package compiler.phase.livean;

import compiler.phase.Phase;

/**
 * Liveness analysis.
 */
public class LiveAn extends Phase {
    
    /**
     * Constructs a new phase for liveness analysis.
     */
    public LiveAn() {
        super("livean");
    }
}
