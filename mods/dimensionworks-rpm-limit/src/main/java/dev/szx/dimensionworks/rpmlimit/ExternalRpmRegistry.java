package dev.szx.dimensionworks.rpmlimit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks external RPM limit modifiers per player (e.g. Gear Heart stage limits). */
final class ExternalRpmRegistry {
    private static final String GEAR_HEART_RPM_SOURCE = "gear_heart:rpm";
    private final Map<UUID, Map<String, Integer>> externalLimits = new ConcurrentHashMap<>();

    void clear() {
        externalLimits.clear();
    }

    boolean put(UUID owner, String source, int sanitizedRpm) {
        Integer previous = limitsFor(owner).put(source, sanitizedRpm);
        return previous == null || previous != sanitizedRpm;
    }

    boolean remove(UUID owner, String source) {
        return limitsFor(owner).remove(source) != null;
    }

    int applyModifiers(UUID owner, int baseEffective, boolean commandOverride) {
        int effective = baseEffective;
        for (Map.Entry<String, Integer> entry : limitsFor(owner).entrySet()) {
            if (commandOverride && GEAR_HEART_RPM_SOURCE.equals(entry.getKey())) {
                continue;
            }
            effective = Math.min(effective, PlayerRpmStore.sanitize(entry.getValue()));
        }
        return effective;
    }

    private Map<String, Integer> limitsFor(UUID owner) {
        return externalLimits.computeIfAbsent(owner, ignored -> new ConcurrentHashMap<>());
    }
}
