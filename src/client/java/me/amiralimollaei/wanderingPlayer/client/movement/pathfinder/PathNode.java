package me.amiralimollaei.wanderingPlayer.client.movement.pathfinder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PathNode {
    public final BlockPos pos;
    public final int hashCode;
    public double prevVelocity = 0;
    public Vec3d prevDelta = Vec3d.ZERO;
    public PathManoeuvre manoeuvre;

    public PathNode(int x, int y, int z, PathManoeuvre manoeuvre) {
        this.pos = new BlockPos(x, y, z);
        this.hashCode = hash(pos.getX(), pos.getY(), pos.getZ());
        this.manoeuvre = manoeuvre;
    }

    public PathNode(BlockPos pos, PathManoeuvre manoeuvre) {
        this.pos = pos;
        this.hashCode = hash(pos.getX(), pos.getY(), pos.getZ());
        this.manoeuvre = manoeuvre;
    }

    public static int hash(int x, int y, int z) {
        return y & 0xFF | (x & 32767) << 8 | (z & 32767) << 24 | (x < 0 ? Integer.MIN_VALUE : 0) | (z < 0 ? 32768 : 0);
    }

    public int getHash() {
        return hashCode;
    }

    public void setPrevVelocity(double value) {
        prevVelocity = value;
    }

    public void setPrevDelta(Vec3d delta) {
        prevDelta = delta;
    }

    public Vec3d getPrevDelta() {
        return prevDelta;
    }

    public double getPrevVelocity() {
        return prevVelocity;
    }

    public Vec3d getVelocityVector() {
        Vec3d prevAngle = getPrevDelta().normalize();
        return new Vec3d(
                getPrevVelocity() * prevAngle.getX(),
                getPrevVelocity() * prevAngle.getY(),
                getPrevVelocity() * prevAngle.getZ()
        );
    }

    public double getSquaredDistance(Vec3d other) {
        Vec3d p = getPosition();
        double xD = Math.pow(p.x - other.x, 2);
        double yD = Math.pow(p.y - other.y, 2);
        double zD = Math.pow(p.z - other.z, 2);
        return xD + yD + zD;
    }

    public double getSquaredDistance(PathNode other) {
        return getSquaredDistance(other.getPosition());
    }

    public double getSquaredDistance(BlockPos other) {
        return getSquaredDistance(other.toCenterPos());
    }

    public double distanceTo(Vec3d other) {
        return Math.sqrt(getSquaredDistance(other));
    }
    public double distanceTo(PathNode other) {
        return Math.sqrt(getSquaredDistance(other));
    }
    public double distanceTo(BlockPos other) {
        return Math.sqrt(getSquaredDistance(other));
    }

    public Vec3d getStandingPosition() {
        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }
    public Vec3d getSwimmingPosition() {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public Vec3d getPosition() {
        return getManoeuvre() == PathManoeuvre.SWIM ? getSwimmingPosition() : getStandingPosition();
    }

    public BlockPos toBlockPos() {
        return pos;
    }

    public PathManoeuvre getManoeuvre() {
        return manoeuvre;
    }
}
