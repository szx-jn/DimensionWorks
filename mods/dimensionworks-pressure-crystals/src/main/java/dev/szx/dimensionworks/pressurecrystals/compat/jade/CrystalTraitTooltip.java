package dev.szx.dimensionworks.pressurecrystals.compat.jade;

import dev.szx.dimensionworks.pressurecrystals.CrystalTraitData;
import dev.szx.dimensionworks.pressurecrystals.DimensionWorksPressureCrystals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(DimensionWorksPressureCrystals.MOD_ID)
public final class CrystalTraitTooltip implements IWailaPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
        DimensionWorksPressureCrystals.MOD_ID, "crystal_traits"
    );

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(new DroppedCrystalProvider(), ItemEntity.class);
    }

    private static final class DroppedCrystalProvider implements IEntityComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof ItemEntity itemEntity) {
                var trait = CrystalTraitData.getTooltipLine(itemEntity.getItem());
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
