package dev.szx.dimensionworks.mekstress.core;

/** Fixed mechanical load charged by a running Mekanism machine. */
public final class MachinePowerMath {
    public static final long SU_PER_RPM = 8L;

    private MachinePowerMath() {
    }

    public static long suForRpm(float rpm) {
        if (!Float.isFinite(rpm) || rpm <= 0.0F) {
            return 0L;
        }
        double value = Math.ceil((double) rpm * SU_PER_RPM);
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) value;
    }

    public static float stressPerRpm(float effectiveScale) {
        if (!Float.isFinite(effectiveScale) || effectiveScale <= 0.0F) {
            return 0.0F;
        }
        return SU_PER_RPM * Math.min(1.0F, effectiveScale);
    }
}
