package dev.szx.dimensionworks.rpmlimit.quota;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
        return tryTakePlan(List.of(new QuotaRequest(owner, type, amount, perSecond, tick)));
    }

    public boolean tryTakeMany(Collection<UUID> owners, GearHeartQuotaType type,
                               double amount, double perSecond, long tick) {
        if (owners == null || owners.isEmpty()) {
            return true;
        }
        Collection<UUID> unique = new LinkedHashSet<>(owners);
        return tryTakePlan(unique.stream()
            .map(owner -> new QuotaRequest(owner, type, amount, perSecond, tick))
            .toList());
    }

    /**
     * Applies every request atomically. A rejected plan stores replenished-but-unspent states only.
     */
    public boolean tryTakePlan(Collection<QuotaRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return true;
        }

        synchronized (FixedIntervalTokenBucket.class) {
            Map<Key, State> rollback = new HashMap<>();
            Map<Key, State> working = new HashMap<>();
            boolean rejected = false;

            for (QuotaRequest request : requests) {
                if (!valid(request)) {
                    rejected = true;
                    continue;
                }

                Key key = new Key(request.owner(), request.type());
                State base = working.getOrDefault(key, STATES.get(key));
                Outcome outcome = advance(base, request.perSecond(), request.amount(), request.tick());
                rollback.putIfAbsent(key, outcome.before());
                working.put(key, outcome.after());
                if (!outcome.accepted()) {
                    rejected = true;
                }
            }

            Map<Key, State> result = rejected ? rollback : working;
            result.forEach(STATES::put);
            return !rejected;
        }
    }

    public void clearOwner(UUID owner) {
        if (owner != null) {
            STATES.keySet().removeIf(key -> key.owner().equals(owner));
        }
    }

    private static boolean valid(QuotaRequest request) {
        return request != null && request.owner() != null && request.type() != null
            && Double.isFinite(request.amount()) && request.amount() >= 0.0D
            && Double.isFinite(request.perSecond()) && request.tick() >= 0L
            && (request.amount() == 0.0D || request.perSecond() > 0.0D);
    }

    private static Outcome advance(State existing, double perSecond, double amount, long tick) {
        double perTick = perSecond / (double) TICKS_PER_SECOND;
        double ceiling = Math.max(perTick, amount);

        State before;
        if (existing == null || existing.lastTick() > tick) {
            before = new State(tick, Math.min(ceiling, perTick));
        } else if (existing.lastTick() < tick) {
            long elapsed = Math.min(tick - existing.lastTick(), TICKS_PER_SECOND);
            before = new State(tick, Math.min(
                ceiling, existing.tokens() + perTick * (double) elapsed));
        } else {
            before = new State(tick, Math.min(existing.tokens(), ceiling));
        }

        if (amount == 0.0D) {
            return new Outcome(before, before, true);
        }
        if (before.tokens() + EPSILON < amount) {
            return new Outcome(before, before, false);
        }
        State after = new State(tick, Math.max(0.0D, before.tokens() - amount));
        return new Outcome(before, after, true);
    }

    public record QuotaRequest(UUID owner, GearHeartQuotaType type,
                               double amount, double perSecond, long tick) {
    }

    private record Key(UUID owner, GearHeartQuotaType type) {
    }

    private record State(long lastTick, double tokens) {
    }

    private record Outcome(State before, State after, boolean accepted) {
    }
}
