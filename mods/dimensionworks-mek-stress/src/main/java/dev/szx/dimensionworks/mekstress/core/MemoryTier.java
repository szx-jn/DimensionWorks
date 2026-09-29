package dev.szx.dimensionworks.mekstress.core;

/**
 * Fixed DDR structure and first-pass balance values for ME Memory hardware.
 *
 * <p>The slot counts are intentionally code constants: they define the hardware
 * contract and cannot be changed through configuration. Capacity and bandwidth
 * defaults are exposed here so pure tests and configuration defaults share one
 * source of truth.
 */
public enum MemoryTier {
    DDR1(1, 4, 8_192L, 8_192L),
    DDR2(2, 8, 9_216L, 16_384L),
    DDR3(3, 16, 10_240L, 32_768L),
    DDR4(4, 32, 12_288L, 65_536L),
    DDR5(5, 64, 16_384L, 131_072L);

    public static final int MAX_DRIVES_PER_NETWORK = 2;

    private final int index;
    private final int slotsPerDrive;
    private final long defaultCardCapacitySu;
    private final long defaultDriveBandwidthRpm;

    MemoryTier(int index, int slotsPerDrive, long cardCapacitySu, long driveBandwidthRpm) {
        this.index = index;
        this.slotsPerDrive = slotsPerDrive;
        this.defaultCardCapacitySu = cardCapacitySu;
        this.defaultDriveBandwidthRpm = driveBandwidthRpm;
    }

    public int index() {
        return index;
    }

    public int slotsPerDrive() {
        return slotsPerDrive;
    }

    public long cardCapacitySu() {
        return defaultCardCapacitySu;
    }

    public long driveBandwidthRpm() {
        return defaultDriveBandwidthRpm;
    }

    public long defaultCardCapacitySu() {
        return defaultCardCapacitySu;
    }

    public long defaultDriveBandwidthRpm() {
        return defaultDriveBandwidthRpm;
    }

    public int cardsInFullNetwork() {
        return slotsPerDrive * MAX_DRIVES_PER_NETWORK;
    }

    public long totalCapacitySu() {
        return Math.multiplyExact(cardsInFullNetwork(), defaultCardCapacitySu);
    }

    public long totalBandwidthRpm() {
        return Math.multiplyExact(MAX_DRIVES_PER_NETWORK, defaultDriveBandwidthRpm);
    }

    public static MemoryTier byIndex(int index) {
        if (index < 1 || index > values().length) {
            throw new IllegalArgumentException("Unknown DDR tier: " + index);
        }
        return values()[index - 1];
    }
}
