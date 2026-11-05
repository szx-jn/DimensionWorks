package dev.szx.dimensionworks.mekstress.core;

/** Converts Create's float-based live stress values to conservative whole-SU allocations. */
public final class StressTransferMath {
    private static final float TWO_TO_63 = 0x1.0p63f;

    private StressTransferMath() {}

    /** Mirrors Create's float multiplication, then rounds down to whole SU for allocation. */
    public static long paidSu(float stressPerRpm, float rpm) {
        float value = stressPerRpm * rpm;
        if (!Float.isFinite(value) || value >= TWO_TO_63) return Long.MAX_VALUE;
        return Math.max(0L, (long) Math.floor(value));
    }

    /** Returns a float source capacity whose actual float stress product stays within the SU budget. */
    public static float capacityFor(long su, float rpm) {
        if (su <= 0 || !Float.isFinite(rpm) || rpm <= 0) return 0;
        float capacity = (float) (su / (double) rpm);
        if (!Float.isFinite(capacity)) capacity = Float.MAX_VALUE;
        while (capacity > 0 && exceeds(capacity, rpm, su)) capacity = Math.nextDown(capacity);
        return capacity;
    }

    private static boolean exceeds(float capacity, float rpm, long su) {
        float used = capacity * rpm;
        if (!Float.isFinite(used) || used >= TWO_TO_63) return true;
        return Math.ceil(used) > su;
    }
}
