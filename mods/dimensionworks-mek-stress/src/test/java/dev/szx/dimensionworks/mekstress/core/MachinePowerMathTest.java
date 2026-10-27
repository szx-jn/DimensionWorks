package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachinePowerMathTest {

    @Test
    void chargesTwoHundredFiftySixSuForEachEffectiveRpm() {
        assertEquals(131_072L, MachinePowerMath.suForRpm(512.0F));
        assertEquals(1_048_576L, MachinePowerMath.suForRpm(4_096.0F));
        assertEquals(0L, MachinePowerMath.suForRpm(0.0F));
        assertEquals(131_072L, MachinePowerMath.suForRpm(-512.0F));
    }

    @Test
    void roundsFractionalRpmUpSoRunningMachinesNeverGetFreeTicks() {
        assertEquals(131_104L, MachinePowerMath.suForRpm(512.125F));
    }

    @Test
    void keepsAnIdleGearboxRunningWithoutChangingSingleMachineLoad() {
        assertEquals(4_096.0D, MachinePowerMath.gearboxLoad(0.0D, 512), 1.0E-9D);
        assertEquals(4_096.0D, MachinePowerMath.gearboxLoad(4_096.0D, 512), 1.0E-9D);
        assertEquals(12_288.0D, MachinePowerMath.gearboxLoad(12_288.0D, 512), 1.0E-9D);
        assertEquals(0.0D, MachinePowerMath.gearboxLoad(4_096.0D, 0), 1.0E-9D);
    }

    @Test
    void convertsEffectiveRpmScaleIntoPerRpmStress() {
        assertEquals(256.0F, MachinePowerMath.stressPerRpm(1.0F), 1.0E-6F);
        assertEquals(64.0F, MachinePowerMath.stressPerRpm(0.25F), 1.0E-6F);
        assertEquals(0.0F, MachinePowerMath.stressPerRpm(-1.0F), 1.0E-6F);
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
    void scalesEveryRecipeOutputAndRoundsFractionalAmountsUp() {
        assertEquals(1.0D, MachinePowerMath.productionMultiplier(8_192, 8_192), 1.0E-9D);
        assertEquals(1.5D, MachinePowerMath.productionMultiplier(12_288, 8_192), 1.0E-9D);
        assertEquals(9L, MachinePowerMath.scaledOutput(3L, 2, 1.5D));
        assertEquals(2L, MachinePowerMath.scaledOutput(1L, 1, 1.25D));
        assertEquals(3L, MachinePowerMath.scaledOutput(1L, 2, 1.25D));
        assertEquals(2L, MachinePowerMath.scaledOutput(1L, 1, 1.99D));
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
}
