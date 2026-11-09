package dev.szx.dimensionworks.pressurecrystals;

public enum PressureTier {
    NONE,
    LOW,
    MEDIUM,
    HIGH;

    public static PressureTier fromRpm(float rpm) {
        if (rpm <= 0.0F) return NONE;
        if (rpm <= 512.0F) return LOW;
        if (rpm <= 2048.0F) return MEDIUM;
        return HIGH;
    }
}
