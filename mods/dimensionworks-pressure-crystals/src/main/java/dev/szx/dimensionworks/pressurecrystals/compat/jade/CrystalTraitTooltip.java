package dev.szx.dimensionworks.pressurecrystals.compat.jade;

import dev.szx.dimensionworks.pressurecrystals.DimensionWorksPressureCrystals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Displays metal traits on crystal item tooltips and dropped-item Jade tooltips. */
@WailaPlugin(DimensionWorksPressureCrystals.MOD_ID)
public final class CrystalTraitTooltip implements IWailaPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
        DimensionWorksPressureCrystals.MOD_ID, "crystal_traits"
    );

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(new DroppedCrystalProvider(), ItemEntity.class);
    }

    public static Component getTraitLine(ItemStack stack) {
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

    private static final class DroppedCrystalProvider implements IEntityComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof ItemEntity itemEntity) {
                Component trait = getTraitLine(itemEntity.getItem());
                if (trait != null) {
                    tooltip.add(trait);
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
