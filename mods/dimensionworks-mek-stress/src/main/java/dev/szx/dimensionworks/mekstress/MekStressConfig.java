package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.core.MachineTier;
import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import net.minecraftforge.common.ForgeConfigSpec;

/** COMMON balance values. Structure rules stay in Java and cannot be configured away. */
public final class MekStressConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.LongValue DDR1_CARD_SU;
    public static final ForgeConfigSpec.LongValue DDR2_CARD_SU;
    public static final ForgeConfigSpec.LongValue DDR3_CARD_SU;
    public static final ForgeConfigSpec.LongValue DDR4_CARD_SU;
    public static final ForgeConfigSpec.LongValue DDR5_CARD_SU;
    public static final ForgeConfigSpec.LongValue DDR1_DRIVE_BANDWIDTH;
    public static final ForgeConfigSpec.LongValue DDR2_DRIVE_BANDWIDTH;
    public static final ForgeConfigSpec.LongValue DDR3_DRIVE_BANDWIDTH;
    public static final ForgeConfigSpec.LongValue DDR4_DRIVE_BANDWIDTH;
    public static final ForgeConfigSpec.LongValue DDR5_DRIVE_BANDWIDTH;
    public static final ForgeConfigSpec.IntValue BASIC_MACHINE_RPM;
    public static final ForgeConfigSpec.IntValue ADVANCED_MACHINE_RPM;
    public static final ForgeConfigSpec.IntValue ELITE_MACHINE_RPM;
    public static final ForgeConfigSpec.IntValue ULTIMATE_MACHINE_RPM;
    public static final ForgeConfigSpec.DoubleValue EFFICIENCY_EXPONENT;
    public static final ForgeConfigSpec.IntValue DIRECT_MAX_RPM;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("SU stored by one Memory Card. Card count changes capacity only, never bandwidth.")
            .push("memory_card_capacity");
        DDR1_CARD_SU = card(builder, "ddr1", MemoryTier.DDR1.defaultCardCapacitySu());
        DDR2_CARD_SU = card(builder, "ddr2", MemoryTier.DDR2.defaultCardCapacitySu());
        DDR3_CARD_SU = card(builder, "ddr3", MemoryTier.DDR3.defaultCardCapacitySu());
        DDR4_CARD_SU = card(builder, "ddr4", MemoryTier.DDR4.defaultCardCapacitySu());
        DDR5_CARD_SU = card(builder, "ddr5", MemoryTier.DDR5.defaultCardCapacitySu());
        builder.pop();

        builder.comment("RPM bandwidth provided by one Memory Drive.")
            .push("memory_drive_bandwidth");
        DDR1_DRIVE_BANDWIDTH = bandwidth(builder, "ddr1", MemoryTier.DDR1.defaultDriveBandwidthRpm());
        DDR2_DRIVE_BANDWIDTH = bandwidth(builder, "ddr2", MemoryTier.DDR2.defaultDriveBandwidthRpm());
        DDR3_DRIVE_BANDWIDTH = bandwidth(builder, "ddr3", MemoryTier.DDR3.defaultDriveBandwidthRpm());
        DDR4_DRIVE_BANDWIDTH = bandwidth(builder, "ddr4", MemoryTier.DDR4.defaultDriveBandwidthRpm());
        DDR5_DRIVE_BANDWIDTH = bandwidth(builder, "ddr5", MemoryTier.DDR5.defaultDriveBandwidthRpm());
        builder.pop();

        builder.comment("Target RPM for each Mekanism machine quality tier.")
            .push("mekanism_machine_rpm");
        BASIC_MACHINE_RPM = machineRpm(builder, "basic", MachineTier.BASIC.targetRpm());
        ADVANCED_MACHINE_RPM = machineRpm(builder, "advanced", MachineTier.ADVANCED.targetRpm());
        ELITE_MACHINE_RPM = machineRpm(builder, "elite", MachineTier.ELITE.targetRpm());
        ULTIMATE_MACHINE_RPM = machineRpm(builder, "ultimate", MachineTier.ULTIMATE.targetRpm());
        builder.pop();

        builder.push("tuning");
        EFFICIENCY_EXPONENT = builder
            .comment("Exponent for network efficiency (q^exponent) and AE production (q^(exponent+1)).")
            .defineInRange("efficiencyExponent", 1.25D, 0.0D, 10.0D);
        DIRECT_MAX_RPM = builder
            .comment("Maximum effective RPM available to a Create-direct machine.")
            .defineInRange("directMaxRpm", 10_240, 1, 10_240);
        builder.pop();

        SPEC = builder.build();
    }

    private MekStressConfig() {
    }

    private static ForgeConfigSpec.LongValue card(ForgeConfigSpec.Builder builder, String tier, long value) {
        return builder.comment("SU per " + tier.toUpperCase() + " Memory Card.")
            .defineInRange(tier + "CardSu", value, 1L, Long.MAX_VALUE);
    }

    private static ForgeConfigSpec.LongValue bandwidth(ForgeConfigSpec.Builder builder, String tier, long value) {
        return builder.comment("RPM bandwidth per " + tier.toUpperCase() + " Memory Drive.")
            .defineInRange(tier + "BandwidthRpm", value, 1L, Long.MAX_VALUE);
    }

    private static ForgeConfigSpec.IntValue machineRpm(ForgeConfigSpec.Builder builder, String tier, int value) {
        return builder.comment(tier + " Mekanism machine target RPM.")
            .defineInRange(tier + "Rpm", value, 1, 65_536);
    }

    public static long cardCapacitySu(MemoryTier tier) {
        return switch (tier) {
            case DDR1 -> DDR1_CARD_SU.get();
            case DDR2 -> DDR2_CARD_SU.get();
            case DDR3 -> DDR3_CARD_SU.get();
            case DDR4 -> DDR4_CARD_SU.get();
            case DDR5 -> DDR5_CARD_SU.get();
        };
    }

    public static long driveBandwidthRpm(MemoryTier tier) {
        return switch (tier) {
            case DDR1 -> DDR1_DRIVE_BANDWIDTH.get();
            case DDR2 -> DDR2_DRIVE_BANDWIDTH.get();
            case DDR3 -> DDR3_DRIVE_BANDWIDTH.get();
            case DDR4 -> DDR4_DRIVE_BANDWIDTH.get();
            case DDR5 -> DDR5_DRIVE_BANDWIDTH.get();
        };
    }

    public static int machineRpm(MachineTier tier) {
        return switch (tier) {
            case BASIC -> BASIC_MACHINE_RPM.get();
            case ADVANCED -> ADVANCED_MACHINE_RPM.get();
            case ELITE -> ELITE_MACHINE_RPM.get();
            case ULTIMATE -> ULTIMATE_MACHINE_RPM.get();
        };
    }

    public static double efficiencyExponent() {
        return EFFICIENCY_EXPONENT.get();
    }

    public static int directMaxRpm() {
        return DIRECT_MAX_RPM.get();
    }
}
