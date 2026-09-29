package dev.szx.dimensionworks.cavefactory.client;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.registry.CFMenus;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
    modid = DimensionWorksCaveFactory.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class CaveFactoryClientEvents {
    private CaveFactoryClientEvents() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(
            CFMenus.factoryMachine(),
            FactoryMachineScreen::new
        ));
        event.enqueueWork(() -> PonderIndex.addPlugin(new CaveFactoryPonderPlugin()));
    }
}
