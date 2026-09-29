package dev.szx.dimensionworks.mekstress.core;

/**
 * Deterministic fractional work scheduler. It uses the absolute server tick as
 * the accumulator, so callers do not have to mutate state when a tile entity
 * is unloaded and loaded again.
 */
public final class WorkScheduler {
    public int workCount(double rate, long tick) {
        return callsForRate(rate, tick);
    }

    public static int callsForRate(double rate, long tick) {
        if (!Double.isFinite(rate) || rate <= 0.0D || tick < 0L) {
            return 0;
        }
        if (rate < 1.0D) {
            double next = rate * (double) (tick + 1L);
            double current = rate * (double) tick;
            return Math.max(0, ceilToInt(next) - ceilToInt(current));
        }
        long next = saturatedProduct(rate, tick + 1L);
        long current = saturatedProduct(rate, tick);
        long calls = next - current;
        return calls > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, calls);
    }

    private static int ceilToInt(double value) {
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= 0.0D) {
            return 0;
        }
        return (int) Math.ceil(value);
    }

    private static long saturatedProduct(double rate, long tick) {
        double value = rate * (double) tick;
        if (value >= (double) Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        if (value <= 0.0D) {
            return 0L;
        }
        return (long) Math.floor(value);
    }
}
