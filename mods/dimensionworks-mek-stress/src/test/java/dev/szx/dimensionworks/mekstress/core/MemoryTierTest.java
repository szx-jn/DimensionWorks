package dev.szx.dimensionworks.mekstress.core;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class MemoryTierTest {
    @Test void tiersControlSlotsAndSmallBaseBandwidth() {
        int[] slots = {4,8,16,32,64};
        for (MemoryTier tier : MemoryTier.values()) {
            assertEquals(slots[tier.index()-1], tier.slotsPerDrive());
            assertEquals(256L << (tier.index()-1), tier.defaultDriveBandwidthRpm());
            assertEquals(tier, MemoryTier.byIndex(tier.index()));
        }
        assertThrows(IllegalArgumentException.class, () -> MemoryTier.byIndex(0));
    }
}
