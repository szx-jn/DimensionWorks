package dev.szx.dimensionworks.createculling;

/** Pure geometry: every positive result must cover the entire target, never samples of it. */
public final class OcclusionProof {
    private static final double EPSILON = 1.0e-7;
    private OcclusionProof() {}

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double min(int axis) { return axis == 0 ? minX : axis == 1 ? minY : minZ; }
        double max(int axis) { return axis == 0 ? maxX : axis == 1 ? maxY : maxZ; }
        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
        boolean usable() {
            for (int axis = 0; axis < 3; axis++) {
                if (!Double.isFinite(min(axis)) || !Double.isFinite(max(axis))
                    || max(axis) <= min(axis) || max(axis) - min(axis) > 8) return false;
            }
            return true;
        }
    }
    @FunctionalInterface
    public interface Solid { boolean at(int x, int y, int z); }

    public static boolean hidden(Box box, double x, double y, double z, Solid solid) {
        if (!box.usable() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
            || box.contains(x, y, z)) return false;
        boolean insideEnvelope = x >= Math.floor(box.minX) && x <= Math.ceil(box.maxX)
            && y >= Math.floor(box.minY) && y <= Math.ceil(box.maxY)
            && z >= Math.floor(box.minZ) && z <= Math.ceil(box.maxZ);
        if (!insideEnvelope && enclosed(box, solid)) return true;
        double[] camera = {x, y, z};
        for (int axis = 0; axis < 3; axis++) {
            double eye = camera[axis];
            boolean positive = eye > box.max(axis);
            if (!positive && eye >= box.min(axis)) continue;
            // All rays must cross this plane strictly between the camera and target.
            int first = positive ? (int) Math.ceil(box.max(axis)) : (int) Math.floor(box.min(axis)) - 1;
            int step = positive ? 1 : -1;
            int distance = (int) Math.abs(eye - (positive ? box.max(axis) : box.min(axis)));
            for (int i = 0; i < Math.min(distance, 24); i++) {
                // Nearby covers; then spread the remaining probes over the full distance.
                int offset = i < 8 ? i : 8 + (i - 8) * Math.max(1, (distance - 8) / 16);
                int slice = first + step * offset;
                double plane = slice + 0.5;
                if (positive ? plane >= eye : plane <= eye) continue;
                if (coveredPlane(box, camera, axis, slice, solid)) return true;
            }
        }
        return false;
    }

    private static boolean enclosed(Box box, Solid solid) {
        int[] low = new int[3], high = new int[3];
        for (int a = 0; a < 3; a++) {
            low[a] = (int) Math.floor(box.min(a));
            high[a] = (int) Math.ceil(box.max(a)) - 1;
        }
        for (int a = 0; a < 3; a++) {
            int b = (a + 1) % 3, c = (a + 2) % 3;
            for (int u = low[b]; u <= high[b]; u++) {
                for (int v = low[c]; v <= high[c]; v++) {
                    if (!at(solid, a, low[a] - 1, u, v) || !at(solid, a, high[a] + 1, u, v)) return false;
                }
            }
        }
        return true;
    }

    private static boolean coveredPlane(Box box, double[] camera, int a, int slice, Solid solid) {
        int b = (a + 1) % 3, c = (a + 2) % 3;
        double minB = Double.POSITIVE_INFINITY, maxB = Double.NEGATIVE_INFINITY;
        double minC = Double.POSITIVE_INFINITY, maxC = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            double depth = (corner & 1) == 0 ? box.min(a) : box.max(a);
            double scale = (slice + 0.5 - camera[a]) / (depth - camera[a]);
            double u = camera[b] + scale * (((corner & 2) == 0 ? box.min(b) : box.max(b)) - camera[b]);
            double v = camera[c] + scale * (((corner & 4) == 0 ? box.min(c) : box.max(c)) - camera[c]);
            minB = Math.min(minB, u); maxB = Math.max(maxB, u);
            minC = Math.min(minC, v); maxC = Math.max(maxC, v);
        }
        int fromB = (int) Math.floor(minB - EPSILON), toB = (int) Math.floor(maxB + EPSILON);
        int fromC = (int) Math.floor(minC - EPSILON), toC = (int) Math.floor(maxC + EPSILON);
        if ((long) (toB - fromB + 1) * (toC - fromC + 1) > 256) return false;
        for (int u = fromB; u <= toB; u++) {
            for (int v = fromC; v <= toC; v++) {
                if (!at(solid, a, slice, u, v)) return false;
            }
        }
        return true;
    }

    private static boolean at(Solid solid, int axis, int depth, int u, int v) {
        return switch (axis) {
            case 0 -> solid.at(depth, u, v);
            case 1 -> solid.at(v, depth, u);
            default -> solid.at(u, v, depth);
        };
    }
}
