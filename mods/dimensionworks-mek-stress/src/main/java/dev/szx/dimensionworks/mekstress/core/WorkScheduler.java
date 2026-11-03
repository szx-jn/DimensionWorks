package dev.szx.dimensionworks.mekstress.core;

/**
 * Deterministic fractional work scheduler. It advances from local machine-tick
 * state so unloaded, disabled, or speed-changed machines never replay missed work.
 */
public final class WorkScheduler {
    public record Advance(long tick, double rate, long localTick, int calls) {
    }

    public int workCount(double rate, long tick) {
        return callsForRate(rate, tick);
    }

    /**
     * Advances one machine tick while discarding history after a gap or rate change.
     *
     * <p>Absolute game time is intentionally not used as an accumulator. A machine that was
     * unloaded, disabled, or running at a different speed must not replay the missing work.
     */
    public static Advance advance(double rate, long tick, Advance previous) {
        if (!Double.isFinite(rate) || rate <= 0.0D || tick < 0L) {
            return new Advance(tick, rate, 0L, 0);
        }

        boolean contiguous = previous != null
            && previous.tick() == tick - 1L
            && Double.compare(previous.rate(), rate) == 0;
        long localTick = contiguous ? previous.localTick() + 1L : 0L;
        return new Advance(tick, rate, localTick, callsForRate(rate, localTick));
    }

    public static int callsForRate(double rate, long tick) {
        if (!Double.isFinite(rate) || rate <= 0.0D || tick < 0L) {
            return 0;
        }
        // Floor both boundaries: partial mechanical work must be earned before a native tick.
        // Ceil would grant a free first tick after every unload, reconnect, or rate change.
        long next = saturatedProduct(rate, tick + 1L);
        long current = saturatedProduct(rate, tick);
        long calls = next - current;
        return calls > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, calls);
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
