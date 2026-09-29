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

    public static final ForgeConfigSpec.DoubleValue GEAR_HEART_OUTPUT_FAILURE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue GEAR_HEART_FREEZE_CHANCE;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_FREEZE_INTERVAL_MIN_SECONDS;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_FREEZE_INTERVAL_MAX_SECONDS;
    public static final ForgeConfigSpec.IntValue GEAR_HEART_FREEZE_DURATION_TICKS;

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
        GEAR_HEART_OUTPUT_FAILURE_CHANCE = builder
            .comment("Chance for a cursed gear heart owner's completed machine operation to fail output.")
            .defineInRange("outputFailureChance", 0.35d, 0.0d, 1.0d);
        GEAR_HEART_FREEZE_CHANCE = builder
            .comment("Chance to freeze for the configured duration when a freeze interval elapses.")
            .defineInRange("freezeChance", 0.75d, 0.0d, 1.0d);
        GEAR_HEART_FREEZE_INTERVAL_MIN_SECONDS = builder
            .defineInRange("freezeIntervalMinSeconds", 40, 1, 3600);
        GEAR_HEART_FREEZE_INTERVAL_MAX_SECONDS = builder
            .defineInRange("freezeIntervalMaxSeconds", 240, 1, 3600);
        GEAR_HEART_FREEZE_DURATION_TICKS = builder
            .defineInRange("freezeDurationTicks", 20, 1, 200);
        builder.pop();

        SPEC = builder.build();
    }

    private RpmLimitConfig() {}
}
