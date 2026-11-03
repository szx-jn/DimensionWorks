package dev.szx.dimensionworks.spectatortoggle.client;

import dev.szx.dimensionworks.spectatortoggle.DimensionWorksSpectatorToggle;
import dev.szx.dimensionworks.spectatortoggle.network.SpectatorToggleNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DimensionWorksSpectatorToggle.MOD_ID, value = Dist.CLIENT)
public final class SpectatorToggleKeyHandler {
    private SpectatorToggleKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        while (SpectatorToggleKeyMappings.TOGGLE_SPECTATOR.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                SpectatorToggleNetwork.sendToggleRequest();
            }
        }
    }
}
