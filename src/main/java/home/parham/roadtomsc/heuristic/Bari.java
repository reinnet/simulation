/*
 * In The Name Of God
 * ======================================
 * [] Project Name : roadtomsc
 *
 * [] Package Name : home.parham.roadtomsc.heuristic
 *
 * [] Creation Date : 13-07-2019
 *
 * [] Created By : Parham Alvani (parham.alvani@gmail.com)
 * =======================================
 */

package home.parham.roadtomsc.heuristic;

import com.sun.org.apache.xpath.internal.operations.Bool;
import home.parham.roadtomsc.domain.Chain;
import home.parham.roadtomsc.domain.Node;
import home.parham.roadtomsc.domain.Types;
import home.parham.roadtomsc.problem.Config;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class Bari {
    private Config cfg;

    public List<List<Integer>> getPlacement() {
        return Collections.unmodifiableList(
                this.placement.stream().map(Collections::unmodifiableList).collect(Collectors.toList())
        );
    }

    private ArrayList<ArrayList<Integer>> placement;

    public Bari(Config cfg) {
        this.cfg = cfg;
        this.placement = new ArrayList<>();

        this.cfg.getChains().forEach(this::place);
    }

    /**
     * place places the given chain with Bari algorithm
     * @param chain for placement
     */
    private void place(Chain chain) {
        System.out.printf(">> place chain with %d nodes\n", chain.nodes());

        // chain placement array that maps each vnf to its physical node
        ArrayList<Integer> placement = new ArrayList<>();

        // each node represents the stage in Bari multi-stage graph
        for (int stage = 0; stage < chain.nodes() - 1; stage++) {
            System.out.printf(">> stage (%d)\n", stage);
            final Types.Type t = chain.getNode(stage);

            // list the available nodes
            List<Node> nodes = this.cfg.getNodes().stream()
                    .filter(node -> !t.isIngress() || node.isIngress()) // ingress
                    .filter(node -> node.getCores() >= t.getCores()) // number of cores
                    .filter(node -> node.getRam() >= t.getRam()) // amount of ram
                    .filter(node -> !t.isIngress() || node.isEgress()) // egress
                    .collect(Collectors.toList());
            // provide a simple integer as score for each node
            Map<Integer, Integer> scores = nodes.stream().collect(Collectors.toMap(
                    n -> this.cfg.getNodeIndex(n.getName()),
                    n -> 0
            ));

            if (stage > 0) { // use bfs from the last placed node
                int selectedNode = placement.get(stage - 1);
                this.bfs(selectedNode, scores);
            }
            Optional<Map.Entry<Integer, Integer>> op = scores.entrySet().stream().min(Comparator.comparingInt(Map.Entry::getValue));
            if (!op.isPresent()) {
                // there is no way to place the given chain
                this.placement.add(null);
                return;
            }
            int bestNode = op.get().getKey();
            // select a physical node with minimum cost
            placement.add(bestNode);
        }

        this.placement.add(placement);
    }

    /**
     * @param root is the source of BFS that has depth 0
     * @param scores is the map that will be filled by the distance between root and its keys
     */
    private void bfs(int root, Map<Integer, Integer> scores) {
        Queue<Pair<Integer, Integer>> q = new LinkedList<>();
        Map<Integer, Boolean> seen = new HashMap<>();

        q.add(new Pair<>(root, 0));

        while (!q.isEmpty()) {
            Pair<Integer, Integer> p = q.remove();
            int source = p.getKey();
            int depth = p.getValue();

            if (!seen.getOrDefault(source, false))
                continue;

            scores.computeIfPresent(source, (node, score) -> depth);
            seen.put(source, true);

            this.cfg.getLinks().stream()
                    .filter(l -> l.getSource() == source)
                    .forEach(l -> q.add(new Pair<>(l.getDestination(), depth+1)));
        }
    }
}
