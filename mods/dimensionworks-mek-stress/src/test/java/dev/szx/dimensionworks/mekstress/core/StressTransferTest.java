package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StressTransferTest {
    @Test
    void calculatesFullSpeedNetworkLoad() {
        assertEquals(768.0D, StressTransfer.fullSpeedLoad(List.of(2.0F, 1.0F), 256), 1.0E-9D);
    }

    @Test
    void throttlesOutputRpmByQ() {
        assertEquals(64, StressTransfer.outputRpm(256, 0.25D));
        assertEquals(0, StressTransfer.outputRpm(256, 0.0D));
        assertEquals(256, StressTransfer.outputRpm(256, 2.0D));
    }

    @Test
    void neverChargesMoreSuThanAvailable() {
        assertEquals(700L, StressTransfer.chargedSu(1_000L, 700L));
        assertEquals(0L, StressTransfer.chargedSu(1_000L, -1L));
    }

    @Test
    void advertisesOnlyChargedCapacityPerRpm() {
        assertEquals(10.9375F, StressTransfer.capacityPerRpm(700L, 64), 1.0E-6F);
        assertEquals(0.0F, StressTransfer.capacityPerRpm(700L, 0), 1.0E-6F);
        assertEquals(0.0F, StressTransfer.capacityPerRpm(0L, 64), 1.0E-6F);
    }

    @Test
    void requiresLoadSpeedAndChargedSuBeforeRunning() {
        assertEquals(true, StressTransfer.canRun(256, 768.0D, 256, 768L));
        assertEquals(false, StressTransfer.canRun(256, 0.0D, 256, 768L));
        assertEquals(false, StressTransfer.canRun(256, 768.0D, 0, 768L));
        assertEquals(false, StressTransfer.canRun(256, 768.0D, 256, 0L));
    }
}
