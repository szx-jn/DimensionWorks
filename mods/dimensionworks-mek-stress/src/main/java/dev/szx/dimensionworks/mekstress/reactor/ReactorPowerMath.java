package dev.szx.dimensionworks.mekstress.reactor;

/** Pure reactor conversion rules; never treats SU as a stored FE balance. */
public final class ReactorPowerMath {
    public static final float OUTPUT_RPM = 256.0F;

    private ReactorPowerMath() {
    }

    /** Create multiplies the source's base capacity by its generated RPM. */
    public static float stressPerRpm(double generatedFePerTick, int connectedPorts) {
        if (connectedPorts <= 0 || !Double.isFinite(generatedFePerTick) || generatedFePerTick <= 0) {
            return 0.0F;
        }
        return dev.szx.dimensionworks.mekstress.core.EnergyPowerMath.coefficient(
            dev.szx.dimensionworks.mekstress.core.EnergyPowerMath.su(generatedFePerTick), OUTPUT_RPM, connectedPorts);
    }

    /** Replace this tick's exported FE with a drain of the internal saturation. */
    public static long drainGenerated(double generatedFePerTick, long availableSaturation) {
        if (!Double.isFinite(generatedFePerTick) || generatedFePerTick <= 0 || availableSaturation <= 0) {
            return 0L;
        }
        return Math.min(availableSaturation, (long) Math.floor(generatedFePerTick));
    }
}
