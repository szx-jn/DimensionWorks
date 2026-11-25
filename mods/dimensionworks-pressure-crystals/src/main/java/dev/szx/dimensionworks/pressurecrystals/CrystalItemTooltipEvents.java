package dev.szx.dimensionworks.pressurecrystals;

import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = DimensionWorksPressureCrystals.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CrystalItemTooltipEvents {
    private CrystalItemTooltipEvents() {}

    @SubscribeEvent
    public static void appendItemTooltip(ItemTooltipEvent event) {
        Component trait = CrystalTraitData.getTooltipLine(event.getItemStack());
        if (trait != null) {
            event.getToolTip().add(trait);
        }
    }
}
