package dev.szx.dimensionworks.mekstress.core;

/** Pure stress and SU conservation math for the ME Gearbox export bridge. */
public final class StressTransfer {
    private StressTransfer() {
    }

    public static double fullSpeedLoad(Iterable<Float> stressPerRpm, int configuredRpm) {
        if (stressPerRpm == null || configuredRpm <= 0) {
            return 0.0D;
        }
        double load = 0.0D;
        for (Float value : stressPerRpm) {
            if (value != null && Float.isFinite(value) && value > 0.0F) {
                load += value.doubleValue() * configuredRpm;
            }
        }
        return load;
    }

    public static int outputRpm(int configuredRpm, double q) {
        if (configuredRpm <= 0 || !Double.isFinite(q) || q <= 0.0D) {
            return 0;
        }
        double clampedQ = Math.min(1.0D, q);
        return (int) Math.min(Integer.MAX_VALUE, Math.round(configuredRpm * clampedQ));
    }

    public static long chargedSu(long requestedSu, long availableSu) {
        if (requestedSu <= 0L || availableSu <= 0L) {
            return 0L;
        }
        return Math.min(requestedSu, availableSu);
    }

    public static float capacityPerRpm(long chargedSu, int outputRpm) {
        if (chargedSu <= 0L || outputRpm <= 0) {
            return 0.0F;
        }
        return (float) ((double) chargedSu / outputRpm);
    }
}
