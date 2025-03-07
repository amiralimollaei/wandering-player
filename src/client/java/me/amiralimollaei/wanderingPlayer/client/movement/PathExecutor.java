package me.amiralimollaei.wanderingPlayer.client.movement;

import me.amiralimollaei.wanderingPlayer.client.WanderingPlayerClient;
import me.amiralimollaei.wanderingPlayer.client.movement.pathfinder.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathExecutor {
    public boolean isEnabled = false;

    private final @NotNull MinecraftClient client;
    private ClientPlayerEntity player;

    private PathFinderContext context;
    public static List<PathNode> nodesList = new ArrayList<>();
    // a Map that connects each node to the node after it in multiple steps
    public static Map<Integer, List<Vec3d>> pathList = new HashMap<>();
    // contains all positions in pathList
    public static List<Vec3d> positionList = new ArrayList<>();

    // execution variables
    public static int currentNodeIdx = -1;
    public static int currentPositionIdx = -1;

    // constants
    public final double DELTA_EMA_ALPHA = 0.5;
    public final int PATH_RESCALING_TIMES = 3;

    public PathExecutor(MinecraftClient client){
        assert client.player != null;
        this.client = client;
        this.player = client.player;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        if (!enabled) {
            stopMovement();
        }
    }

    public void setNodes(List<PathNode> pathNodes) {
        player = client.player;
        assert client.player != null;
        context = new PathFinderContext(player);

        clear();
        nodesList = pathNodes;
        preparePath();
    }

    public void preparePath() {
        // if we don't have at least two nodes we cannot calculate a path
        if (nodesList == null || nodesList.size() < 2) {
            return;
        }

        for (int nodeIdx = 0; nodeIdx < nodesList.size() -1; nodeIdx++) {
            PathNode currNode = nodesList.get(nodeIdx);
            PathNode nextNode = nodesList.get(nodeIdx+1);

            Vec3d currPos = currNode.getManoeuvre() == PathManoeuvre.SWIM ? currNode.getSwimmingPosition() : currNode.getStandingPosition();
            Vec3d nextPos = nextNode.getManoeuvre() == PathManoeuvre.SWIM ? nextNode.getSwimmingPosition() : nextNode.getStandingPosition();

            // avoid colliding with obstacles or unsafe blocks (lava, cacti, etc.)
            Vec3d currUnsafePreventionDelta = getUnsafeDirection(currNode).multiply(0.5);
            Vec3d nextUnsafePreventionDelta = getUnsafeDirection(nextNode).multiply(0.5);
            currPos = currPos.add(currUnsafePreventionDelta);
            nextPos = nextPos.add(nextUnsafePreventionDelta);

            // perform linear interpolation: [current, next)
            List<Vec3d> linearPosList = new ArrayList<>();
            int interpolationTimes = (int) Math.ceil((float) PATH_RESCALING_TIMES * nextPos.distanceTo(currPos));
            if (interpolationTimes > 0) {
                for (int i = 0; i < interpolationTimes; i++) {
                    double alpha = (double) i / (double) interpolationTimes;
                    Vec3d interpolatedPos = currPos.multiply(1-alpha).add(nextPos.multiply(alpha));
                    linearPosList.add(interpolatedPos);
                }
            } else {
                linearPosList.add(currPos);
            }

            pathList.put(currNode.getHash(), applyManoeuvresConstraints(linearPosList, currNode));
        }

        PathNode lastNode = nodesList.get(nodesList.size()-1);
        List<Vec3d> lastPosList = new ArrayList<>();
        lastPosList.add(lastNode.getPosition());
        pathList.put(lastNode.getHash(), lastPosList);

        positionList = new ArrayList<>();
        for (PathNode node : nodesList) {
            List<Vec3d> positions = pathList.get(node.getHash());
            if (positions != null){
                positionList.addAll(positions);
            }
        }
    }

    private static @NotNull List<Vec3d> applyManoeuvresConstraints(List<Vec3d> linearPosList, PathNode currNode) {
        List<Vec3d> modifiedPosList = new ArrayList<>();
        for (Vec3d pos: linearPosList) {
            Vec3d newPos;
            // TODO: add more Manoeuvres
            switch (currNode.getManoeuvre()) {
                case WALK -> {
                    newPos = new Vec3d(pos.x, Math.round(pos.y), pos.z);
                }
                default -> {
                    newPos = pos;
                }
            }
            modifiedPosList.add(newPos);
        }
        return modifiedPosList;
    }

    private Vec3d getUnsafeDirection(PathNode node) {
        Vec3d unsafePreventionDelta = new Vec3d(0, 0, 0);
        for (PathNode neighbor : context.getHorizontalNeighborsOf(node)) {
            if (neighbor.getManoeuvre() != PathManoeuvre.NULL) {
                continue;
            }
            unsafePreventionDelta = unsafePreventionDelta.add(
                    node.pos.subtract(neighbor.pos).toCenterPos()
            );
        }
        Vec3d unsafeDirection = unsafePreventionDelta.normalize();
        WanderingPlayerClient.getLogger().debug(
                "node={}, unsafeDirection={}",
                node,
                unsafeDirection
        );

        return unsafeDirection;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tickMovement);
    }

    public void clear() {
        nodesList.clear();
        pathList.clear();
        positionList.clear();
        currentNodeIdx = -1; // reset node index
        currentPositionIdx = -1; // reset position index
    }

    public void finishExecution() {
        stopMovement();
        clear();
    }

    public void tickMovement(MinecraftClient client) {
        if (player == null || !isEnabled) {
            return;
        }

        // Check if path is empty or the target is reached
        if (nodesList == null || nodesList.isEmpty()) {
            return;
        }

        Vec3d playerPos = player.getPos();
        Vec3d targetPosition = nodesList.get(nodesList.size()-1).getPosition();

        // check if we have reached the target
        if (getWeightedDistance(targetPosition, playerPos, 0.2) <= 1.0) {
            finishExecution();
            return;
        }

        // find the node that we are currently at
        // the current node is closest to the player that its index is higher than the current index and
        // its distance is lower than the threshold
        double minNodeDistance = 1.0;
        if (currentNodeIdx == -1) {
            minNodeDistance = 4.0;
        } else if (nodesList.size() > (currentNodeIdx+1)) {
            minNodeDistance = 1.2 * nodesList.get(currentNodeIdx).distanceTo(nodesList.get(currentNodeIdx+1));
        }
        for (int i = currentNodeIdx + 1; i < nodesList.size(); i++) {
            PathNode node = nodesList.get(i);
            Vec3d nodePos = node.getManoeuvre() == PathManoeuvre.SWIM ? node.getSwimmingPosition() : node.getStandingPosition();
            double weightedDistance = getWeightedDistance(nodePos, playerPos, 0.1);
            if (weightedDistance < minNodeDistance) {
                currentNodeIdx = i;
                currentPositionIdx = -1; // reset position index
                minNodeDistance = weightedDistance;
            }
        }

        // Get the next node to move towards
        PathNode currentNode = nodesList.get(currentNodeIdx);

        List<Vec3d> currentPosList = pathList.get(currentNode.getHash());
        double minPositionDistance = 1.0;
        if (currentPositionIdx == -1) {
            minPositionDistance = Double.POSITIVE_INFINITY;
        } else if (currentPosList.size() > (currentPositionIdx+1)) {
            minPositionDistance = 1.2 * currentPosList.get(currentPositionIdx).distanceTo(currentPosList.get(currentPositionIdx+1));
        }
        for (int i = currentPositionIdx + 1; i < currentPosList.size(); i++) {
            Vec3d pos = currentPosList.get(i);
            // current pos is the closest pos to the player
            double weightedDistance = getWeightedDistance(pos, playerPos, 0.1);
            if (weightedDistance < minPositionDistance) {
                currentPositionIdx = i;
                minPositionDistance = weightedDistance;
            }
        }
        Vec3d currentTarget = currentPosList.get(currentPositionIdx);

        Vec3d deltaPos = currentTarget.subtract(playerPos);

        HeadDirection headDirection = getHeadDirection(currentTarget);

        switch (currentNode.getManoeuvre()) {
            case WALK -> { // walking, jumping, running
                player.setYaw(headDirection.yaw());
                player.prevYaw = headDirection.yaw();

                client.options.forwardKey.setPressed(true);

                double distance = getWeightedDistance(currentNode.getPosition(), playerPos, 0.1);
                player.setSprinting(distance > 1.0);
                client.options.jumpKey.setPressed(player.isOnGround() && (deltaPos.y > 0.3));
            }
            case SWIM -> { // swimming both on the surface of water/lava and underwater/lava.
                player.setYaw(headDirection.yaw());
                player.prevYaw = headDirection.yaw();

                client.options.forwardKey.setPressed(true);

                player.setPitch(headDirection.pitch());
                player.prevPitch = headDirection.pitch();

                player.setSprinting(true);

                client.options.jumpKey.setPressed(headDirection.delta().y > 0.3);
            }
            default -> throw new NotImplementedException();
        }
    }

    private @NotNull PathExecutor.HeadDirection getHeadDirection(Vec3d currentTarget) {
        Vec3d eyesPos = EntityAnchorArgumentType.EntityAnchor.EYES.positionAt(player);
        Vec3d delta = currentTarget.subtract(eyesPos);

        Vec3d direction = delta.normalize();
        Vec3d currDirection = Vec3d.fromPolar(player.getPitch(), player.getYaw());

        direction = currDirection.multiply(1-DELTA_EMA_ALPHA).add(direction.multiply(DELTA_EMA_ALPHA));

        float yaw = MathHelper.wrapDegrees((float)(MathHelper.atan2(direction.z, direction.x) * 180.0F / (float)Math.PI) - 90.0F);
        float pitch = MathHelper.wrapDegrees((float)(Math.atan2(Math.sqrt(direction.z * direction.z + direction.x * direction.x), direction.y) * 180.0F / (float)Math.PI) - 90.0F);
        return new HeadDirection(delta, yaw, pitch);
    }

    private record HeadDirection(Vec3d delta, float yaw, float pitch) { }

    private double getWeightedDistance(Vec3d nodePos, Vec3d playerPos, double deltaTime) {
        assert deltaTime >= 0.0;
        double heightWeight = 0.2;
        if (deltaTime < 0.001) {
            return MathUtils.distanceBetweenPointAndLine(
                nodePos.multiply(1, heightWeight, 1),
                playerPos.subtract(player.getVelocity().multiply(deltaTime)).multiply(1, heightWeight, 1),
                playerPos.add(player.getVelocity().multiply(deltaTime)).multiply(1, heightWeight, 1)
            );
        } else {
            return Math.sqrt(
                Math.pow(nodePos.x - playerPos.x, 2) +
                Math.pow(nodePos.y - playerPos.y, 2) * heightWeight +
                Math.pow(nodePos.z - playerPos.z, 2)
            );
        }
    }

    private void stopMovement() {
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        player.setSprinting(false);
        player.setJumping(false);
    }
}
