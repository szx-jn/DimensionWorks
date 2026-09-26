package dev.szx.dimensionworks.transfer.client;

import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sends a dedicated server request when the configurable key is pressed. */
@Mod.EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class TransferKeyHandler {
    private TransferKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (TransferKeyMappings.OPEN_TRANSFER.consumeClick()) {
            TransferNetwork.sendToggleRequest();
        }
    }
}
