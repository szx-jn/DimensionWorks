package dev.szx.dimensionworks.mekstress.core;

/** Mekanism quality tiers covered by the AE mechanical power bridge. */
public enum MachineTier {
    BASIC(1_024, 1_024L, 0),
    ADVANCED(1_024, 2_048L, 1),
    ELITE(1_024, 4_096L, 2),
    ULTIMATE(1_024, 8_192L, 3);

    private final int targetRpm;
    private final long bandwidthRpm;
    private final int mekanismOrdinal;

    MachineTier(int targetRpm, long bandwidthRpm, int mekanismOrdinal) {
        this.targetRpm = targetRpm;
        this.bandwidthRpm = bandwidthRpm;
        this.mekanismOrdinal = mekanismOrdinal;
    }

    public int targetRpm() {
        return targetRpm;
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
