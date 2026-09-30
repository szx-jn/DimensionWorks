package dev.szx.dimensionworks.mekstress.core;

/** Pure capacity and bandwidth math shared by drives, configuration and tests. */
public final class MemoryDriveMath {
    private static final int MIN_POWER = -16;
    private static final int MAX_POWER = 16;

    private MemoryDriveMath() {
    }

    public static long cardCapacitySu(long baseCardCapacitySu, int storageExponent) {
        if (baseCardCapacitySu <= 0L) {
            return 0L;
        }
        return scaleByPowerOfTwo(baseCardCapacitySu, storageExponent);
    }

    public static long bandwidthDeltaRpm(long baseDriveBandwidthRpm, int speedExponent, int slotsPerDrive) {
        if (baseDriveBandwidthRpm <= 0L || slotsPerDrive <= 0) {
            return 0L;
        }
        double multiplier = Math.scalb(1.0D, speedExponent);
        return Math.round((double) baseDriveBandwidthRpm * (multiplier - 1.0D) / (double) slotsPerDrive);
    }

    public static long effectiveBandwidthRpm(long baseDriveBandwidthRpm, int slotsPerDrive, long totalDeltaRpm) {
        if (baseDriveBandwidthRpm < 0L) {
            baseDriveBandwidthRpm = 0L;
        }
        long total = baseDriveBandwidthRpm + totalDeltaRpm;
        if (totalDeltaRpm > 0L && total < baseDriveBandwidthRpm) {
            return Long.MAX_VALUE;
        }
        if (totalDeltaRpm < 0L && total < 0L) {
            return 0L;
        }
        return total;
    }

    public static long scaleByPowerOfTwo(long value, int exponent) {
        if (value == 0L) {
            return 0L;
        }
        int boundedExponent = Math.max(MIN_POWER, Math.min(MAX_POWER, exponent));
        if (boundedExponent >= 0) {
            if (value > (Long.MAX_VALUE >> boundedExponent)) {
                return Long.MAX_VALUE;
            }
            return value << boundedExponent;
        }
        return value >> -boundedExponent;
    }
}
