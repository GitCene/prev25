package compiler.phase.livean;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import compiler.phase.imcgen.IMC;
import compiler.phase.memory.MEM;

public class LIV {
    
    public static class AsmGraph {
        public HashMap<MEM.Temp, List<MEM.Temp>> graph;
        public int n;

        public AsmGraph() {
            this.graph = new HashMap<MEM.Temp, List<MEM.Temp>>();
            this.n = 0;
        }

        public void addVertex(MEM.Temp temp) {
            if (!this.graph.containsKey(temp)) {
                this.graph.put(temp, new LinkedList<MEM.Temp>());
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
                this.addEdge(live, temp);
        }
    }

}
