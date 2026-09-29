package dev.szx.dimensionworks.mekstress.client;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DimensionWorksMekStress.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MemoryDriveClientEvents {
    private MemoryDriveClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(
            DimensionWorksMekStress.MEMORY_DRIVE_MENU.get(), MemoryDriveScreen::new));
    }
}
