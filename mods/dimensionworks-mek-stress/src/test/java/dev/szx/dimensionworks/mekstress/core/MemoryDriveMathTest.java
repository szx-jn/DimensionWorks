package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryDriveMathTest {

    @Test
    void emptyDriveKeepsItsBaseBandwidth() {
        assertEquals(131_072L, MemoryDriveMath.effectiveBandwidthRpm(131_072L, 64, 0L));
    }

    @Test
    void fullHomogeneousDrivesFollowTheArchetypeFactors() {
        assertFullDrive(MemoryCardType.DEFECTIVE, 262_144L, 32_768L);
        assertFullDrive(MemoryCardType.ECONOMY, 524_288L, 65_536L);
        assertFullDrive(MemoryCardType.BALANCED, 1_048_576L, 131_072L);
        assertFullDrive(MemoryCardType.HIGH_SPEED, 524_288L, 262_144L);
        assertFullDrive(MemoryCardType.HIGH_STORAGE, 2_097_152L, 65_536L);
        assertFullDrive(MemoryCardType.FINAL, 4_194_304L, 524_288L);
    }

    @Test
    void mixedCardsContributeLinearlyPerSlot() {
        long delta = MemoryDriveMath.bandwidthDeltaRpm(131_072L, -1, 64)
            + MemoryDriveMath.bandwidthDeltaRpm(131_072L, 1, 64);
        assertEquals(132_096L, MemoryDriveMath.effectiveBandwidthRpm(131_072L, 64, delta));
    }

    @Test
    void capacityScalesExactlyByPowerOfTwo() {
        assertEquals(4_096L, MemoryDriveMath.cardCapacitySu(16_384L, -2));
        assertEquals(8_192L, MemoryDriveMath.cardCapacitySu(16_384L, -1));
        assertEquals(16_384L, MemoryDriveMath.cardCapacitySu(16_384L, 0));
        assertEquals(32_768L, MemoryDriveMath.cardCapacitySu(16_384L, 1));
        assertEquals(65_536L, MemoryDriveMath.cardCapacitySu(16_384L, 2));
    }

    private static void assertFullDrive(MemoryCardType type, long expectedCapacity, long expectedBandwidth) {
        List<Integer> speedExponents = java.util.Collections.nCopies(64, type.defaultSpeedExponent());
        long delta = speedExponents.stream()
            .mapToLong(exponent -> MemoryDriveMath.bandwidthDeltaRpm(131_072L, exponent, 64))
            .sum();
        assertEquals(expectedCapacity, MemoryDriveMath.cardCapacitySu(16_384L, type.defaultStorageExponent()) * 64L);
        assertEquals(expectedBandwidth, MemoryDriveMath.effectiveBandwidthRpm(131_072L, 64, delta));
    }
}
