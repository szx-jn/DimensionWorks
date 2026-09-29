package dev.szx.dimensionworks.mekstress.core;

/** Mekanism quality tiers covered by the AE mechanical power bridge. */
public enum MachineTier {
    BASIC(512, 4_096L, 512L, 0),
    ADVANCED(1_024, 8_192L, 1_024L, 1),
    ELITE(2_048, 16_384L, 2_048L, 2),
    ULTIMATE(4_096, 32_768L, 4_096L, 3);

    private final int targetRpm;
    private final long suPerTick;
    private final long bandwidthRpm;
    private final int mekanismOrdinal;

    MachineTier(int targetRpm, long suPerTick, long bandwidthRpm, int mekanismOrdinal) {
        this.targetRpm = targetRpm;
        this.suPerTick = suPerTick;
        this.bandwidthRpm = bandwidthRpm;
        this.mekanismOrdinal = mekanismOrdinal;
    }

    public int targetRpm() {
        return targetRpm;
    }

    public long suPerTick() {
        return suPerTick;
    }

    public long bandwidthRpm() {
        return bandwidthRpm;
    }

    public int mekanismOrdinal() {
        return mekanismOrdinal;
    }

    public static MachineTier fromMekanismOrdinal(int ordinal) {
        for (MachineTier tier : values()) {
            if (tier.mekanismOrdinal == ordinal) {
                return tier;
            }
        }
        return BASIC;
    }
}
