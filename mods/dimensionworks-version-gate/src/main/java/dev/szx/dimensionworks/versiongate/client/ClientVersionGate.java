package dev.szx.dimensionworks.versiongate.client;

import dev.szx.dimensionworks.versiongate.DimensionWorksVersionGate;
import dev.szx.dimensionworks.versiongate.VersionCheck;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "dimensionworks_version_gate", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientVersionGate {
    private static volatile VersionCheck pending;

    private ClientVersionGate() {}

    public static Path gameDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
    }

    public static void checkServerVersion(String serverVersion, Connection connection) {
        Path directory = gameDirectory();
        VersionCheck check = VersionCheck.evaluate(directory, serverVersion);
        DimensionWorksVersionGate.LOGGER.info("DimensionWorks client VERSION: {} = {}; server = {}",
                directory.resolve("VERSION"), check.localText(), check.serverText());
        if (check.status() == VersionCheck.Status.CURRENT) {
            pending = null;
            return;
        }
        DimensionWorksVersionGate.LOGGER.warn("DimensionWorks login rejected: {}; {}",
                check.message(), check.error());
        pending = check;
        connection.disconnect(Component.literal(check.message()));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        VersionCheck mismatch = pending;
        if (mismatch == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DisconnectedScreen) {
            pending = null;
            minecraft.setScreen(new VersionMismatchScreen(mismatch));
        } else if (minecraft.screen instanceof TitleScreen) {
            // Keep the pending screen until the login disconnect screen is displayed.
        }
    }
}
