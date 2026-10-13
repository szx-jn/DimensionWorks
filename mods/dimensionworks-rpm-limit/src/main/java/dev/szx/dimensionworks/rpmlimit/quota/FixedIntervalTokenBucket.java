package dev.szx.dimensionworks.rpmlimit.quota;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deterministic per-player rate quota. Tokens refill from zero each time and are capped at the
 * smaller of one tick's allowance and the current request, so idle time never banks arbitrary
 * excess while low integer rates can still complete one whole operation.
 */
public final class FixedIntervalTokenBucket {
    private static final long TICKS_PER_SECOND = 20L;
    private static final double EPSILON = 1.0E-9D;
    private static final Map<Key, State> STATES = new ConcurrentHashMap<>();

    public boolean tryTake(UUID owner, GearHeartQuotaType type,
                           double amount, double perSecond, long tick) {
        if (owner == null || type == null || !Double.isFinite(amount) || amount < 0.0D) {
            return false;
        }
        if (amount == 0.0D) {
            return true;
        }
        if (!Double.isFinite(perSecond) || perSecond <= 0.0D || tick < 0L) {
            return false;
        }

        double perTick = perSecond / (double) TICKS_PER_SECOND;
        double ceiling = Math.max(perTick, amount);
        Key key = new Key(owner, type);
        AtomicBoolean accepted = new AtomicBoolean(false);
        STATES.compute(key, (ignored, existing) -> {
            State state;
            if (existing == null || existing.lastTick > tick) {
                state = new State(tick, Math.min(ceiling, perTick));
            } else if (existing.lastTick < tick) {
                long elapsed = Math.min(tick - existing.lastTick, TICKS_PER_SECOND);
                double replenished = Math.min(
                    ceiling, existing.tokens + perTick * (double) elapsed);
                state = new State(tick, replenished);
            } else {
                state = new State(tick, Math.min(existing.tokens, ceiling));
            }

            if (state.tokens + EPSILON < amount) {
                return state;
            }
            accepted.set(true);
            return new State(tick, Math.max(0.0D, state.tokens - amount));
        });
        return accepted.get();
    }

    public void clearOwner(UUID owner) {
        if (owner != null) {
            STATES.keySet().removeIf(key -> key.owner().equals(owner));
        }
    }

    private record Key(UUID owner, GearHeartQuotaType type) {
    }

    private record State(long lastTick, double tokens) {
    }
}
