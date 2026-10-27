package dev.szx.dimensionworks.mekstress.core;

/** Fixed mechanical load charged by a running Mekanism machine or idle gearbox. */
public final class MachinePowerMath {
    public static final long SU_PER_RPM = 256L;
    private static final long GEARBOX_SU_PER_RPM = 8L;

    private MachinePowerMath() {
    }

    public static long suForRpm(float rpm) {
        float speed = Math.abs(rpm);
        if (!Float.isFinite(rpm) || speed == 0.0F) {
            return 0L;
        }
        double value = Math.ceil((double) speed * SU_PER_RPM);
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) value;
    }

    public static float stressPerRpm(float effectiveScale) {
        if (!Float.isFinite(effectiveScale) || effectiveScale <= 0.0F) {
            return 0.0F;
        }
        return SU_PER_RPM * Math.min(1.0F, effectiveScale);
    }

    /** Keeps the propagated speed a direct MEK route consumes before its own tier checks. */
    public static float directRpm(float requestedRpm, int maximumRpm) {
        float speed = Math.abs(requestedRpm);
        if (!Float.isFinite(requestedRpm) || speed == 0.0F || maximumRpm <= 0) {
            return 0.0F;
        }
        return Math.min(speed, (float) maximumRpm);
    }

    /** Converts a per-RPM stress coefficient into Create's speed-scaled load. */
    public static double actualStress(float stressPerRpm, float rpm) {
        float speed = Math.abs(rpm);
        if (!Float.isFinite(stressPerRpm) || stressPerRpm <= 0.0F || !Float.isFinite(rpm) || speed == 0.0F) {
            return 0.0D;
        }
        return (double) stressPerRpm * (double) speed;
    }

    /** Work scales with supplied RPM and caps at full speed once the target is met. */
    public static double workRate(int actualRpm, int targetRpm) {
        if (actualRpm <= 0 || targetRpm <= 0) {
            return 0.0D;
        }
        return Math.min(1.0D, (double) actualRpm / (double) targetRpm);
    }

    /** Production output scales above the target RPM and never shrinks below it. */
    public static double productionMultiplier(int actualRpm, int targetRpm) {
        if (actualRpm <= 0 || targetRpm <= 0) {
            return 0.0D;
        }
        return Math.max(1.0D, (double) actualRpm / (double) targetRpm);
    }

    /** End-to-end throughput after combining tick frequency and per-operation output. */
    public static double throughputMultiplier(int actualRpm, int targetRpm) {
        if (actualRpm <= 0 || targetRpm <= 0) {
            return 0.0D;
        }
        return (double) actualRpm / (double) targetRpm;
    }

    /** Applies a production multiplier to each recipe result, rounding fractional item amounts up. */
    public static long scaledOutput(long baseAmount, int operations, double multiplier) {
        if (baseAmount <= 0L || operations <= 0 || !Double.isFinite(multiplier) || multiplier <= 0.0D) {
            return 0L;
        }
        double scaled = (double) baseAmount * (double) operations * multiplier;
        if (!Double.isFinite(scaled) || scaled <= 0.0D) {
            return 0L;
        }
        return scaled >= (double) Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.ceil(scaled);
    }

    /** Finds the largest operation count whose scaled output still fits the available room. */
    public static int maxOutputOperations(long baseAmount, long availableRoom, double multiplier) {
        if (baseAmount <= 0L || availableRoom <= 0L || !Double.isFinite(multiplier) || multiplier <= 0.0D) {
            return 0;
        }
        long operations = (long) Math.floor((double) availableRoom / ((double) baseAmount * multiplier));
        while (operations < Integer.MAX_VALUE
            && scaledOutput(baseAmount, saturatedInt(operations + 1L), multiplier) <= availableRoom) {
            operations++;
        }
        while (operations > 0L && scaledOutput(baseAmount, saturatedInt(operations), multiplier) > availableRoom) {
            operations--;
        }
        return saturatedInt(operations);
    }

    private static int saturatedInt(long value) {
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= 0L) {
            return 0;
        }
        return (int) value;
    }

    /**
     * Keeps an idle export gearbox rotating while preserving the documented
     * RPM x 8 idle load for the ME gearbox.
     */
    public static double gearboxLoad(double downstreamLoad, int effectiveRpm) {
        if (effectiveRpm <= 0) {
            return 0.0D;
        }
        double idleLoad = (double) GEARBOX_SU_PER_RPM * effectiveRpm;
        return Double.isFinite(downstreamLoad) && downstreamLoad > 0.0D
            ? Math.max(idleLoad, downstreamLoad)
            : idleLoad;
    }
}
