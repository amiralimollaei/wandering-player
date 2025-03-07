package me.amiralimollaei.wanderingPlayer.client.movement.pathfinder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/*
Lazy A* Pathfinder is a variant of A*, optimized for grids where instead of
searching for all possible neighbours in each search step, we repeat the last
action done (e.g. if the last action was moving 1 block forward, we repeat that)
until we can't, if that scenario happens, we do a full neighbor search.

this algorithm is NOT guaranteed to be optimal, but it should be significantly faster
for navigating *simple* terrain.
 */
public class LazyAStarPathfinder extends AStarPathfinderBase {
    // this does NOT have to match minecraft's acceleration speed, used to penalize the pathfinder to
    // achieve the highest velocity, effectively finding paths that have smoother turns
    public final double ACCELERATION_SPEED = 0.2;

    public PathNode prevNode = null;

    public LazyAStarPathfinder(PathNode start, PathFinderGoal goal, PathFinderContext context) {
        super(start, goal, context);
    }

    private void doNeighborSearch(PathNode current) {
        // Explore neighbors
        for (PathNode neighbor : context.getNeighborsOf(current)) {
            double dCost = heuristicCost(current, neighbor);
            double tentativeGScore = gScore.getOrDefault(current.getHash(), Double.MAX_VALUE) + dCost;

            Vec3d deltaPos = neighbor.getStandingPosition().subtract(current.getStandingPosition());
            neighbor.setPrevDelta(deltaPos);

            // calculate the velocity that an object gains by going from current node to the neighbor node
            // regarding of the initial velocity
            double velocityGained = Math.sqrt(2*ACCELERATION_SPEED*deltaPos.length());

            double prevVelocity = current.getPrevVelocity();
            // calculate the new velocity considering the initial velocity and the angle of the turn
            double velocity = velocityGained + current.getVelocityVector().dotProduct(deltaPos.normalize());

            neighbor.setPrevVelocity(velocity);

            //WanderingPlayerClient.getLogger().info(
            //        "pos={} velocity={}", neighbor.pos, neighbor.getPrevVelocity()
            //);

            double prevGScore = gScore.getOrDefault(neighbor.getHash(), Double.MAX_VALUE);

            if ((tentativeGScore - velocity * 0.5) < (prevGScore - prevVelocity * 0.5)) {
                // Update path information
                cameFrom.put(neighbor.getHash(), current);
                gScore.put(neighbor.getHash(), tentativeGScore);
                fScore.put(neighbor.getHash(), tentativeGScore + heuristicCost(neighbor));

                // Add or update the neighbor in the open set
                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor);
                }
            }
        }
    }

    public void step(PathNode current) {
        if (prevNode != null) {
            BlockPos deltaBlockPos = current.pos.subtract(prevNode.pos);
            BlockPos newLazyPos = current.pos.add(deltaBlockPos);
            PathManoeuvre manoeuvre  = context.findManoeuvre(current, newLazyPos);
            if (manoeuvre != PathManoeuvre.NULL) {
                PathNode neighbor = new PathNode(newLazyPos, manoeuvre);
                double dCost = heuristicCost(current, neighbor);
                double tentativeGScore = gScore.getOrDefault(current.getHash(), Double.MAX_VALUE) + dCost;

                // Update path information
                cameFrom.put(neighbor.getHash(), current);
                gScore.put(neighbor.getHash(), tentativeGScore);
                fScore.put(neighbor.getHash(), tentativeGScore + heuristicCost(neighbor));

                // Add or update the neighbor in the open set
                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor);
                }
            } else {
                doNeighborSearch(current);
            }
        } else {
            doNeighborSearch(current);
        }
        prevNode = current;
    }
}
