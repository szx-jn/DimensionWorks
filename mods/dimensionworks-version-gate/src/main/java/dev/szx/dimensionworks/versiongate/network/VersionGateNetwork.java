package dev.szx.dimensionworks.versiongate.network;

import dev.szx.dimensionworks.versiongate.DimensionWorksVersionGate;
import dev.szx.dimensionworks.versiongate.PackVersion;
import dev.szx.dimensionworks.versiongate.client.ClientVersionGate;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public final class VersionGateNetwork {
    static final ConfigurationTask.Type TASK_TYPE = new ConfigurationTask.Type(
            ResourceLocation.fromNamespaceAndPath(DimensionWorksVersionGate.MOD_ID, "check_pack_version"));

    private VersionGateNetwork() {}

    public static void register(IEventBus modBus, Dist dist) {
        // The client method reference is created only on the physical client.
        IPayloadHandler<LoginVersionPacket> clientHandler = dist == Dist.CLIENT
                ? ClientVersionGate::handleServerVersion
                : (packet, context) -> { throw new IllegalStateException("Client payload received on a dedicated server"); };
        Path directory = dist == Dist.CLIENT ? ClientVersionGate.gameDirectory()
                : FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        modBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("2");
            registrar.configurationToClient(LoginVersionPacket.TYPE, LoginVersionPacket.STREAM_CODEC, clientHandler);
            registrar.configurationToServer(VersionAcceptedPacket.TYPE, VersionAcceptedPacket.STREAM_CODEC,
                    (packet, context) -> context.finishCurrentTask(TASK_TYPE));
        });
        modBus.addListener((net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent event) ->
                event.register(new VersionTask(directory)));
    }

    record VersionTask(Path gameDirectory) implements ICustomConfigurationTask {
        @Override
        public void run(Consumer<CustomPacketPayload> sender) {
            String version;
            try {
                version = PackVersion.readVersion(gameDirectory).toString();
                DimensionWorksVersionGate.LOGGER.info("DimensionWorks server VERSION: {} = {}",
                        gameDirectory.resolve("VERSION"), version);
            } catch (IOException exception) {
                DimensionWorksVersionGate.LOGGER.error("Cannot read DimensionWorks server VERSION: {}: {}",
                        gameDirectory.resolve("VERSION"), exception.getMessage());
                version = "";
            }
            sender.accept(new LoginVersionPacket(version));
        }

        @Override
        public ConfigurationTask.Type type() {
            return TASK_TYPE;
        }
    }
}
