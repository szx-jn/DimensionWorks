package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachinePowerMathTest {

    @Test
    void keepsAnIdleGearboxRunningWithoutChangingSingleMachineLoad() {
        assertEquals(4_096.0D, MachinePowerMath.gearboxLoad(0.0D, 512), 1.0E-9D);
        assertEquals(4_096.0D, MachinePowerMath.gearboxLoad(4_096.0D, 512), 1.0E-9D);
        assertEquals(12_288.0D, MachinePowerMath.gearboxLoad(12_288.0D, 512), 1.0E-9D);
        assertEquals(0.0D, MachinePowerMath.gearboxLoad(4_096.0D, 0), 1.0E-9D);
    }

    @Test
    void multipliesPerRpmStressByActualNetworkSpeed() {
        assertEquals(1_048_576.0D, MachinePowerMath.actualStress(256.0F, 4_096.0F), 1.0E-6D);
        assertEquals(1_048_576.0D, MachinePowerMath.actualStress(256.0F, -4_096.0F), 1.0E-6D);
        assertEquals(0.0D, MachinePowerMath.actualStress(8.0F, 0.0F), 1.0E-6D);
    }

    @Test
    void scalesWorkWithRpmAndCapsAtTargetSpeed() {
        assertEquals(0.0D, MachinePowerMath.workRate(0, 4_096), 1.0E-9D);
        assertEquals(0.25D, MachinePowerMath.workRate(1_024, 4_096), 1.0E-9D);
        assertEquals(1.0D, MachinePowerMath.workRate(4_096, 4_096), 1.0E-9D);
        assertEquals(1.0D, MachinePowerMath.workRate(8_192, 4_096), 1.0E-9D);
        assertEquals(0.0D, MachinePowerMath.workRate(4_096, 0), 1.0E-9D);
    }

    @Test
    void scalesRecipeOutputWithoutRoundingUpUnpaidFuel() {
        assertEquals(1.0D, MachinePowerMath.productionMultiplier(8_192, 8_192), 1.0E-9D);
        assertEquals(1.5D, MachinePowerMath.productionMultiplier(12_288, 8_192), 1.0E-9D);
        assertEquals(9L, MachinePowerMath.scaledOutput(3L, 2, 1.5D));
        assertEquals(1L, MachinePowerMath.scaledOutput(1L, 1, 1.25D));
        assertEquals(2L, MachinePowerMath.scaledOutput(1L, 2, 1.25D));
        assertEquals(1L, MachinePowerMath.scaledOutput(1L, 1, 1.99D));
        assertEquals(2L, MachinePowerMath.scaledOutput(2L, 1, 1025D / 1024D));
    }

    @Test
    void reportsEndToEndThroughputOnBothSidesOfTheTarget() {
        assertEquals(0.25D, MachinePowerMath.throughputMultiplier(1_024, 4_096), 1.0E-9D);
        assertEquals(1.0D, MachinePowerMath.throughputMultiplier(4_096, 4_096), 1.0E-9D);
        assertEquals(2.0D, MachinePowerMath.throughputMultiplier(8_192, 4_096), 1.0E-9D);
    }

    @Test
    void keepsDirectNetworkRpmForMekTierScalingWithinTheConfiguredMaximum() {
        assertEquals(4_096.0F, MachinePowerMath.directRpm(4_096.0F, 10_240), 1.0E-6F);
        assertEquals(10_240.0F, MachinePowerMath.directRpm(20_480.0F, 10_240), 1.0E-6F);
        assertEquals(4_096.0F, MachinePowerMath.directRpm(-4_096.0F, 10_240), 1.0E-6F);
        assertEquals(10_240.0F, MachinePowerMath.directRpm(-20_480.0F, 10_240), 1.0E-6F);
        assertEquals(0.0F, MachinePowerMath.directRpm(Float.NaN, 10_240), 1.0E-6F);
        assertEquals(0.0F, MachinePowerMath.directRpm(4_096.0F, 0), 1.0E-6F);
    }
    @org.junit.jupiter.api.Test
    void tieredOverdriveHasDistinctCapsAndRespectsTarget() {
        int[] targets = {1024, 2048, 4096, 8192};
        double[] caps = {1.25, 1.5, 1.75, 2.0};
        for (int i = 0; i < targets.length; i++) {
            assertEquals(1.0, MachinePowerMath.tierProductionMultiplier(targets[i], targets[i], caps[i]), 1e-9);
            assertEquals(caps[i], MachinePowerMath.tierThroughputMultiplier(10240, targets[i], caps[i]), 1e-9);
            assertEquals(caps[i], MachinePowerMath.tierProductionMultiplier(20000, targets[i], caps[i]), 1e-9);
            assertEquals(0.5, MachinePowerMath.tierThroughputMultiplier(targets[i] / 2f, targets[i], caps[i]), 1e-9);
        }
        assertEquals(1.0, MachinePowerMath.tierProductionMultiplier(10240, 10240, 2), 1e-9);
        assertEquals(0.0, MachinePowerMath.tierThroughputMultiplier(0, 1024, 1.25), 1e-9);
    }

    @Test
    void defaultFactoryUpgradesImproveThroughputAtEveryPoweredSpeed() {
        int[] parallel = {3, 5, 7, 9};
        for (int rpm = 1; rpm <= 10240; rpm++) {
            double previous = 0;
            for (MachineTier tier : MachineTier.values()) {
                int index = tier.mekanismOrdinal();
                double rate = MachinePowerMath.tierThroughputMultiplier(rpm, tier.targetRpm(), 1.25 + index * .25);
                double output = parallel[index] * rate;
                org.junit.jupiter.api.Assertions.assertTrue(output > previous, "RPM=" + rpm + ", tier=" + tier);
                previous = output;
            }
        }
    }

}
