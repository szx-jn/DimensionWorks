package dev.szx.dimensionworks.mekstress.core;

import java.util.function.IntUnaryOperator;

/** Container data packets carry signed 16-bit values; never send a long as two ints. */
public final class MemoryDriveData {
    public static final int CAPACITY = 1;
    public static final int STORED = 5;
    public static final int BANDWIDTH = 9;
    public static final int STATUS = 13;
    public static final int CARDS = 14;
    public static final int PAGE = 15;
    public static final int SIZE = 16;

    private MemoryDriveData() {}

    public static int word(long value, int word) {
        return (int) (value >>> (word * 16)) & 0xffff;
    }

    public static long read(IntUnaryOperator data, int offset) {
        long value = 0;
        for (int word = 0; word < 4; word++) {
            value |= (long) (data.applyAsInt(offset + word) & 0xffff) << (word * 16);
        }
        return value;
    }
}
