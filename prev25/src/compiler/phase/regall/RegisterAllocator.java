package compiler.phase.regall;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Vector;

import compiler.common.report.Report;
import compiler.phase.asmgen.*;
import compiler.phase.livean.*;
import compiler.phase.memory.*;

public class RegisterAllocator {
    private Vector<ASM.AsmChunk> asm;
    private HashMap<ASM.AsmChunk, LIV.AsmGraph> graphmap;
    public int numRegs;
    private boolean debug = true;

    public RegisterAllocator(Vector<ASM.AsmChunk> asm, HashMap<ASM.AsmChunk, LIV.AsmGraph> graphmap, int numRegs) {
        this.asm = asm;
        this.graphmap = graphmap;
        this.numRegs = numRegs;
    }

    public boolean allocate() {
        // Graph coloring with k colors
        // Analysis
        // 1. While possible: Remove a vertex with less than k neighbors and put it on a stack.
        // 2. Remove some vertex with more neighbors and put it on the stack, and mark it as potential spill; goto 1.
        // Building
        // 3. Pop a vertex from stack back to the graph.
        //      - If it can be coloured: colour it.
        //      - Otherwise: an actual spill.
        // 4. If there exists an actual spill: fix it (them) -> modify assembly code, and run again.
        for (ASM.AsmChunk chunk : this.asm) {
            LIV.AsmGraph graph = graphmap.get(chunk);
            MEM.Temp vertex = null;
            HashSet<MEM.Temp> neighbors = null;
            
            //UGLY CODE TO BE REFACTORED
            Stack<MEM.Temp> vertexStack = new Stack<MEM.Temp>(); 
            Stack<HashSet<MEM.Temp>> neighborsStack = new Stack<HashSet<MEM.Temp>>();
            Stack<Boolean> spillStack = new Stack<Boolean>();
            // Deconstruct the graph
            while (graph.graph.size() > 0) {

                boolean potentialSpill = true;
                for (MEM.Temp temp : graph.graph.keySet()) {
                    neighbors = graph.graph.get(temp);
                    vertex = temp;
                    if (neighbors.size() < this.numRegs) {
                        potentialSpill = false;
                        break;
                    }
                }
                //System.out.printf("Found vertex %s with %d neighbors", vertex, neighbors.size());
                
                vertexStack.push(vertex);
                neighborsStack.push(neighbors);
                spillStack.push(potentialSpill);

                // Remove purged vertex from its neighbors' connections
                for (MEM.Temp neighbor : neighbors) {
                    HashSet<MEM.Temp> neighborsNeighbors = graph.graph.get(neighbor);
                    neighborsNeighbors.remove(vertex);
                }

                graph.graph.remove(vertex);
            }
            
            // Reconstruct from stacks
            LIV.AsmGraph newGraph = new LIV.AsmGraph();
            HashMap<MEM.Temp, Integer> coloring = new HashMap<MEM.Temp, Integer>(AsmGen.constraints);
            int maxcolor = 0;
            while (vertexStack.size() > 0) {
                MEM.Temp freshVertex = vertexStack.pop();
                //System.out.println("Got fresh vertex " + freshVertex);
                HashSet<MEM.Temp> freshNeighbors = neighborsStack.pop();
                Boolean spill = spillStack.pop();

                /*
                * 
                newGraph.addVertex(freshVertex);
                for (MEM.Temp neighbor : freshNeighbors) {
                    if (newGraph.graph.containsKey(neighbor)) {
                        newGraph.addEdge(freshVertex, neighbor);
                    } else {
                        System.out.println("Some weird order of building graph!");
                    }
                }
                */
                // If already in coloring from constraints, skip it...
                if (coloring.containsKey(freshVertex)) continue;
                // else Try to find a color
                boolean colored = false;
                // Reserve r0 for null
                for (int i = 1; i < this.numRegs; i++) {
                    boolean colorAvailable = true;
                    for (MEM.Temp neighbor : freshNeighbors) {
                        if (coloring.containsKey(neighbor)) {
                            if (coloring.get(neighbor) == i) {
                                colorAvailable = false;
                                break;
                            }
                        } //else
                           // System.out.println("odd behavior in reconstructing graph");
                    }
                    if (colorAvailable) {
                        coloring.put(freshVertex, i);
                        //System.out.println("Colored it with color " + i);
                        colored = true;
                        maxcolor = Integer.max(i, maxcolor);
                        break;
                    }
                }
                if (!colored) {
                    //System.out.printf("Failed to color vertex %s in chunk %s\n", freshVertex, chunk.name);
                    throw new Report.Error("Failed to color vertex " + freshVertex + " in chunk " + chunk.name);
                }
                /**
                if (!colored) {
                     * 
                     if (spill) {
                        System.out.printf("Failed to color potential spill: %s\n.", freshVertex);
                    } else {
                        System.out.printf("Failed to color vertex which was not marked as potential spill: %s\n.", freshVertex);
                    }
                }
                */
            }
            System.out.println("##### Displaying coloring for chunk " + chunk.name + " #####");
            displayColoring(coloring);
            chunk.coloring = coloring;

            // If we are here, this chunk has been successfully colored. Let's burn in the colors.
            // A naive ad hoc solution to determining where to put PUSHJ dest registers:
            // PUSHJ will always work OK if you give it $M+1, where M is the largest register number used in this chunk.
            // Because immediately the result of PUSHJ is used only to SET a register.
            // Instead of coloring with n registers we color with n+1 registers, that's fine.
            boolean setAfterPushj = false;
            Vector<ASM.Instr> deleteRedundantInstrs = new Vector<ASM.Instr>();
            for (ASM.Instr instr : chunk.asm) {
                if (instr instanceof ASM.SET set && setAfterPushj) {
                    if (set.X instanceof ASM.Register reg) {
                        if (reg.physical == null)
                            deleteRedundantInstrs.add(instr);
                    }
                }
                if (instr instanceof ASM.PUSHJ pushj) {
                    coloring.put(pushj.callreg.virtual, maxcolor+1);
                    setAfterPushj = true;
                } else setAfterPushj = false;
                instr.color(coloring);
            }
            
            // Here, we could trim some of the SET's that happen after calls.
            chunk.asm.removeAll(deleteRedundantInstrs);
        }
        return true;
    }

    public void displayColoring(HashMap<MEM.Temp, Integer> coloring) {
        for (MEM.Temp temp : coloring.keySet()) {
            System.out.printf("Temp: %s, register: %d\n", temp, coloring.get(temp));
        }
    }

    public void emitAll() {
        System.out.println("########## REGISTER ALLOCATION OUTPUT ##########");
        for (ASM.AsmChunk chunk : this.asm)
            chunk.emitPhysical();
        System.out.println();
    }
}
