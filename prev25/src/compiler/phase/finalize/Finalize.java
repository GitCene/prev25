package compiler.phase.finalize;

import java.util.Vector;

import compiler.phase.Phase;

public class Finalize extends Phase{

    public static Vector<String> codeText = new Vector<String>();

    public Finalize() {
        super("finalize");
    }
}
