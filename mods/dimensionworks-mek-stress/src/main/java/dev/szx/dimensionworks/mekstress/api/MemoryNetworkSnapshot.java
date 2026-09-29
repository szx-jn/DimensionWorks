package dev.szx.dimensionworks.mekstress.api;

public record MemoryNetworkSnapshot(
    int driveCount,
    long capacitySu,
    long storedSu,
    long bandwidthRpm,
    long demandRpm,
    long demandSuPerTick,
    double stockQ,
    double bandwidthQ,
    double finalQ,
    MemoryNetworkStatus status
) {
    public static MemoryNetworkSnapshot empty(MemoryNetworkStatus status) {
        return new MemoryNetworkSnapshot(0, 0L, 0L, 0L, 0L, 0L, 1.0D, 1.0D, 1.0D, status);
    }
}
