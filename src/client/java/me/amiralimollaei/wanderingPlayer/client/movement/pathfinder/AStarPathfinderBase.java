package me.amiralimollaei.wanderingPlayer.client.movement.pathfinder;

import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public abstract class AStarPathfinderBase {
    public final PathNode start;
    public final PathFinderGoal goal;
    public final PathFinderContext context;
    public final NodeComparator pathComparator = new NodeComparator(this);
    public final PriorityQueue<PathNode> openSet = new PriorityQueue<>(pathComparator);
    public final HashMap<Integer, PathNode> cameFrom = new HashMap<>();
    public final HashMap<Integer, Double> gScore = new HashMap<>();
    public final HashMap<Integer, Double> fScore = new HashMap<>();

    public AStarPathfinderBase(PathNode start, PathFinderGoal goal, PathFinderContext context) {
        this.start = start;
        this.goal = goal;
        this.context = context;
    }

    public double heuristicCost(PathNode node) {
        // Use Euclidean distance or another admissible heuristic
        return 2 * node.distanceTo(goal.getTarget());
    }

    public double heuristicCost(PathNode node1, PathNode node2) {
        // Use Euclidean distance or another admissible heuristic
        return node1.distanceTo(node2);
    }

    public List<PathNode> reconstructPath(PathNode current) {
        List<PathNode> totalPath = new ArrayList<>();
        while (cameFrom.containsKey(current.getHash())) {
            totalPath.add(0, current);
            current = cameFrom.get(current.getHash());
        }
        totalPath.add(0, start); // Add the starting node

        return totalPath;
    }

    public List<PathNode> search() {
        // Initialize starting point
        openSet.add(this.start);
        gScore.put(this.start.getHash(), 0D);
        fScore.put(this.start.getHash(), heuristicCost(this.start));
        while (!openSet.isEmpty()) {
            // Retrieve and remove the node with the lowest fScore
            PathNode current = openSet.poll();
            // Check if the goal is reached
            if (current.getHash() == goal.getHash()) {
                return reconstructPath(current);
            }
            // calculate next step
            this.step(current);
        }
        return null; // No path found
    }

    public void step(PathNode current) {
        throw new NotImplementedException("Method `step` must be implemented");
    }
}
