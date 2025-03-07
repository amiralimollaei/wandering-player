package me.amiralimollaei.wanderingPlayer.client.movement.pathfinder;

import net.minecraft.util.math.Vec3d;

public class MathUtils {
    /**
     * Determines if three points are collinear.
     *
     * @param p1 The first point.
     * @param p2 The second point (middle point).
     * @param p3 The third point.
     * @return True if the points are collinear, false otherwise.
     */
    public static boolean areCollinear(Vec3d p1, Vec3d p2, Vec3d p3) {
        // Compute vectors p1->p2 and p2->p3
        Vec3d v1 = p2.subtract(p1);
        Vec3d v2 = p3.subtract(p2);

        // Calculate the cross product of the two vectors
        Vec3d crossProduct = v1.crossProduct(v2);

        // If the cross product is close to zero, the points are collinear
        double epsilon = 1e-6; // Tolerance for floating-point comparison
        return crossProduct.lengthSquared() < epsilon;
    }

    // https://stackoverflow.com/questions/4858264/find-the-distance-from-a-3d-point-to-a-line-segment
    public static double distanceBetweenPointAndLine(Vec3d point, Vec3d start, Vec3d end){
        Vec3d v = new Vec3d(point.x, point.y, point.z);
        Vec3d a = new Vec3d(start.x, start.y, start.z);
        Vec3d b = new Vec3d(end.x, end.y, end.z);

        Vec3d ab = b.subtract(a);
        Vec3d av = v.subtract(a);

        if (av.dotProduct(ab) <= 0.0) { // Point is lagging behind start of the segment, so perpendicular distance is not viable.
            return av.length();     // Use distance to start of segment instead.
        }

        Vec3d bv = v.subtract(b);

        if (bv.dotProduct(ab) <= 0.0) { // Point is advanced past the end of the segment, so perpendicular distance is not viable.
            return bv.length();     // Use distance to end of segment instead.
        }

        return (ab.crossProduct(av)).length() / ab.length() ;       // Perpendicular distance of point to segment.
    }
}
