package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MemoryCardMigrationTest {

    @Test
    void mapsEveryLegacyIdToItsBalancedArchetype() {
        for (int tier = 1; tier <= 5; tier++) {
            MemoryCardMigration.LegacyCardMapping mapping = MemoryCardMigration
                .legacyMapping("memory_card_ddr" + tier)
                .orElseThrow();

            assertEquals(MemoryTier.byIndex(tier), mapping.tier());
            assertEquals(MemoryCardType.BALANCED, mapping.type());
            assertEquals(
                "memory_card_ddr" + tier + "_balanced",
                "memory_card_ddr" + mapping.tier().index() + "_" + mapping.type().id());
        }
    }

    @Test
    void ignoresUnknownAndLegacyDriveIds() {
        assertTrue(MemoryCardMigration.legacyMapping(null).isEmpty());
        assertTrue(MemoryCardMigration.legacyMapping("memory_card_ddr6").isEmpty());
        assertTrue(MemoryCardMigration.legacyMapping("memory_drive_ddr1").isEmpty());
        assertTrue(MemoryCardMigration.legacyMapping("memory_card_ddr1_balanced").isEmpty());
    }
}
