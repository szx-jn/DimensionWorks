package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.core.MachineTier;
import dev.szx.dimensionworks.mekstress.core.MemoryCardType;
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
    public static final ForgeConfigSpec.IntValue ECONOMY_STORAGE_EXPONENT;
    public static final ForgeConfigSpec.IntValue ECONOMY_SPEED_EXPONENT;
    public static final ForgeConfigSpec.IntValue BALANCED_STORAGE_EXPONENT;
    public static final ForgeConfigSpec.IntValue BALANCED_SPEED_EXPONENT;
    public static final ForgeConfigSpec.IntValue HIGH_SPEED_STORAGE_EXPONENT;
    public static final ForgeConfigSpec.IntValue HIGH_SPEED_SPEED_EXPONENT;
    public static final ForgeConfigSpec.IntValue HIGH_STORAGE_STORAGE_EXPONENT;
    public static final ForgeConfigSpec.IntValue HIGH_STORAGE_SPEED_EXPONENT;
    public static final ForgeConfigSpec.IntValue DEFECTIVE_STORAGE_EXPONENT;
    public static final ForgeConfigSpec.IntValue DEFECTIVE_SPEED_EXPONENT;
    public static final ForgeConfigSpec.IntValue FINAL_STORAGE_EXPONENT;
    public static final ForgeConfigSpec.IntValue FINAL_SPEED_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue DEFECTIVE_CHANCE;
    public static final ForgeConfigSpec.IntValue BASIC_MACHINE_RPM;
    public static final ForgeConfigSpec.IntValue ADVANCED_MACHINE_RPM;
    public static final ForgeConfigSpec.IntValue ELITE_MACHINE_RPM;
    public static final ForgeConfigSpec.IntValue ULTIMATE_MACHINE_RPM;
    public static final ForgeConfigSpec.DoubleValue EFFICIENCY_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue MEKANISM_FE_PER_SU;
    public static final ForgeConfigSpec.IntValue DIRECT_MAX_RPM;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("FE consumed by one unit of Mekanism processing work.")
            .push("energy");
        MEKANISM_FE_PER_SU = builder
            .comment("Mekanism FE represented by one SU. Default: 2.5 FE = 1 SU.")
            .defineInRange("mekanismPerSu", 2.5D, 0.001D, 1_000_000.0D);
        builder.pop();

        builder.comment("Base SU stored by one balanced Memory Card. Card archetypes scale this value.")
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

        builder.comment("Per-card storage and speed exponents for each memory-card archetype.")
            .push("memory_card_types");
        builder.push(MemoryCardType.ECONOMY.id());
        ECONOMY_STORAGE_EXPONENT = exponent(builder, "storageExponent", MemoryCardType.ECONOMY.defaultStorageExponent());
        ECONOMY_SPEED_EXPONENT = exponent(builder, "speedExponent", MemoryCardType.ECONOMY.defaultSpeedExponent());
        builder.pop();
        builder.push(MemoryCardType.BALANCED.id());
        BALANCED_STORAGE_EXPONENT = exponent(builder, "storageExponent", MemoryCardType.BALANCED.defaultStorageExponent());
        BALANCED_SPEED_EXPONENT = exponent(builder, "speedExponent", MemoryCardType.BALANCED.defaultSpeedExponent());
        builder.pop();
        builder.push(MemoryCardType.HIGH_SPEED.id());
        HIGH_SPEED_STORAGE_EXPONENT = exponent(builder, "storageExponent", MemoryCardType.HIGH_SPEED.defaultStorageExponent());
        HIGH_SPEED_SPEED_EXPONENT = exponent(builder, "speedExponent", MemoryCardType.HIGH_SPEED.defaultSpeedExponent());
        builder.pop();
        builder.push(MemoryCardType.HIGH_STORAGE.id());
        HIGH_STORAGE_STORAGE_EXPONENT = exponent(builder, "storageExponent", MemoryCardType.HIGH_STORAGE.defaultStorageExponent());
        HIGH_STORAGE_SPEED_EXPONENT = exponent(builder, "speedExponent", MemoryCardType.HIGH_STORAGE.defaultSpeedExponent());
        builder.pop();
        builder.push(MemoryCardType.DEFECTIVE.id());
        DEFECTIVE_STORAGE_EXPONENT = exponent(builder, "storageExponent", MemoryCardType.DEFECTIVE.defaultStorageExponent());
        DEFECTIVE_SPEED_EXPONENT = exponent(builder, "speedExponent", MemoryCardType.DEFECTIVE.defaultSpeedExponent());
        builder.pop();
        builder.push(MemoryCardType.FINAL.id());
        FINAL_STORAGE_EXPONENT = exponent(builder, "storageExponent", MemoryCardType.FINAL.defaultStorageExponent());
        FINAL_SPEED_EXPONENT = exponent(builder, "speedExponent", MemoryCardType.FINAL.defaultSpeedExponent());
        builder.pop();
        DEFECTIVE_CHANCE = builder
            .comment("Independent chance for each crafted normal card to become a defective card.")
            .defineInRange("defectiveChance", 0.25D, 0.0D, 1.0D);
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
        return builder.comment("Base SU stored by one balanced " + tier.toUpperCase() + " Memory Card.")
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

    private static ForgeConfigSpec.IntValue exponent(
        ForgeConfigSpec.Builder builder, String key, int defaultValue) {
        return builder.defineInRange(key, defaultValue, -8, 8);
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

    public static int storageExponent(MemoryCardType type) {
        return switch (type) {
            case ECONOMY -> ECONOMY_STORAGE_EXPONENT.get();
            case BALANCED -> BALANCED_STORAGE_EXPONENT.get();
            case HIGH_SPEED -> HIGH_SPEED_STORAGE_EXPONENT.get();
            case HIGH_STORAGE -> HIGH_STORAGE_STORAGE_EXPONENT.get();
            case DEFECTIVE -> DEFECTIVE_STORAGE_EXPONENT.get();
            case FINAL -> FINAL_STORAGE_EXPONENT.get();
        };
    }

    public static int speedExponent(MemoryCardType type) {
        return switch (type) {
            case ECONOMY -> ECONOMY_SPEED_EXPONENT.get();
            case BALANCED -> BALANCED_SPEED_EXPONENT.get();
            case HIGH_SPEED -> HIGH_SPEED_SPEED_EXPONENT.get();
            case HIGH_STORAGE -> HIGH_STORAGE_SPEED_EXPONENT.get();
            case DEFECTIVE -> DEFECTIVE_SPEED_EXPONENT.get();
            case FINAL -> FINAL_SPEED_EXPONENT.get();
        };
    }

    public static double defectiveChance() {
        return DEFECTIVE_CHANCE.get();
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

    public static double fePerSu() {
        return MEKANISM_FE_PER_SU.get();
    }
}
