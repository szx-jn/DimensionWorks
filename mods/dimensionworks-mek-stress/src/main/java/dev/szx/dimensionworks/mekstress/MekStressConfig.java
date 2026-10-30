package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.core.MachineTier;
import dev.szx.dimensionworks.mekstress.core.MemoryCardType;
import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import java.util.EnumMap;
import net.minecraftforge.common.ForgeConfigSpec;

/** RPM scheduling and bounded transmission loss; no card-provided stress capacity. */
public final class MekStressConfig {
    public static final ForgeConfigSpec SPEC;
    private static final EnumMap<MemoryTier, ForgeConfigSpec.LongValue> DRIVE_RPM = new EnumMap<>(MemoryTier.class);
    private static final EnumMap<MemoryTier, ForgeConfigSpec.LongValue> CARD_RPM = new EnumMap<>(MemoryTier.class);
    private static final EnumMap<MemoryCardType, ForgeConfigSpec.IntValue> SPEED = new EnumMap<>(MemoryCardType.class);
    private static final EnumMap<MachineTier, ForgeConfigSpec.IntValue> MACHINE_RPM = new EnumMap<>(MachineTier.class);
    public static final ForgeConfigSpec.DoubleValue DEFECTIVE_CHANCE;
    public static final ForgeConfigSpec.IntValue MAXIMUM_INPUT_LOSS_BP;
    public static final ForgeConfigSpec.IntValue DIRECT_MAX_RPM;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("memory_drive_bandwidth");
        for (MemoryTier tier : MemoryTier.values())
            DRIVE_RPM.put(tier, builder.defineInRange("ddr" + tier.index() + "BandwidthRpm",
                tier.defaultDriveBandwidthRpm(), 1L, Long.MAX_VALUE));
        builder.pop();
        builder.push("memory_card_bandwidth");
        for (MemoryTier tier : MemoryTier.values())
            CARD_RPM.put(tier, builder.defineInRange("ddr" + tier.index() + "CardRpm",
                tier.defaultCardBandwidthRpm(), 1L, Long.MAX_VALUE));
        builder.pop();
        builder.push("memory_card_types");
        DEFECTIVE_CHANCE = builder.defineInRange("defectiveChance", 0.25D, 0D, 1D);
        for (MemoryCardType type : MemoryCardType.values()) {
            builder.push(type.id());
            SPEED.put(type, builder.defineInRange("speedExponent", type.defaultSpeedExponent(), -8, 8));
            builder.pop();
        }
        builder.pop();
        builder.push("mekanism_machine_rpm");
        for (MachineTier tier : MachineTier.values())
            MACHINE_RPM.put(tier, builder.defineInRange(tier.name().toLowerCase() + "Rpm", tier.targetRpm(), 1, 65536));
        builder.pop();
        builder.push("tuning");
        MAXIMUM_INPUT_LOSS_BP = builder.comment("Maximum network input stress loss, in basis points (1500 = 15%).")
            .defineInRange("maximumInputLossBasisPoints", 1500, 0, 10000);
        DIRECT_MAX_RPM = builder.defineInRange("directMaxRpm", 10240, 1, 10240);
        builder.pop();
        SPEC = builder.build();
    }

    private MekStressConfig() {}
    public static long driveBandwidthRpm(MemoryTier tier) { return DRIVE_RPM.get(tier).get(); }
    public static long cardBandwidthRpm(MemoryTier tier) { return CARD_RPM.get(tier).get(); }
    public static int speedExponent(MemoryCardType type) { return SPEED.get(type).get(); }
    public static int machineRpm(MachineTier tier) { return MACHINE_RPM.get(tier).get(); }
    public static double defectiveChance() { return DEFECTIVE_CHANCE.get(); }
    public static int maximumInputLossBasisPoints() { return MAXIMUM_INPUT_LOSS_BP.get(); }
    public static int directMaxRpm() { return DIRECT_MAX_RPM.get(); }
}
