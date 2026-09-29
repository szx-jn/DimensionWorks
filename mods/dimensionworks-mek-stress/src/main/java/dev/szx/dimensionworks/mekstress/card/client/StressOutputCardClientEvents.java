package dev.szx.dimensionworks.mekstress.card.client;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DimensionWorksMekStress.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StressOutputCardClientEvents {

    private StressOutputCardClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(
            DimensionWorksMekStress.STRESS_OUTPUT_CARD_MENU.get(),
            StressOutputCardScreen::new
        ));
    }
}
