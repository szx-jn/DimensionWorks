package dev.szx.dimensionworks.rpmlimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RpmLimitMathTest {

    @Test
    void capsGearAmplifiedSpeedAtTheOwnerLimit() {
        assertEquals(512.0F, RpmLimitMath.cap(4096.0F, 512), 0.0F);
    }

    @Test
    void keepsTheDirectionWhenCapping() {
        assertEquals(-256.0F, RpmLimitMath.cap(-1024.0F, 256), 0.0F);
    }

    @Test
    void leavesSpeedsBelowTheLimitUntouched() {
        assertEquals(128.0F, RpmLimitMath.cap(128.0F, 512), 0.0F);
    }

    @Test
    void usesTheLowestActiveOwnerLimitOnSharedNetworks() {
        assertEquals(128, RpmLimitMath.lowestLimit(10240, 0, 128));
    }
}
