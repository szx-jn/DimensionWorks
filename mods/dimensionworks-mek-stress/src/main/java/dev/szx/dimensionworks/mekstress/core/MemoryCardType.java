package dev.szx.dimensionworks.mekstress.core;

/** Each card trades input stress loss for additive RPM bandwidth. */
public enum MemoryCardType {
    ECONOMY("economy", -1, 200, true),
    BALANCED("balanced", 0, 150, true),
    HIGH_SPEED("high_speed", 1, 175, true),
    HIGH_STORAGE("high_storage", -1, 75, true),
    DEFECTIVE("defective", -2, 2000, false),
    FINAL("final", 2, 0, false);
    private final String id;
    private final int speed;
    private final int reduction;
    private final boolean craftable;
    MemoryCardType(String id, int speed, int reduction, boolean craftable) {
        this.id = id; this.speed = speed; this.reduction = reduction; this.craftable = craftable;
    }
    public String id() { return id; }
    public int defaultSpeedExponent() { return speed; }
    public int inputReductionBasisPoints() { return reduction; }
    public boolean craftable() { return craftable; }
}
