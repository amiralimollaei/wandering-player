package me.amiralimollaei.wanderingPlayer.client.movement.pathfinder;

import me.amiralimollaei.wanderingPlayer.client.WanderingPlayerClient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PathFinderContext {
    public ClientPlayerEntity player;
    public World world;

    public PathFinderContext(ClientPlayerEntity player) {
        this.player = player;
        this.world = player.getWorld();
    }

    public Chunk getPlayerChunk() {
        return world.getChunk(getPlayerPos());
    }

    public int getWorldMaxHeight() {
        return world.getTopY();
    }

    public int getWorldMinHeight() {
        return world.getBottomY();
    }

    public BlockPos getPlayerPos() {
        return player.getBlockPos();
    }

    public @Nullable BlockState getBlockState(BlockPos pos) {
        Chunk chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || chunk.getStatus() != ChunkStatus.FULL) {
            return null; // Return null if the chunk isn't fully loaded or does not exist
        }

        return chunk.getBlockState(pos);
    }

    public @Nullable FluidState getFluidState(BlockPos pos) {
        Chunk chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || chunk.getStatus() != ChunkStatus.FULL) {
            return null; // Return null if the chunk isn't fully loaded or does not exist
        }

        return chunk.getFluidState(pos);
    }

    public @Nullable Block getBlockAt(BlockPos pos) {
        Chunk chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || chunk.getStatus() != ChunkStatus.FULL) {
            return null; // Return null if the chunk isn't fully loaded or does not exist
        }

        return chunk.getBlockState(pos).getBlock();
    }

    public boolean blockExistsAt(BlockPos pos) {
        return getBlockState(pos) != null;
    }

    public VoxelShape getBlockShape(BlockState blockState, BlockPos pos) {
        return blockState.getCollisionShape(world, pos);
    }

    public VoxelShape getFluidShape(BlockState blockState, BlockPos pos) {
        return blockState.getCollisionShape(world, pos);
    }

    public boolean canClipInto(BlockState blockState, BlockPos pos) {
        if (blockState == null) {
            return false;
        }
        return blockState.isAir() || getBlockShape(blockState, pos).isEmpty();
    }

    public boolean canPlayerStandAt(BlockPos pos) {
        return (!canClipInto(getBlockState(pos.down()), pos.down())) &&
                canClipInto(getBlockState(pos), pos) &&
                canClipInto(getBlockState(pos.up()), pos.up());
    }

    public boolean isUnderWater(BlockPos pos) {
        BlockState block = getBlockState(pos);
        return (block != null && block.isOf(Blocks.WATER));
    }

    public boolean isUnderLava(BlockPos pos) {
        BlockState block = getBlockState(pos);
        return (block != null && block.isOf(Blocks.LAVA));
    }

    public boolean isSubmerged(BlockPos pos) {
        BlockState block = getBlockState(pos);
        return (block != null && (block.isOf(Blocks.LAVA) || block.isOf(Blocks.WATER)));
    }

    public boolean canPlayerWalkAt(BlockPos pos) {
        //ClientMain.getLogger().info(
        //        "block position: ({}), name: {}, is safe: {}",
        //        pos.toShortString(),
        //        blockState.getBlock().getName().getString(),
        //        isSafe
        //);
        return canPlayerStandAt(pos) && !isSubmerged(pos);// && !isTouchingUnsafeBlocks();
    }

    public @NotNull List<Integer> getSafeHeightsAt(BlockPos pos, int range) {
        List<Integer> safeHeights = new ArrayList<>();
        int maxY = pos.getY()+range;
        int minY = pos.getY()-range;
        WanderingPlayerClient.getLogger().info("top height: {}, bottom height {}", maxY, minY);
        int y = maxY;
        while (y >= minY) {
            if (canPlayerWalkAt(new BlockPos(pos.getX(), y, pos.getZ()))) {
                safeHeights.add(y);
            }
            y -= 1;
        }
        return safeHeights;
    }

    // TODO: update raycast to only care about blocks that have collision with the player
    public boolean raycastHitsAll(BlockPos startPos, BlockPos endPos) {
        Vec3d start = startPos.toCenterPos();
        Vec3d end = endPos.toCenterPos();

        RaycastContext context = new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.ANY,
                player
        );

        List<HitResult.Type> hitResultTypes = new ArrayList<>();

        BlockView.raycast(start, end, context, (innerContext, pos) -> {
            BlockState blockState = this.getBlockState(pos);
            FluidState fluidState = this.getFluidState(pos);
            assert blockState != null;
            VoxelShape voxelShape = innerContext.getBlockShape(blockState, world, pos);
            BlockHitResult blockHitResult = world.raycastBlock(start, end, pos, voxelShape, blockState);
            VoxelShape voxelShape2 = innerContext.getFluidShape(fluidState, world, pos);
            BlockHitResult blockHitResult2 = voxelShape2.raycast(start, end, pos);
            double d = blockHitResult == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult.getPos());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult2.getPos());
            BlockHitResult finalHitResult = d <= e ? blockHitResult : blockHitResult2;
            hitResultTypes.add(finalHitResult == null ? HitResult.Type.MISS : finalHitResult.getType());
            return null;
        }, innerContext -> {
            Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
            return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
        });

        for (HitResult.Type hittype : hitResultTypes){
            switch (hittype) {
                case BLOCK, ENTITY -> {
                    continue;
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean raycastHitsAny(BlockPos startPos, BlockPos endPos) {
        Vec3d start = startPos.toCenterPos();
        Vec3d end = endPos.toCenterPos();

        RaycastContext context = new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.ANY,
                player
        );

        BlockHitResult blockHitResult = player.getWorld().raycast(context);
        switch (blockHitResult.getType()) {
            case BLOCK, ENTITY -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public List<PathNode> simplifyPath(List<PathNode> path) {
        // this simple algorithm tries to simplify the path as much as possible without changing it
        // we check if any group of 3 consecutive nodes in the path can be simplified by moving directly
        // from the first one to the last if so, remove the middle node.
        // iterate until nothing changes.

        while (true) {
            List<PathNode> simplePath = new ArrayList<>(path);
            for (int i = 0; i < path.size()-2; i+=3) {
                PathNode p1 = path.get(i);
                PathNode p2 = path.get(i+1);
                PathNode p3 = path.get(i+2);

                // we can only simplify the path if the manoeuvre in all nodes is the same
                if (!(p1.getManoeuvre() == p2.getManoeuvre() && p2.getManoeuvre() == p3.getManoeuvre())) {
                    continue;
                }

                // we can only simplify WALK, SWIM or CLIMB manoeuvres, other manoeuvres are more complex and
                // need to be followed precisely per each node.
                switch (p1.getManoeuvre()) {
                    case WALK, SWIM, CLIMB, FALL -> {}
                    default -> {continue;}
                }

                // if the nodes are collinear we can remove the middle one,
                // else if the player doesn't collide with blocks, moving from p1 to p3 and
                // there's solid ground at all times moving from p1 to p3, remove the middle node
                if (MathUtils.areCollinear(p1.getStandingPosition(), p2.getStandingPosition(), p3.getStandingPosition())) {
                    simplePath.remove(p2);
                } else if (raycastHitsAll(p1.pos.down(), p3.pos.down()) && !raycastHitsAny(p1.pos, p3.pos) && !raycastHitsAny(p1.pos.up(), p3.pos.up())) {
                    simplePath.remove(p2);
                }
            }
            if (simplePath.equals(path)) {
                break;
            }
            path = simplePath;
        }

        return path;
    }

    public PathManoeuvre findManoeuvre(PathNode node, BlockPos neighborPos) {
        PathManoeuvre manoeuvre = PathManoeuvre.NULL; // TODO: handle other manoeuvres

        if (canPlayerWalkAt(neighborPos)) {
            manoeuvre = PathManoeuvre.WALK;
        } else if (isUnderWater(neighborPos) || (isUnderLava(neighborPos) && player.isFireImmune())) {
            manoeuvre = PathManoeuvre.SWIM;
        }
        // TODO: handle other manoeuvres

        return manoeuvre;
    }

    public @NotNull List<PathNode> getNeighborsOf(PathNode node, boolean allow_unsafe) {
        BlockPos current = node.toBlockPos();
        List<PathNode> neighbors = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -5; y <= 1; y++) { // we can only drop to 5 blocks down
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Vec3d deltaPos = new Vec3d(x, y, z);

                    BlockPos neighborPos = current.add(x, y, z);
                    BlockState neighborBlockState = getBlockState(neighborPos);

                    if (neighborBlockState == null) {
                        continue;
                    }

                    if (deltaPos.length() > 1) {
                        if (raycastHitsAny(node.pos, neighborPos) || raycastHitsAny(node.pos.up(), neighborPos.up())) {
                            continue;
                        }
                    }

                    PathManoeuvre manoeuvre = findManoeuvre(node, neighborPos);

                    if (manoeuvre == PathManoeuvre.NULL && !allow_unsafe) {
                        continue;
                    }

                    /*if (isLineBlocked(current, neighborPos) && !allow_unsafe) {
                        WanderingPlayerClient.getLogger().info(
                                "Skipped neighbor node for being blocked: {} -> {}", current, neighborPos
                        );
                        continue;
                    }*/

                    neighbors.add(new PathNode(neighborPos, manoeuvre));
                }
            }
        }

        //ClientMain.getLogger().info("Node: {}, Neighbors: {}", node.toCenterPos(), neighbors.size());
        return neighbors;
    }

    public @NotNull List<PathNode> getNeighborsOf(PathNode node) {
        return getNeighborsOf(node, false);
    }

    public @NotNull List<PathNode> getHorizontalNeighborsOf(PathNode node) {
        BlockPos current = node.toBlockPos();
        List<PathNode> neighbors = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos neighborPos = current.add(x, 0, z);
                BlockState neighborBlockState = getBlockState(neighborPos);

                if (neighborBlockState == null) {
                    continue;
                }

                PathManoeuvre manoeuvre = findManoeuvre(node, neighborPos);;

                neighbors.add(new PathNode(neighborPos, manoeuvre));
            }
        }

        //ClientMain.getLogger().info("Node: {}, Neighbors: {}", node.toCenterPos(), neighbors.size());

        return neighbors;
    }
}