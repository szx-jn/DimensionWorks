package dev.szx.dimensionworks.mekstress.core;

/** Drive slots and base RPM only; mechanical stress capacity comes exclusively from imports. */
public enum MemoryTier {
    DDR1(1, 4), DDR2(2, 8), DDR3(3, 16), DDR4(4, 32), DDR5(5, 64);
    public static final int MAX_DRIVES_PER_NETWORK = 2;
    private final int index;
    private final int slots;
    MemoryTier(int index, int slots) { this.index = index; this.slots = slots; }
    public int index() { return index; }
    public int slotsPerDrive() { return slots; }
    public int cardsInFullNetwork() { return slots * MAX_DRIVES_PER_NETWORK; }
    public long defaultDriveBandwidthRpm() { return 256L << (index - 1); }
    public long defaultCardBandwidthRpm() { return 256L << (index - 1); }
    public static MemoryTier byIndex(int index) {
        if (index < 1 || index > values().length) throw new IllegalArgumentException("Unknown DDR tier: " + index);
        return values()[index - 1];
    }
}
