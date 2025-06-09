package compiler.phase.finalize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import compiler.common.report.Report;
import compiler.phase.asmgen.ASM.*;
import compiler.phase.imclin.LIN.*;
import compiler.phase.finalize.FIN.*;
import compiler.phase.finalize.Finalize.*;

public class CodeFinalizer {
    
    public Vector<AsmChunk> asm;
    public Vector<DataChunk> data;

    // TODO: stack segment is not okay. Define own Stack.
    private String[] header = {
        "%%%%% MMIX assembly output",
        "         LOC Stack_Segment",
        "         GREG @",
        "         LOC Data_Segment",
        "         GREG @"
    };

    public CodeFinalizer(Vector<AsmChunk> asm, Vector<DataChunk> data) {
        this.asm = asm;
        this.data = data;
    }

    public void finalizeCode() {
        for (String line : this.header)
            Finalize.codeText.add(line);
		for (DataChunk dataChunk : this.data) {
            String data = String.format("%-8s BYTE \"%s\",0\n", dataChunk.label.name, dataChunk.init);
            Finalize.codeText.add(data);
        }
        // Get INIT chunk
        Finalize.codeText.add("         LOC #100");
        Finalize.codeText.add("% init subroutine");

        for (String line : FIN.getInit())
            Finalize.codeText.add(line);

        // Place for stdlib functions
        Finalize.codeText.add("% STDLIB");
        File stdlib = new File("../src/compiler/common/stdlib");
        for (File src : stdlib.listFiles()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(src))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Finalize.codeText.add(line);
                }
            } catch (IOException e) {
                throw new Report.Error("Error reading source files from stdlib.");
            }
            
        }

		for (AsmChunk asmChunk : this.asm) {
            String comment = String.format("%% name: %s, entry: %s, exit: %s, FP: %s, RV: %s\n", asmChunk.name, asmChunk.entryLabel.name, asmChunk.exitLabel.name, asmChunk.frame.FP, asmChunk.frame.RV);
            Finalize.codeText.add(comment);
            for (String line : FIN.getEntry(asmChunk))
                Finalize.codeText.add(line);
			
            Vector<String> lines = asmChunk.getPhysical();
            for (String line : lines)
                Finalize.codeText.add(line);


            for (String line : FIN.getExit(asmChunk))
                Finalize.codeText.add(line);
		}
	}
    
}
