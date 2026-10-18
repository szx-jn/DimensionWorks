package dev.szx.dimensionworks.versiongate.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "dimensionworks_version_gate", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientVersionGate {
    private static volatile Mismatch pending;

    private ClientVersionGate() {}

    public static void setMismatch(String localVersion, String serverVersion) {
        pending = new Mismatch(localVersion, serverVersion);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Mismatch mismatch = pending;
        if (mismatch == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DisconnectedScreen) {
            pending = null;
            minecraft.setScreen(new VersionMismatchScreen(mismatch.localVersion(), mismatch.serverVersion()));
        } else if (minecraft.screen instanceof TitleScreen) {
            // Keep the pending screen until the login disconnect screen is displayed.
        }
    }

    public record Mismatch(String localVersion, String serverVersion) {}
}
