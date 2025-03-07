package me.amiralimollaei.wanderingPlayer.client.movement.pathfinder;

import net.minecraft.util.math.BlockPos;

public class PathFinderGoal {
    public BlockPos pos;
    public int hashCode;

    public PathFinderGoal(BlockPos pos) {
        this.pos = pos;
        hashCode = PathNode.hash(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPos getTarget() {
        return pos;
    }

    public int getHash() {
        return hashCode;
    }
}
