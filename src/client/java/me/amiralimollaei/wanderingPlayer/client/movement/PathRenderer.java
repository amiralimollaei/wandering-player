package me.amiralimollaei.wanderingPlayer.client.movement;

import com.mojang.blaze3d.systems.RenderSystem;
import me.amiralimollaei.wanderingPlayer.client.movement.pathfinder.PathNode;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;


import static net.minecraft.client.render.VertexFormats.OVERLAY_ELEMENT;


public class PathRenderer {
    private final MinecraftClient client;

    public PathRenderer(MinecraftClient client) {
        this.client = client;
    }

    public void register() {
        // Register the render event
        WorldRenderEvents.END.register(this::onRenderWorld);
    }

    private void onRenderWorld(WorldRenderContext context) {
        // Retrieve the list of PathNodes
        List<Position> positions = getPositions();

        if (positions == null || positions.size() < 2) {
            return;
        }

        // Set up the rendering context
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider vertexConsumers = context.consumers();

        // Get the player's camera position (not used for translation or rotation here)
        Vec3d cameraPos = context.camera().getPos();

        // Apply translation based on camera position, to render the path in world space.
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Disable depth testing to ensure the lines render on top of blocks
        //RenderSystem.disableDepthTest();

        // Render lines between consecutive PathNodes
        for (int i = 0; i < positions.size() - 1; i++) {
            Position start = positions.get(i);
            Position end = positions.get(i + 1);

            Vec3d startPos = start.pos;
            Vec3d endPos = end.pos;

            // Set the light value of the nodes
            int light = context.world().getMaxLightLevel();

            // Render a line from startPos to endPos
            renderLine(matrices, vertexConsumers, startPos, endPos, light);

            // Render indicator markers at the start and end of this line.
            // Here we use red for the start and green for the end.
            renderIndicator(matrices, vertexConsumers, endPos, light, start.isCurrent ? 0x00FF00 : 0xFF0000);
        }

        // Enable depth testing again after rendering the lines
        //RenderSystem.enableDepthTest();
    }

    private void renderLine(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d start, Vec3d end, int light) {

        // Push the current matrix
        matrices.push();

        assert client.player != null;
        Vec3d playerPos = client.player.getPos();

        // Set the line width to make the lines thicker (default is 1.0f, increase for thicker lines)
        RenderSystem.lineWidth(5F);  // You can adjust the width as needed

        // Set up the vertex consumer
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());

