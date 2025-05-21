package compiler.phase.livean;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import compiler.phase.asmgen.ASM;
import compiler.phase.memory.MEM;
import compiler.phase.imcgen.IMC;

public class LivenessAnalyzer {
    private Vector<ASM.AsmChunk> asm = new Vector<ASM.AsmChunk>();
    private HashMap<ASM.Instr, HashSet<MEM.Temp>> in = new HashMap<ASM.Instr, HashSet<MEM.Temp>>();
    private HashMap<ASM.Instr, HashSet<MEM.Temp>> out = new HashMap<ASM.Instr, HashSet<MEM.Temp>>();

    public LivenessAnalyzer(Vector<ASM.AsmChunk> asm) {
        this.asm = asm;
    }

    public void setSucc() {
        for (ASM.AsmChunk chunk : this.asm) {
            ASM.Instr prevInstr = null;
            for (ASM.Instr instr : chunk.asm) {
                if (prevInstr != null)
                    prevInstr.addSucc(instr);
                if (instr instanceof ASM.Jump jmp)
                    for (MEM.Label lab : jmp.jumpsTo) {
                        jmp.addSucc(ASM.Instr.labelMap.get(lab));
                        prevInstr = null;
                    }
                else
                    prevInstr = instr;
            }
        }
    }

    public HashSet<MEM.Temp> setIn(ASM.Instr instr) {
        HashSet<MEM.Temp> inSet = new HashSet<MEM.Temp>();
        if (this.out.containsKey(instr))
            inSet.addAll(this.out.get(instr));
        for (IMC.TEMP deffd : instr.def)
            inSet.remove(deffd.temp);
        //inSet.addAll(instr.use);
        for (IMC.TEMP ddd : instr.use) {
            inSet.add(ddd.temp);
        }
        this.in.put(instr, inSet);
        return inSet;
    }

    public HashSet<MEM.Temp> setOut(ASM.Instr instr) {
        HashSet<MEM.Temp> outSet = new HashSet<MEM.Temp>();
        for (ASM.Instr s : instr.succ)
            if (this.in.containsKey(s))
                outSet.addAll(in.get(s));
        this.out.put(instr, outSet);
        return outSet;
    }

    public void setInsOuts() {
        for (ASM.AsmChunk chunk : this.asm) {
            boolean stabilized;
            do {
                stabilized = true;
                for (int i = chunk.asm.size() - 1; i >= 0; i--) {
                    ASM.Instr instr = chunk.asm.get(i);

                    HashSet<MEM.Temp> prevOutSet = null;
                    if (stabilized && !this.out.containsKey(instr))
                        stabilized = false;
                    if (this.out.containsKey(instr))
                        prevOutSet = this.out.get(instr);
                    HashSet<MEM.Temp> prevInSet = null;
                    if (stabilized && !this.in.containsKey(instr))
                        stabilized = false;
                    if (this.in.containsKey(instr))
                        prevInSet = this.in.get(instr);
                    
                    HashSet<MEM.Temp> outSet = this.setOut(instr);
                    if (stabilized && !(outSet.equals(prevOutSet)))
                        stabilized = false;
                    HashSet<MEM.Temp> inSet = this.setIn(instr);
                    if (stabilized && !(inSet.equals(prevInSet)))
                        stabilized = false;
                }
            } while (!stabilized);
        }
    }

    public void analyze() {
        this.setSucc();
        this.setInsOuts();
        this.buildGraph();
    }

    public void emitAll() {
        System.out.println("########## LIVENESS ANALYSIS OUTPUT ##########");
        for (ASM.AsmChunk chunk : this.asm)
            this.emitChunk(chunk);
        System.out.println();
    }

    public void emitChunk(ASM.AsmChunk chunk) {
        System.out.printf("\n##### fun: %s\n", chunk.name);
        for (ASM.Instr instr : chunk.asm)
            this.emit(instr);
    }

    public void emit(ASM.Instr instr) {
        System.out.print(";      IN of next: ");
        for (MEM.Temp temp : this.in.get(instr))
            System.out.printf("%s ", temp);
        System.out.printf("\n%s\n", instr);
        System.out.print("# OUT of prev: ");
        for (MEM.Temp temp : this.out.get(instr))
            System.out.printf("%s ", temp);
    }

    public void buildGraph() {
        for (ASM.AsmChunk chunk : this.asm) {
            LIV.AsmGraph chunkGraph = new LIV.AsmGraph();
            for (ASM.Instr instr : chunk.asm) {
                HashSet<MEM.Temp> inSet = this.in.get(instr);
                for (MEM.Temp temp : inSet) {
                    chunkGraph.addAllEdges(temp, inSet);
                }
                HashSet<MEM.Temp> outSet = this.out.get(instr);
                for (MEM.Temp temp : outSet) {
                    chunkGraph.addAllEdges(temp, outSet);
                }
            }
            //System.out.println("Displaying graph for chunk: " + chunk.name);
            //chunkGraph.display();
            LiveAn.graphMap.put(chunk, chunkGraph);
        }
    }
}
