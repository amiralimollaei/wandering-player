package me.amiralimollaei.wanderingPlayer.client;

import me.amiralimollaei.wanderingPlayer.client.movement.PathRenderer;
import me.amiralimollaei.wanderingPlayer.client.movement.PathExecutor;
import me.amiralimollaei.wanderingPlayer.client.movement.pathfinder.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WanderingPlayerClient implements ClientModInitializer {
    private static KeyBinding pathfindKey, pathExecuteKey;
    public static final String MOD_ID = "wandering-player";
    public static final String MOD_VERSION = "1.0.0-SNAPSHOT";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static PathExecutor pathExecutor;
    public static PathRenderer pathRenderer;

    @Override
    public void onInitializeClient() {
        MinecraftClient mc = MinecraftClient.getInstance();
        pathExecutor = new PathExecutor(mc);
        pathExecutor.register();
        pathRenderer = new PathRenderer(mc);
        pathRenderer.register();

        pathfindKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wandering-player.path.search",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "category.wandering-player.title"
        ));

        pathExecuteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wandering-player.path.execute",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "category.wandering-player.title"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (pathfindKey.wasPressed()) {
                ClientPlayerEntity player = client.player;
                if (player == null) return;

                PathFinderContext context = new PathFinderContext(player);

                BlockPos targetPos = new BlockPos(0, 0, 0); //getPlayerLookBlockPos(player);

                World world = player.getWorld();
                BlockPos start = BlockPos.ofFloored(player.getPos());
                BlockPos target = new BlockPos(
                        targetPos.getX(),
                        world.getTopY(Heightmap.Type.WORLD_SURFACE, targetPos.getX(), targetPos.getZ()),
                        targetPos.getZ()
                );

                /*if (!(context.canPlayerWalkAt(target) || context.isSubmerged(target))) {
                    player.sendMessage(Text.literal("You can not reach that target.").withColor(Colors.RED));
                    return;
                }*/

                AStarPathfinderBase pathfinder = new AStarPathfinder(
                        new PathNode(start.getX(), start.getY(), start.getZ(), PathManoeuvre.WALK),
                        new PathFinderGoal(target),
                        context
                );

                List<PathNode> path = pathfinder.search();
                if (path != null) {
                    // simplify/smooth the path
                    path = context.simplifyPath(path);
                    pathExecutor.setNodes(path);
                } else {
                    player.sendMessage(Text.literal("You can not reach that target.").withColor(Colors.RED));
                    return;
                }
            }

            while (pathExecuteKey.wasPressed()) {
                pathExecutor.setEnabled(!pathExecutor.isEnabled);
            }
        });
    }

    public static BlockPos getPlayerLookBlockPos(ClientPlayerEntity player) {
        // Get the player's position and rotation
        Vec3d playerPos = player.getPos();

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        Vec3d lookDirection = Vec3d.fromPolar(pitch, yaw);

        // Perform raycast
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(lookDirection.multiply(128.0));

        BlockHitResult hit = player.getWorld().raycast(
                new RaycastContext(
                        start,
                        end,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.ANY,
                        player)
        );
        return hit.getBlockPos();
    }

    public static String getModId() {
        return MOD_ID;
    }

    public static Logger getLogger(){
        return LOGGER;
    }
}