        // Define the start and end points of the line
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) start.x, (float) start.y, (float) start.z)
                .color(127, (int) Math.min(Math.sqrt(playerPos.distanceTo(start))*25.5, 255), 0, 255)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .light(light)
                .normal(0, 0, 0)
                .next();

        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) end.x, (float) end.y, (float) end.z)
                .color(127, (int) Math.min(Math.sqrt(playerPos.distanceTo(end))*25.5, 255), 0, 255)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .light(light)
                .normal(0, 0, 0)
                .next();

        // Set the line width to make the lines thicker (default is 1.0f, increase for thicker lines)
        RenderSystem.lineWidth(1.0f);  // You can adjust the width as needed

        // Pop the matrix
        matrices.pop();
    }

    /**
     * Renders a small cross indicator at the given point.
     *
     * @param matrices        the MatrixStack for transformations
     * @param vertexConsumers the VertexConsumerProvider
     * @param point           the world coordinate for the indicator
     * @param light           light level
     * @param color           an integer representing the RGB color (e.g., 0xFF0000 for red)
     */
    private void renderIndicator(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d point, int light, int color) {
        matrices.push();
        // Optionally, set a different line width for indicators
        RenderSystem.lineWidth(3.0F);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());

        // Size of the indicator cross
        float size = 1F/16F;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 255;

        // side 1
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) (point.x + size), (float) point.y, (float) (point.z + size))
                .color(r, g, b, a)
                .light(light)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .normal(0, 0, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) (point.x + size), (float) point.y, (float) (point.z - size))
                .color(r, g, b, a)
                .light(light)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .normal(0, 0, 0)
                .next();

        // side 2
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) (point.x + size), (float) point.y, (float) (point.z + size))
                .color(r, g, b, a)
                .light(light)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .normal(0, 0, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) (point.x - size), (float) point.y, (float) (point.z + size))
                .color(r, g, b, a)
                .light(light)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .normal(0, 0, 0)
                .next();

        // side 3
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) (point.x - size), (float) point.y, (float) (point.z + size))
                .color(r, g, b, a)
                .light(light)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .normal(0, 0, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) (point.x - size), (float) point.y, (float) (point.z - size))
                .color(r, g, b, a)
                .light(light)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .normal(0, 0, 0)
                .next();

        // side 4
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) (point.x + size), (float) point.y, (float) (point.z - size))
                .color(r, g, b, a)
                .light(light)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .normal(0, 0, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) (point.x - size), (float) point.y, (float) (point.z - size))
                .color(r, g, b, a)
                .light(light)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .normal(0, 0, 0)
                .next();

        // Reset line width for subsequent rendering
        RenderSystem.lineWidth(1.0F);
        matrices.pop();
    }

    /**
     * Renders a small cube centered at the given point using RenderLayer.getSolid().
     *
     * @param matrices        the MatrixStack for transformations
     * @param vertexConsumers the VertexConsumerProvider
     * @param center          the world coordinate for the cube center
     * @param light           light level
     * @param color           an integer representing the RGB color (e.g., 0xFF0000 for red)
     */
    private void renderCubeIndicator(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d center, int light, int color) {
        matrices.push();
        // Get the vertex consumer from the solid render layer.
        VertexConsumer cubeConsumer = vertexConsumers.getBuffer(RenderLayer.getSolid());

        float halfSize = 1F/32F;
        // Use the standard Minecraft coordinate system: (x, y, z) where y is vertical.
        float x = (float) center.x;
        float y = (float) center.y;
        float z = (float) center.z;

        // Define the 8 vertices of the cube in standard (x, y, z) order.
        float[] vx = new float[8];
        float[] vy = new float[8];
        float[] vz = new float[8];

        vx[0] = x - halfSize; vy[0] = y - halfSize; vz[0] = z - halfSize;
        vx[1] = x + halfSize; vy[1] = y - halfSize; vz[1] = z - halfSize;
        vx[2] = x + halfSize; vy[2] = y + halfSize; vz[2] = z - halfSize;
        vx[3] = x - halfSize; vy[3] = y + halfSize; vz[3] = z - halfSize;
        vx[4] = x - halfSize; vy[4] = y - halfSize; vz[4] = z + halfSize;
        vx[5] = x + halfSize; vy[5] = y - halfSize; vz[5] = z + halfSize;
        vx[6] = x + halfSize; vy[6] = y + halfSize; vz[6] = z + halfSize;
        vx[7] = x - halfSize; vy[7] = y + halfSize; vz[7] = z + halfSize;

        // Extract RGB components from the color integer.
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 255;

        // Define each face with a proper winding order:
        // Front face (facing positive Z): vertices 4, 5, 6, 7
        addCubeFace(cubeConsumer, matrices, vx, vy, vz, 4, 5, 6, 7, r, g, b, a, light, 0, 0, 1);
        // Back face (facing negative Z): vertices 0, 3, 2, 1 (to maintain counterclockwise order)
        addCubeFace(cubeConsumer, matrices, vx, vy, vz, 0, 3, 2, 1, r, g, b, a, light, 0, 0, -1);
        // Left face (facing negative X): vertices 0, 4, 7, 3
        addCubeFace(cubeConsumer, matrices, vx, vy, vz, 0, 4, 7, 3, r, g, b, a, light, -1, 0, 0);
        // Right face (facing positive X): vertices 1, 2, 6, 5
        addCubeFace(cubeConsumer, matrices, vx, vy, vz, 1, 2, 6, 5, r, g, b, a, light, 1, 0, 0);
        // Top face (facing positive Y): vertices 3, 7, 6, 2
        addCubeFace(cubeConsumer, matrices, vx, vy, vz, 3, 7, 6, 2, r, g, b, a, light, 0, 1, 0);
        // Bottom face (facing negative Y): vertices 0, 1, 5, 4
        addCubeFace(cubeConsumer, matrices, vx, vy, vz, 0, 1, 5, 4, r, g, b, a, light, 0, -1, 0);

        matrices.pop();
    }




    /**
     * Adds two triangles (one face) for the cube.
     *
     * @param consumer the VertexConsumer
     * @param matrices the MatrixStack
     * @param vx       the x coordinates of cube vertices
     * @param vy       the y coordinates of cube vertices
     * @param vz       the z coordinates of cube vertices
     * @param i0       index of first vertex
     * @param i1       index of second vertex
     * @param i2       index of third vertex
     * @param i3       index of fourth vertex
     * @param r        red component
     * @param g        green component
     * @param b        blue component
     * @param a        alpha component
     * @param light    light level
     * @param nx       normal x
     * @param ny       normal y
     * @param nz       normal z
     */
    private void addCubeFace(VertexConsumer consumer, MatrixStack matrices, float[] vx, float[] vy, float[] vz,
                             int i0, int i1, int i2, int i3,
                             int r, int g, int b, int a, int light,
                             float nx, float ny, float nz) {
        // First triangle: vertices i0, i1, i2
        consumer.vertex(matrices.peek().getPositionMatrix(), vx[i0], vy[i0], vz[i0])
                .color(r, g, b, a)
                .texture(0, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .light(light)
                .normal(nx, ny, nz)
                .next();
        consumer.vertex(matrices.peek().getPositionMatrix(), vx[i1], vy[i1], vz[i1])
                .color(r, g, b, a)
                .texture(1, 0)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .light(light)
                .normal(nx, ny, nz)
                .next();
        consumer.vertex(matrices.peek().getPositionMatrix(), vx[i2], vy[i2], vz[i2])
                .color(r, g, b, a)
                .texture(1, 1)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .light(light)
                .normal(nx, ny, nz)
                .next();
        // Second triangle: vertices i0, i3, i2 (swapping i2 and i3 fixes the winding)
        consumer.vertex(matrices.peek().getPositionMatrix(), vx[i3], vy[i3], vz[i3])
                .color(r, g, b, a)
                .texture(0, 1)
                .overlay(OVERLAY_ELEMENT.getUvIndex())
                .light(light)
                .normal(nx, ny, nz)
                .next();
    }


    private List<Position> getPositions() {
        if (PathExecutor.nodesList == null || PathExecutor.pathList == null) {
            return null;
        }
        List<Position> positionList = new ArrayList<>();
        for (int i = 0; i < PathExecutor.nodesList.size()-1; i++) {
            PathNode node = PathExecutor.nodesList.get(i);
            List<Vec3d> positions = PathExecutor.pathList.get(node.getHash());
            for (int j = 0; j < positions.size(); j++) {
                Vec3d pos = positions.get(j);
                positionList.add(
                        new Position(
                                pos,
                                i == PathExecutor.currentNodeIdx && j == PathExecutor.currentPositionIdx
                        )
                );
            }
        }
        return positionList;
    }

    private record Position(Vec3d pos, boolean isCurrent) {
    }
}