package dev.szx.dimensionworks.mekstress.core;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class StressTransferTest {
    @Test void sourceNeverAdvertisesMoreThanItsReservedShare() {
        for (long su : new long[]{1, 700, 262144, 2621441}) {
            for (int rpm : new int[]{1, 3, 64, 10240}) {
                float coefficient = StressTransfer.capacityPerRpm(su, rpm);
                assertTrue((double) coefficient * rpm <= su);
            }
        }
        assertEquals(10.9375f, StressTransfer.capacityPerRpm(700, 64));
        assertEquals(0, StressTransfer.capacityPerRpm(0, 64));
        assertEquals(0, StressTransfer.capacityPerRpm(700, 0));
    }
}
