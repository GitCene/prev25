package compiler.phase.livean;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import compiler.phase.imcgen.IMC;
import compiler.phase.memory.MEM;

public class LIV {

    private static class AsmVertex {
        public MEM.Temp temp;
        public HashSet<MEM.Temp> neighbors;
        // public Register mapping...
        public AsmVertex(MEM.Temp temp) {
            this.temp = temp;
        }
        public int countNeighbors() {
            return this.neighbors.size();
        }

    }

    public static class NewAsmGraph {
        public LinkedList<LIV.AsmVertex> graph;
        public HashMap<MEM.Temp, LIV.AsmVertex> tempsToVertices;
        
        public NewAsmGraph() {
            this.graph = new LinkedList<LIV.AsmVertex>();
            this.tempsToVertices = new HashMap<MEM.Temp, LIV.AsmVertex>();
        }

        public LIV.AsmVertex ensureVertex(MEM.Temp temp) {
            LIV.AsmVertex vertex;
            if (!this.tempsToVertices.containsKey(temp)) {
                vertex = new LIV.AsmVertex(temp);
                this.graph.addLast(vertex);
                this.tempsToVertices.put(temp, vertex);
            } else {
                vertex = this.tempsToVertices.get(temp);
            }
            return vertex;
        }

        public void addEdge(LIV.AsmVertex v1, LIV.AsmVertex v2) {

        }
    }
    
    public static class AsmGraph {
        public HashMap<MEM.Temp, HashSet<MEM.Temp>> graph;
        public int n;

        public AsmGraph() {
            this.graph = new HashMap<MEM.Temp, HashSet<MEM.Temp>>();
            this.n = 0;
        }

        public void addVertex(MEM.Temp temp) {
            if (!this.graph.containsKey(temp)) {
                this.graph.put(temp, new HashSet<MEM.Temp>());
                this.n++;
            }
        }

        public void addEdge(MEM.Temp t1, MEM.Temp t2) {
            addVertex(t1);
            addVertex(t2);
            this.graph.get(t1).add(t2);
            this.graph.get(t2).add(t1);
        }

        public void addAllEdges(MEM.Temp temp, Set<MEM.Temp> lives) {
            for (MEM.Temp live : lives)
                if (!live.equals(temp))
                    this.addEdge(live, temp);
        }

        public void display() {
            for (MEM.Temp vertex : this.graph.keySet()) {
                HashSet<MEM.Temp> neighbors = this.graph.get(vertex);
                System.out.printf("Variable %s has neighbors: ", vertex);
                for (MEM.Temp neighbor : neighbors) {
                    System.out.printf("%s ", neighbor);
                }
                System.out.println();
            }
        }
    }

}
