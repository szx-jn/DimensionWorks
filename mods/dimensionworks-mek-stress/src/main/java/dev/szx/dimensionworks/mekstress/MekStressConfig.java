package dev.szx.dimensionworks.mekstress;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MekStressConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue JOULES_PER_SU;
    public static final ForgeConfigSpec.IntValue CARD_DEFAULT_RPM;
    public static final ForgeConfigSpec.IntValue CARD_MAX_RPM;
    public static final ForgeConfigSpec.LongValue CARD_DEFAULT_STRESS_PER_TICK;
    public static final ForgeConfigSpec.LongValue CARD_MAX_STRESS_PER_TICK;
    public static final ForgeConfigSpec.IntValue OWNER_FALLBACK_RPM;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("energy");
        JOULES_PER_SU = builder
            .comment("Mekanism joules represented by one Applied Create stress unit.")
            .defineInRange("joulesPerSu", 2.5D, 0.001D, 1_000_000.0D);
        builder.pop();

        builder.push("card");
        CARD_DEFAULT_RPM = builder
            .comment("Requested RPM stored by a newly configured stress output card.")
            .defineInRange("defaultRpm", 32, 0, 10_240);
        CARD_MAX_RPM = builder
            .comment("Upper bound accepted by the card UI before the owner limit is applied.")
            .defineInRange("maxRpm", 10_240, 1, 10_240);
        CARD_DEFAULT_STRESS_PER_TICK = builder
            .comment("Default maximum stress delivered by one output bus each tick.")
            .defineInRange("defaultStressPerTick", 1_024L, 0L, 1_048_576L);
        CARD_MAX_STRESS_PER_TICK = builder
            .comment("Absolute per-tick stress limit accepted by the card UI.")
            .defineInRange("maxStressPerTick", 1_048_576L, 1L, 1_048_576L);
        OWNER_FALLBACK_RPM = builder
            .comment("RPM used when an export bus has no resolvable owning player.")
            .defineInRange("ownerFallbackRpm", 32, 1, 10_240);
        builder.pop();
        SPEC = builder.build();
    }

    private MekStressConfig() {
    }

    public static double joulesPerSu() {
        return JOULES_PER_SU.get();
    }
}
