package dev.szx.dimensionworks.mekstress.api;

/** Instantaneous mechanical capacity, not a persistent energy inventory. */
public record MemoryNetworkSnapshot(int driveCount, long capacitySu, long effectiveCapacitySu,
                                    long allocatedSu, long bandwidthRpm, long demandRpm,
                                    MemoryNetworkStatus status) {
    public long availableSu() { return Math.max(0, effectiveCapacitySu - allocatedSu); }
    public double finalQ() { return status == MemoryNetworkStatus.OK ? 1.0 : 0.0; }
    public static MemoryNetworkSnapshot empty(MemoryNetworkStatus status) {
        return new MemoryNetworkSnapshot(0, 0, 0, 0, 0, 0, status);
    }
}
