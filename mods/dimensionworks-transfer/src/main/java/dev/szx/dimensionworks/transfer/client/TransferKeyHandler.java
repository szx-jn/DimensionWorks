package dev.szx.dimensionworks.transfer.client;

import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** Sends a dedicated server request when the configurable key is pressed. */
@net.neoforged.fml.common.EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class TransferKeyHandler {
    private TransferKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {


        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (TransferKeyMappings.OPEN_TRANSFER.consumeClick()) {
            TransferNetwork.sendToggleRequest();
        }
    }
}
