package dev.szx.dimensionworks.mekstress.core;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/** Required input speed for Mekanism's four upgradeable machine tiers. */
public enum StressMachineTier {
    BASIC(128),
    ADVANCED(512),
    ELITE(2048),
    ULTIMATE(10240);

    private final int requiredRpm;

    StressMachineTier(int requiredRpm) {
        this.requiredRpm = requiredRpm;
    }

    public int requiredRpm() {
        return requiredRpm;
    }

    public static StressMachineTier forBaseTier(@Nullable String baseTierName) {
        if (baseTierName == null) {
            return BASIC;
        }
        try {
            return valueOf(baseTierName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BASIC;
        }
    }
}
