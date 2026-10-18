package com.diningphilosophers.detection;

import com.diningphilosophers.core.Fork;
import com.diningphilosophers.core.Philosopher;
import com.diningphilosophers.core.PhilosopherState;
import com.diningphilosophers.core.Table;

import java.util.*;

/**
 * Wait-For Graph (WFG) representation of resource dependencies among Philosophers.
 *
 * Syllabus Concepts:
 * - RESOURCE ALLOCATION GRAPH (RAG) & WAIT-FOR GRAPH (WFG):
 *   In single-unit resource systems, a cycle in the Wait-For Graph is a NECESSARY and SUFFICIENT
 *   condition for deadlock.
 * - Nodes represent active threads (Philosophers).
 * - Directed edge (Pi -> Pj) indicates that Philosopher Pi is blocked waiting for a Fork currently held by Pj.
 * - Cycle Detection: Depth-First Search (DFS) with recursion stack tracking to locate closed dependency loops.
 */
public class WaitForGraph {
    private final Map<Integer, Integer> edges = new HashMap<>(); // From waiting philosopher -> holding philosopher
    private final Map<Integer, Integer> waitingForForks = new HashMap<>(); // From waiting philosopher -> forkId

    /**
     * Constructs a snapshot of the current Wait-For Graph from the active Table state.
     */
    public static WaitForGraph build(Table table) {
        WaitForGraph wfg = new WaitForGraph();
        List<Philosopher> philosophers = table.getPhilosophers();

        for (Philosopher p : philosophers) {
            Integer waitingForkId = p.getWaitingForForkId();
            if (p.getState() == PhilosopherState.BLOCKED && waitingForkId != null) {
                Fork contestedFork = table.getFork(waitingForkId);
                Integer ownerId = contestedFork.getCurrentOwnerId();
                if (ownerId != null && !ownerId.equals(p.getId())) {
                    wfg.edges.put(p.getId(), ownerId);
                    wfg.waitingForForks.put(p.getId(), waitingForkId);
                }
            }
        }
        return wfg;
    }

    public Map<Integer, Integer> getEdges() {
        return Collections.unmodifiableMap(edges);
    }

    public Map<Integer, Integer> getWaitingForForks() {
        return Collections.unmodifiableMap(waitingForForks);
    }

    /**
     * Detects if there is a cycle in the Wait-For Graph using DFS.
     * Returns the ordered cycle list of philosopher IDs (e.g., [0, 1, 2, 3, 4, 0]),
     * or empty list if no cycle exists.
     */
    public List<Integer> findCycle() {
        Set<Integer> visited = new HashSet<>();
        Set<Integer> onStack = new HashSet<>();
        List<Integer> path = new ArrayList<>();

        for (Integer startNode : edges.keySet()) {
            if (!visited.contains(startNode)) {
                List<Integer> cycle = dfs(startNode, visited, onStack, path);
                if (cycle != null && !cycle.isEmpty()) {
                    return cycle;
                }
            }
        }
        return Collections.emptyList();
    }

    private List<Integer> dfs(Integer current, Set<Integer> visited, Set<Integer> onStack, List<Integer> path) {
        visited.add(current);
        onStack.add(current);
        path.add(current);

        Integer neighbor = edges.get(current);
        if (neighbor != null) {
            if (!visited.contains(neighbor)) {
                List<Integer> cycle = dfs(neighbor, visited, onStack, path);
                if (cycle != null) return cycle;
            } else if (onStack.contains(neighbor)) {
                // Cycle detected! Extract the cyclic loop portion of the path
                int cycleStartIndex = path.indexOf(neighbor);
                if (cycleStartIndex != -1) {
                    List<Integer> cyclePath = new ArrayList<>(path.subList(cycleStartIndex, path.size()));
                    cyclePath.add(neighbor); // Close the cycle loop
                    return cyclePath;
                }
            }
        }

        path.remove(path.size() - 1);
        onStack.remove(current);
        return null;
    }

    /**
     * Formats cycle path as a readable string: "P0 -> P1 -> P2 -> P3 -> P4 -> P0"
     */
    public static String formatCyclePath(List<Integer> cycle) {
        if (cycle == null || cycle.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cycle.size(); i++) {
            sb.append("P").append(cycle.get(i));
            if (i < cycle.size() - 1) {
                sb.append(" -> ");
            }
        }
        return sb.toString();
    }
}
