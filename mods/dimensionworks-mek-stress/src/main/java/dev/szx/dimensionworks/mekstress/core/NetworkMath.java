package dev.szx.dimensionworks.mekstress.core;

/** Pure math for ME network congestion and production scaling. */
public final class NetworkMath {
    public static final double MIN_RPM_SCALE = 0.6D;

    private NetworkMath() {
    }

    public static double clampRatio(double ratio) {
        if (!Double.isFinite(ratio) || ratio <= 0.0D) {
            return 0.0D;
        }
        return Math.min(1.0D, ratio);
    }

    public static double stockRatio(long storedSu, long demandSuPerTick) {
        if (demandSuPerTick <= 0L) {
            return 1.0D;
        }
        if (storedSu <= 0L) {
            return 0.0D;
        }
        return clampRatio((double) storedSu / (double) demandSuPerTick);
    }

    /**
     * Normalized relation used by the calibration tests. The math treats the
     * two inputs as a capacity/load pair and reports the smaller side as the
     * utilization ratio.
     */
    public static double bandwidthRatio(long a, long b) {
        if (a <= 0L || b <= 0L) {
            return 0.0D;
        }
        return clampRatio((double) Math.min(a, b) / (double) Math.max(a, b));
    }

    /** Actual q_bw: min(1, bandwidth / demand). */
    public static double bandwidthQ(long bandwidthRpm, long demandRpm) {
        if (demandRpm <= 0L) {
            return 1.0D;
        }
        return clampRatio((double) bandwidthRpm / (double) demandRpm);
    }

    /** RPM coverage is throttled proportionally and cut off below 60%. */
    public static double rpmQ(long bandwidthRpm, long demandRpm) {
        if (bandwidthRpm <= 0L || demandRpm <= 0L) {
            return 0.0D;
        }
        double ratio = (double) bandwidthRpm / (double) demandRpm;
        if (ratio < MIN_RPM_SCALE) {
            return 0.0D;
        }
        return Math.min(1.0D, ratio);
    }

    public static double finalQ(double stockQ, double bandwidthQ) {
        return Math.min(clampRatio(stockQ), clampRatio(bandwidthQ));
    }

    /** Mechanical power is available only when both SU stock and RPM bandwidth exist. */
    public static double availableQ(long storedSu, long demandSuPerTick, long bandwidthRpm, long demandRpm) {
        if (storedSu <= 0L || demandSuPerTick <= 0L || bandwidthRpm <= 0L || demandRpm <= 0L) {
            return 0.0D;
        }
        return finalQ(
            stockRatio(storedSu, demandSuPerTick),
            rpmQ(bandwidthRpm, demandRpm));
    }

    public static double efficiency(double q, double exponent) {
        return Math.pow(clampRatio(q), exponent);
    }

    public static double productionRate(double effectiveRpmRatio, double q, double exponent) {
        double rpmRatio = Math.max(0.0D, Math.min(1.0D, effectiveRpmRatio));
        return rpmRatio * Math.pow(clampRatio(q), exponent + 1.0D);
    }

    public static long actualRpm(long effectiveRpm, double q) {
        return Math.round(Math.max(0L, effectiveRpm) * clampRatio(q));
    }
}
