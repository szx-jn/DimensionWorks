package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NetworkMathTest {

    @Test
    void computesTheDdr5TerminalCalibration() {
        long demandRpm = 10L * 4096L + 100L * 2048L;
        assertEquals(245_760L, demandRpm);
        assertEquals(1_966_080L, demandRpm * 8L);
        assertEquals(0.9375D, NetworkMath.clampRatio(NetworkMath.bandwidthRatio(262_144L, demandRpm)), 1.0E-9D);
    }

    @Test
    void usesTheLowerOfInventoryAndBandwidth() {
        assertEquals(0.5D, NetworkMath.finalQ(NetworkMath.stockRatio(100L, 200L), 1.0D), 1.0E-9D);
        assertEquals(0.25D, NetworkMath.finalQ(1.0D, NetworkMath.bandwidthRatio(25L, 100L)), 1.0E-9D);
    }

    @Test
    void appliesCongestionEfficiencyToProduction() {
        assertEquals(0.4204482D, NetworkMath.efficiency(0.5D, 1.25D), 1.0E-7D);
        assertEquals(0.2102241D, NetworkMath.productionRate(1.0D, 0.5D, 1.25D), 1.0E-7D);
    }

    @Test
    void handlesZeroInventoryAndExactCapacity() {
        assertEquals(0.0D, NetworkMath.stockRatio(0L, 100L), 1.0E-9D);
        assertEquals(1.0D, NetworkMath.stockRatio(100L, 100L), 1.0E-9D);
        assertEquals(0.0D, NetworkMath.bandwidthQ(0L, 100L), 1.0E-9D);
        assertEquals(1.0D, NetworkMath.bandwidthQ(100L, 100L), 1.0E-9D);
    }

    @Test
    void reportsOneWhenThereIsNoDemand() {
        assertEquals(1.0D, NetworkMath.stockRatio(0L, 0L), 1.0E-9D);
        assertEquals(1.0D, NetworkMath.bandwidthQ(0L, 0L), 1.0E-9D);
        assertEquals(1.0D, NetworkMath.finalQ(1.0D, 1.0D), 1.0E-9D);
    }

    @Test
    void requiresBothSuAndRpmForAvailableMechanicalPower() {
        assertEquals(0.0D, NetworkMath.availableQ(0L, 100L, 1_024L, 256L), 1.0E-9D);
        assertEquals(0.0D, NetworkMath.availableQ(100L, 100L, 0L, 256L), 1.0E-9D);
        assertEquals(0.0D, NetworkMath.availableQ(100L, 0L, 1_024L, 256L), 1.0E-9D);
        assertEquals(0.0D, NetworkMath.availableQ(100L, 100L, 1_024L, 0L), 1.0E-9D);
        assertEquals(0.5D, NetworkMath.availableQ(100L, 200L, 1_024L, 512L), 1.0E-9D);
        assertEquals(1.0D, NetworkMath.availableQ(200L, 200L, 1_024L, 512L), 1.0E-9D);
    }
}
