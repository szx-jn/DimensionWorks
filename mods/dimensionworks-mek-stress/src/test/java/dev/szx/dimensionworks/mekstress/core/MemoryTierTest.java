package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemoryTierTest {

    @Test
    void exposesTheDesignedCardAndBandwidthTables() {
        assertEquals(4, MemoryTier.DDR1.slotsPerDrive());
        assertEquals(64, MemoryTier.DDR5.slotsPerDrive());
        assertEquals(8192L, MemoryTier.DDR1.cardCapacitySu());
        assertEquals(9216L, MemoryTier.DDR2.cardCapacitySu());
        assertEquals(10240L, MemoryTier.DDR3.cardCapacitySu());
        assertEquals(12288L, MemoryTier.DDR4.cardCapacitySu());
        assertEquals(16384L, MemoryTier.DDR5.cardCapacitySu());
        assertEquals(8192L, MemoryTier.DDR1.driveBandwidthRpm());
        assertEquals(131072L, MemoryTier.DDR5.driveBandwidthRpm());
    }

    @Test
    void fullNetworksMatchTheCalibrationTable() {
        assertEquals(65_536L, MemoryTier.DDR1.totalCapacitySu());
        assertEquals(147_456L, MemoryTier.DDR2.totalCapacitySu());
        assertEquals(327_680L, MemoryTier.DDR3.totalCapacitySu());
        assertEquals(786_432L, MemoryTier.DDR4.totalCapacitySu());
        assertEquals(2_097_152L, MemoryTier.DDR5.totalCapacitySu());
        assertEquals(16_384L, MemoryTier.DDR1.totalBandwidthRpm());
        assertEquals(262_144L, MemoryTier.DDR5.totalBandwidthRpm());
    }
}
