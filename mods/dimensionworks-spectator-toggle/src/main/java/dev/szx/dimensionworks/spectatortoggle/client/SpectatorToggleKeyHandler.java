package dev.szx.dimensionworks.spectatortoggle.client;

import dev.szx.dimensionworks.spectatortoggle.DimensionWorksSpectatorToggle;
import dev.szx.dimensionworks.spectatortoggle.network.SpectatorToggleNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DimensionWorksSpectatorToggle.MOD_ID, value = Dist.CLIENT)
public final class SpectatorToggleKeyHandler {
    private SpectatorToggleKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (SpectatorToggleKeyMappings.TOGGLE_SPECTATOR.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                SpectatorToggleNetwork.sendToggleRequest();
            }
        }
    }
}
