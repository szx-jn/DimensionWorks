package dev.szx.dimensionworks.rpmlimit;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class RpmLimitConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue DEFAULT_RPM;
    public static final ForgeConfigSpec.IntValue MAX_RPM;
    public static final ForgeConfigSpec.IntValue CACHE_INTERVAL_TICKS;

    public static final ForgeConfigSpec.BooleanValue OVERSPEED_ENABLED;
    public static final ForgeConfigSpec.IntValue OVERSPEED_CAP;
    public static final ForgeConfigSpec.BooleanValue OVERSPEED_STRESS_SCALING;
    public static final ForgeConfigSpec.ConfigValue<String> OVERSPEED_STEP_TABLE;
    public static final ForgeConfigSpec.IntValue OVERSPEED_DEFAULT_SATURATION;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> OVERSPEED_CLASS_WHITELIST;

    public static final ForgeConfigSpec.IntValue GEAR_HEART_ROTATION_CURSED_RPM;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_ROTATION_REPAIRED_RPM;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_CREATE_CAPACITY_CURSED_SU;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_MEK_ORE_OPERATIONS_CURSED_PER_SECOND;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_MEK_ORE_OPERATIONS_REPAIRED_PER_SECOND;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_AE_CRAFTING_JOBS_CURSED;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_AE_CRAFTING_JOBS_REPAIRED;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_CROSS_PROCESS_CURSED_PER_SECOND;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_CROSS_PROCESS_REPAIRED_PER_SECOND;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_ACTIVE_MACHINES_CURSED;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_ACTIVE_MACHINES_REPAIRED;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_FINAL_PRODUCTION_CURSED_PER_SECOND;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_FINAL_PRODUCTION_REPAIRED_PER_SECOND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("rpm");
        DEFAULT_RPM = builder.defineInRange("defaultRpm", 32, 1, 10240);
        MAX_RPM = builder.defineInRange("maxRpm", 10240, 1, 10240);
        CACHE_INTERVAL_TICKS = builder.comment("12 ticks = 0.6 seconds")
            .defineInRange("cacheIntervalTicks", 12, 1, 200);
        builder.pop();

        builder.push("overspeed");
        OVERSPEED_ENABLED = builder
            .comment("Machines spinning past their own saturation point process several batches per tick.")
            .define("enabled", true);
        OVERSPEED_CAP = builder
            .comment("Hard ceiling for the batch multiplier of every machine.")
            .defineInRange("cap", 15, 1, 64);
        OVERSPEED_STRESS_SCALING = builder
            .comment("Multiply the machine's stress impact by its batch multiplier.")
            .define("stressScaling", true);
        OVERSPEED_STEP_TABLE = builder
            .comment(
                "Step table written as ratio:multiplier pairs, where ratio = RPM / the machine's saturation point.",
                "The multiplier of the last reached breakpoint is used; the cap above overrides larger values.",
                "Fallback value: " + OverspeedCurve.DEFAULT_TABLE
            )
            .define("stepTable", OverspeedCurve.DEFAULT_TABLE);
        OVERSPEED_DEFAULT_SATURATION = builder
            .comment("Saturation point assumed for whitelisted machines that are not known by name.")
            .defineInRange("defaultSaturationRpm", 512, 1, 10240);
        OVERSPEED_CLASS_WHITELIST = builder
            .comment(
                "Extra block entity classes that should batch too, as fully qualified or simple names.",
                "Rotation-only and item-moving blocks must stay out of this list."
            )
            .defineListAllowEmpty(
                List.of("classWhitelist"),
                List::of,
                entry -> entry instanceof String
            );
        builder.pop();

        builder.push("gearHeart");
        GEAR_HEART_ROTATION_CURSED_RPM = builder
            .defineInRange("rotationCursedRpm", 32, 1, 10240);
        GEAR_HEART_ROTATION_REPAIRED_RPM = builder
            .defineInRange("rotationRepairedRpm", 512, 1, 10240);
        GEAR_HEART_CREATE_CAPACITY_CURSED_SU = builder
            .defineInRange("createCapacityCursedSu", 4096, 1, Integer.MAX_VALUE);
        GEAR_HEART_MEK_ORE_OPERATIONS_CURSED_PER_SECOND = builder
            .defineInRange("mekOreOperationsCursedPerSecond", 8, 1, 1_000_000);
        GEAR_HEART_MEK_ORE_OPERATIONS_REPAIRED_PER_SECOND = builder
            .defineInRange("mekOreOperationsRepairedPerSecond", 64, 1, 1_000_000);
        GEAR_HEART_AE_CRAFTING_JOBS_CURSED = builder
            .defineInRange("aeCraftingJobsCursed", 1, 1, 1024);
        GEAR_HEART_AE_CRAFTING_JOBS_REPAIRED = builder
            .defineInRange("aeCraftingJobsRepaired", 80, 1, 1024);
        GEAR_HEART_CROSS_PROCESS_CURSED_PER_SECOND = builder
            .defineInRange("crossProcessItemsCursedPerSecond", 16, 1, 1_000_000);
        GEAR_HEART_CROSS_PROCESS_REPAIRED_PER_SECOND = builder
            .defineInRange("crossProcessItemsRepairedPerSecond", 128, 1, 1_000_000);
        GEAR_HEART_ACTIVE_MACHINES_CURSED = builder
            .defineInRange("activeMachinesCursed", 8, 1, 100_000);
        GEAR_HEART_ACTIVE_MACHINES_REPAIRED = builder
            .defineInRange("activeMachinesRepaired", 64, 1, 100_000);
        GEAR_HEART_FINAL_PRODUCTION_CURSED_PER_SECOND = builder
            .defineInRange("finalProductionItemsCursedPerSecond", 16, 1, 1_000_000);
        GEAR_HEART_FINAL_PRODUCTION_REPAIRED_PER_SECOND = builder
            .defineInRange("finalProductionItemsRepairedPerSecond", 128, 1, 1_000_000);
        builder.pop();

        SPEC = builder.build();
    }

    private RpmLimitConfig() {}
}
