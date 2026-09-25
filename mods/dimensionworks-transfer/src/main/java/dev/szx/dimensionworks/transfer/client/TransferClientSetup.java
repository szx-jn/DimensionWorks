package dev.szx.dimensionworks.transfer.client;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WDTransfer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TransferClientSetup {
    private TransferClientSetup() {
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(WDKeys.TRANSFER_ID, new TransferDimensionEffects());
    }
}
