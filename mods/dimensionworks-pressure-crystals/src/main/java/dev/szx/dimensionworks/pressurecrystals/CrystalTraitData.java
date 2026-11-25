package dev.szx.dimensionworks.pressurecrystals;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class CrystalTraitData {
    private CrystalTraitData() {}

    public static Component getTooltipLine(ItemStack stack) {
        if (!stack.is(DimensionWorksPressureCrystals.LOW_PRESSURE_CRYSTAL.get())
            && !stack.is(DimensionWorksPressureCrystals.MEDIUM_PRESSURE_CRYSTAL.get())
            && !stack.is(DimensionWorksPressureCrystals.HIGH_PRESSURE_CRYSTAL.get())) {
            return null;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag root = customData.copyTag();
        if (!root.contains("DimensionWorks", CompoundTag.TAG_COMPOUND)) {
            return null;
        }

        String metal = root.getCompound("DimensionWorks").getString("Metal");
        return switch (metal) {
            case "iron", "copper", "zinc", "gold", "tin", "osmium" ->
                Component.translatable("tooltip.dimensionworks_pressure_crystals.trait." + metal)
                    .withStyle(ChatFormatting.AQUA);
            default -> null;
        };
    }
}
