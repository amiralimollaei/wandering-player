package me.amiralimollaei.wanderingPlayer.client.movement.pathfinder;

import java.util.Comparator;

public class NodeComparator implements Comparator<PathNode> {
    public AStarPathfinderBase pathfinder;

    public NodeComparator(AStarPathfinderBase pathfinder) {
        this.pathfinder = pathfinder;
    }

    private double getScore(PathNode node) {
        return this.pathfinder.fScore.getOrDefault(node.getHash(), Double.POSITIVE_INFINITY) - node.getPrevVelocity();
    }

    @Override
    public int compare(PathNode o1, PathNode o2) {
        return Double.compare(getScore(o1), getScore(o2));
    }
}