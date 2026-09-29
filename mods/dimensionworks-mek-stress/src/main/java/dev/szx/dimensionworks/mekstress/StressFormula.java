package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.rpmlimit.OverspeedCurve;

/** Pure conversion and speed helpers shared by mixins and tests. */
public final class StressFormula {

    public static final int DEFAULT_SATURATION_RPM = 512;

    private StressFormula() {
    }

    public static int clampRpm(int requested, int ownerLimit, int maximum) {
        int effectiveLimit = Math.min(Math.max(ownerLimit, 0), Math.max(maximum, 0));
        return Math.max(0, Math.min(requested, effectiveLimit));
    }

    public static long clampStress(long requested, long maximum) {
        return Math.max(0L, Math.min(requested, Math.max(maximum, 0L)));
    }

    public static long joulesToStress(double joules, double joulesPerStress) {
        if (!(joules > 0.0D) || !(joulesPerStress > 0.0D)) {
            return 0L;
        }
        double stress = Math.ceil(joules / joulesPerStress);
        return stress >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) stress;
    }

    public static long joulesToStressFloor(double joules, double joulesPerStress) {
        if (!(joules > 0.0D) || !(joulesPerStress > 0.0D)) {
            return 0L;
        }
        double stress = Math.floor(joules / joulesPerStress);
        return stress >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) stress;
    }

    public static double stressToJoules(long stress, double joulesPerStress) {
        if (stress <= 0L || !(joulesPerStress > 0.0D)) {
            return 0.0D;
        }
        double joules = stress * joulesPerStress;
        return Double.isFinite(joules) ? joules : Double.MAX_VALUE;
    }

    public static double multiplier(int requestedRpm, int saturationRpm, int cap) {
        return OverspeedCurve.multiplier(Math.abs(requestedRpm), saturationRpm, cap);
    }

    public static int effectiveTicks(int nativeTicks, double multiplier) {
        if (nativeTicks <= 1 || !(multiplier > 1.0D)) {
            return Math.max(1, nativeTicks);
        }
        return Math.max(1, (int) Math.ceil(nativeTicks / multiplier));
    }
}
