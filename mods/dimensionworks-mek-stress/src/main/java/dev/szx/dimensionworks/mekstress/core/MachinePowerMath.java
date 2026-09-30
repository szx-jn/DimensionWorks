package dev.szx.dimensionworks.mekstress.core;

/** Fixed mechanical load charged by a running Mekanism machine or idle gearbox. */
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

    /**
     * Keeps an idle export gearbox rotating while preserving the documented
     * RPM x 8 load for a single standard machine.
     */
    public static double gearboxLoad(double downstreamLoad, int effectiveRpm) {
        if (effectiveRpm <= 0) {
            return 0.0D;
        }
        double idleLoad = (double) SU_PER_RPM * effectiveRpm;
        return Double.isFinite(downstreamLoad) && downstreamLoad > 0.0D
            ? Math.max(idleLoad, downstreamLoad)
            : idleLoad;
    }
}
