package dev.szx.dimensionworks.mekstress.core;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class MemoryDriveMathTest {
    @Test void cardRpmAddsToSmallBaseInsteadOfReplacingIt() {
        long base = MemoryTier.DDR1.defaultDriveBandwidthRpm();
        long card = MemoryTier.DDR1.defaultCardBandwidthRpm();
        assertEquals(1280, base + 4 * MemoryDriveMath.scaleByPowerOfTwo(card, 0));
        assertEquals(2304, base + 4 * MemoryDriveMath.scaleByPowerOfTwo(card, 1));
        assertEquals(128, MemoryDriveMath.scaleByPowerOfTwo(card, -1));
        assertEquals(64, MemoryDriveMath.scaleByPowerOfTwo(card, -2));
        assertEquals(Long.MAX_VALUE, MemoryDriveMath.scaleByPowerOfTwo(Long.MAX_VALUE, 2));
    }
}
