package dev.szx.dimensionworks.mekstress.core;

/** The six orthogonal memory-card archetypes used by every DDR tier. */
public enum MemoryCardType {
    ECONOMY("economy", -1, -1, true),
    BALANCED("balanced", 0, 0, true),
    HIGH_SPEED("high_speed", -1, 1, true),
    HIGH_STORAGE("high_storage", 1, -1, true),
    DEFECTIVE("defective", -2, -2, false),
    FINAL("final", 2, 2, false);

    private final String id;
    private final int defaultStorageExponent;
    private final int defaultSpeedExponent;
    private final boolean craftable;

    MemoryCardType(String id, int defaultStorageExponent, int defaultSpeedExponent, boolean craftable) {
        this.id = id;
        this.defaultStorageExponent = defaultStorageExponent;
        this.defaultSpeedExponent = defaultSpeedExponent;
        this.craftable = craftable;
    }

    public String id() {
        return id;
    }

    public int defaultStorageExponent() {
        return defaultStorageExponent;
    }

    public int defaultSpeedExponent() {
        return defaultSpeedExponent;
    }

    public boolean craftable() {
        return craftable;
    }
}
