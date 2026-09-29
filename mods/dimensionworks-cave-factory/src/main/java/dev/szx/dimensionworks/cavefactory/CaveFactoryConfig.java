package dev.szx.dimensionworks.cavefactory;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CaveFactoryConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue STABILITY_SECONDS;
    public static final ForgeConfigSpec.IntValue PHASE_BATCHES;
    public static final ForgeConfigSpec.IntValue AMPOULE_CAPACITY;
    public static final ForgeConfigSpec.IntValue STABILIZATION_INPUT;
    public static final ForgeConfigSpec.IntValue KINETIC_SATURATION_RPM;
    public static final ForgeConfigSpec.DoubleValue STRESS_IMPACT;
    public static final ForgeConfigSpec.IntValue NUMERIC_MODULE_PERCENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("factory");
        STABILITY_SECONDS = builder
            .comment("Real-time seconds before a filled ampoule decays after leaving its origin dimension.")
            .defineInRange("stabilitySeconds", 60, 1, 86_400);
        PHASE_BATCHES = builder
            .comment("Successful batches required before the factory phase rotates.")
            .defineInRange("phaseBatches", 8, 1, 1_024);
        AMPOULE_CAPACITY = builder
            .comment("Fluid capacity of one ampoule in mB.")
            .defineInRange("ampouleCapacity", 250, 1, 10_000);
        STABILIZATION_INPUT = builder
            .comment("Fluid consumed by one stabilization operation in mB.")
            .defineInRange("stabilizationInput", 1_000, 1, 100_000);
        KINETIC_SATURATION_RPM = builder
            .comment("Factory machines reach their maximum native processing rate at this RPM.")
            .defineInRange("kineticSaturationRpm", 512, 1, 10_240);
        STRESS_IMPACT = builder
            .comment("Create stress impact of each factory controller and stabilizer.")
            .defineInRange("stressImpact", 64.0D, 0.0D, 1_024.0D);
        NUMERIC_MODULE_PERCENT = builder
            .comment(
                "Deterministic strength of implemented numeric modules, from 10 to 25 percent.",
                "Reserved modules receive no behavior from this value until their milestone is enabled."
            )
            .defineInRange("numericModulePercent", 25, 10, 25);
        builder.pop();
        SPEC = builder.build();
    }

    public static int stabilizationInput() {
        try {
            return STABILIZATION_INPUT.get();
        } catch (IllegalStateException | NullPointerException ignored) {
            return 1_000;
        }
    }

    public static int numericModulePercent() {
        try {
            return NUMERIC_MODULE_PERCENT.get();
        } catch (IllegalStateException | NullPointerException ignored) {
            return 25;
        }
    }

    private CaveFactoryConfig() {}
}
