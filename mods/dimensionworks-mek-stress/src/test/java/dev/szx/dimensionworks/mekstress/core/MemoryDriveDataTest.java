package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class MemoryDriveDataTest {
    @Test
    void survivesSignedShortContainerPacketsAtCapacityAndBandwidthBoundaries() {
        for (long value : new long[]{0, 32768, 65535, 65536, 131072, 262144,
                                     1L << 32, (1L << 48) + 12345, Long.MAX_VALUE}) {
            int[] packet = new int[MemoryDriveData.SIZE];
            for (int word = 0; word < 4; word++)
                packet[MemoryDriveData.CAPACITY + word] = (short) MemoryDriveData.word(value, word);
            assertEquals(value, MemoryDriveData.read(i -> packet[i], MemoryDriveData.CAPACITY));
        }
    }
}
