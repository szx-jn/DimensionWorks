package dev.szx.dimensionworks.mekstress.core;

/** Saturating additive RPM contributions. Cards provide no mechanical stress capacity. */
public final class MemoryDriveMath {
    private MemoryDriveMath() {}
    public static long scaleByPowerOfTwo(long value, int exponent) {
        if (value <= 0) return 0;
        int bounded = Math.max(-16, Math.min(16, exponent));
        if (bounded < 0) return value >> -bounded;
        return value > (Long.MAX_VALUE >> bounded) ? Long.MAX_VALUE : value << bounded;
    }
}
