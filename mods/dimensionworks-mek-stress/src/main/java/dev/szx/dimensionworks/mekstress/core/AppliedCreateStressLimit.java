package dev.szx.dimensionworks.mekstress.core;

/** Compatibility limits for Applied Create's user-configurable stress supply. */
public final class AppliedCreateStressLimit {

    private static final float STRESS_PER_RPM = 8.0F;

    private AppliedCreateStressLimit() {
    }

    public static float effectiveGearboxMaximum(float configuredMaximum, int outputRpm) {
        float configuredLimit = Math.max(0.0F, configuredMaximum);
        float outputLimit = (float) Math.abs((long) outputRpm) * STRESS_PER_RPM;
        return Math.max(configuredLimit, outputLimit);
    }
}
