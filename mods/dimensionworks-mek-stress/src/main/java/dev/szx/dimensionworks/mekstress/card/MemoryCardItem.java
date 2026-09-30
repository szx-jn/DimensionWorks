package dev.szx.dimensionworks.mekstress.card;

import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.core.MemoryCardType;
import dev.szx.dimensionworks.mekstress.core.MemoryDriveMath;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MemoryCardItem extends Item {
    private final MemoryTier tier;
    private final MemoryCardType type;

    public MemoryCardItem(MemoryTier tier, MemoryCardType type, Properties properties) {
        super(properties);
        this.tier = tier;
        this.type = type;
    }

    public MemoryTier tier() {
        return tier;
    }

    public MemoryCardType type() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        long capacitySu = MemoryDriveMath.cardCapacitySu(
            MekStressConfig.cardCapacitySu(tier), MekStressConfig.storageExponent(type));
        long bandwidthDeltaRpm = MemoryDriveMath.bandwidthDeltaRpm(
            MekStressConfig.driveBandwidthRpm(tier), MekStressConfig.speedExponent(type), tier.slotsPerDrive());
        double fullDriveMultiplier = Math.scalb(1.0D, MekStressConfig.speedExponent(type));
        tooltip.add(Component.translatable(typeTranslationKey()).withStyle(typeColor()));
        tooltip.add(Component.translatable("tooltip.dimensionworks_mek_stress.memory_card_capacity",
            capacitySu).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dimensionworks_mek_stress.memory_card_bandwidth",
            signed(bandwidthDeltaRpm)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dimensionworks_mek_stress.memory_card_full_drive",
            multiplier(fullDriveMultiplier)).withStyle(ChatFormatting.GRAY));
    }

    public String typeTranslationKey() {
        return "memory_card_type.dimensionworks_mek_stress." + type.id();
    }

    private ChatFormatting typeColor() {
        return switch (type) {
            case ECONOMY -> ChatFormatting.GREEN;
            case BALANCED -> ChatFormatting.AQUA;
            case HIGH_SPEED -> ChatFormatting.RED;
            case HIGH_STORAGE -> ChatFormatting.LIGHT_PURPLE;
            case DEFECTIVE -> ChatFormatting.DARK_GRAY;
            case FINAL -> ChatFormatting.GOLD;
        };
    }

    private static String signed(long value) {
        return value > 0L ? "+" + value : Long.toString(value);
    }

    private static String multiplier(double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
