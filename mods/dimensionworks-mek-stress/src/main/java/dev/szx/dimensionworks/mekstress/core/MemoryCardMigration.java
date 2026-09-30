package dev.szx.dimensionworks.mekstress.core;

import java.util.Optional;

/** Pure mapping from retired generic memory-card IDs to the balanced archetype. */
public final class MemoryCardMigration {
    private static final String LEGACY_PREFIX = "memory_card_ddr";

    private MemoryCardMigration() {
    }

    public static Optional<LegacyCardMapping> legacyMapping(String path) {
        if (path == null || !path.startsWith(LEGACY_PREFIX)) {
            return Optional.empty();
        }
        for (MemoryTier tier : MemoryTier.values()) {
            if (path.equals(LEGACY_PREFIX + tier.index())) {
                return Optional.of(new LegacyCardMapping(tier, MemoryCardType.BALANCED));
            }
        }
        return Optional.empty();
    }

    public record LegacyCardMapping(MemoryTier tier, MemoryCardType type) {
    }
}
