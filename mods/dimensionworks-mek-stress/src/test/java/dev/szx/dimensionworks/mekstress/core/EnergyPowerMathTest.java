package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EnergyPowerMathTest {
    @Test void dividesNativeJBeforeApplyingFeRatio() {
        for (double joulesPerFe : new double[]{1, 2.5, 10}) {
            assertEquals(800, EnergyPowerMath.su(400 * joulesPerFe / joulesPerFe));
        }
        assertEquals(800, EnergyPowerMath.su(1000 / 2.5));
    }

    @Test void allPortsShareOneProductionAndSpeedOnlyChangesCoefficient() {
        for (int ports = 1; ports <= 16; ports++) {
            for (float rpm : new float[]{-256, 32, 128, 1024}) {
                double total = EnergyPowerMath.coefficient(EnergyPowerMath.su(12345), rpm, ports) * (double) Math.abs(rpm) * ports;
                assertEquals(24690, total, 0.005);
            }
        }
    }

    @Test void invalidPowerAndExpiredTicksCannotGenerate() {
        for (double value : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEquals(0, EnergyPowerMath.coefficient(value, 32, 1));
        }
        assertEquals(0, EnergyPowerMath.coefficient(800, 0, 1));
        assertEquals(0, EnergyPowerMath.coefficient(800, 32, 0));
        assertTrue(EnergyPowerMath.isLive(100, 100));
        assertTrue(EnergyPowerMath.isLive(100, 101));
        assertFalse(EnergyPowerMath.isLive(100, 102));
        assertFalse(EnergyPowerMath.isLive(101, 100));
    }

    @Test void floatRoundingCannotIncreaseGenerationOrDiscountConsumption() {
        for (double su : new double[]{0.8, 1, 12345, 1_000_000_001D}) {
            for (float rpm : new float[]{31.7F, 256, 1024}) {
                assertTrue(EnergyPowerMath.coefficient(su,rpm,3)*(double)rpm*3 <= su);
                assertTrue(EnergyPowerMath.demandCoefficient(su,rpm)*(double)rpm >= su);
            }
        }
        assertEquals(0.125, MachinePowerMath.throughputMultiplier(128,1024));
        assertEquals(0.1255, MachinePowerMath.throughputMultiplier(128.512F,1024),0.000001);
    }
}
