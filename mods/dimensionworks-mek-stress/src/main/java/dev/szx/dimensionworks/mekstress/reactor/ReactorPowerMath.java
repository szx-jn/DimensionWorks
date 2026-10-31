package dev.szx.dimensionworks.mekstress.reactor;

/** Pure reactor conversion rules; never treats SU as a stored FE balance. */
public final class ReactorPowerMath {
    public static final float OUTPUT_RPM = 256.0F;
    public static final float INPUT_STRESS_PER_RPM = 256.0F;
    private static final double SU_PER_FE = 2.0D;

    private ReactorPowerMath() {
    }

    /** Create multiplies the source's base capacity by its generated RPM. */
    public static float stressPerRpm(double generatedFePerTick, int connectedPorts) {
        if (connectedPorts <= 0 || !Double.isFinite(generatedFePerTick) || generatedFePerTick <= 0) {
            return 0.0F;
        }
        double perPort = generatedFePerTick * SU_PER_FE / (OUTPUT_RPM * connectedPorts);
        return (float) Math.min(Float.MAX_VALUE / 1024.0D, perPort);
    }

    /** Replace this tick's exported FE with a drain of the internal saturation. */
    public static long drainGenerated(double generatedFePerTick, long availableSaturation) {
        if (!Double.isFinite(generatedFePerTick) || generatedFePerTick <= 0 || availableSaturation <= 0) {
            return 0L;
        }
        return Math.min(availableSaturation, (long) Math.floor(generatedFePerTick));
    }
}
