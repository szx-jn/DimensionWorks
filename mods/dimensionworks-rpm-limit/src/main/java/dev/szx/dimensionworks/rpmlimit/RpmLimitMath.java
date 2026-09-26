package dev.szx.dimensionworks.rpmlimit;

/** Pure speed-clamp helpers used by both kinetic hooks and their regression tests. */
public final class RpmLimitMath {

    private RpmLimitMath() {}

    public static float cap(float speed, int limit) {
        if (speed == 0)
            return 0;
        float absoluteLimit = Math.max(1, limit);
        return Math.abs(speed) > absoluteLimit ? Math.copySign(absoluteLimit, speed) : speed;
    }

    /**
     * Returns the smallest active limit. A non-positive value means that side of the transmission
     * has no owner, so it should not constrain the result.
     */
    public static int lowestLimit(int... limits) {
        int lowest = Integer.MAX_VALUE;
        for (int limit : limits) {
            if (limit > 0)
                lowest = Math.min(lowest, limit);
        }
        return lowest == Integer.MAX_VALUE ? RpmLimitConfig.MAX_RPM.get() : lowest;
    }
}
