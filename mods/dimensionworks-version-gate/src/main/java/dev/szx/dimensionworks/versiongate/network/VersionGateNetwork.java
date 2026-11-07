package dev.szx.dimensionworks.versiongate.network;

import dev.szx.dimensionworks.versiongate.DimensionWorksVersionGate;
import dev.szx.dimensionworks.versiongate.PackVersion;
import dev.szx.dimensionworks.versiongate.client.ClientVersionGate;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.Pair;

public final class VersionGateNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DimensionWorksVersionGate.MOD_ID, "login"),
            () -> PROTOCOL,
            ignored -> true,
            ignored -> true);

    private VersionGateNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(LoginVersionPacket.class, 0, NetworkDirection.LOGIN_TO_CLIENT)
                .encoder(LoginVersionPacket::encode)
                .decoder(LoginVersionPacket::new)
                .consumerNetworkThread(VersionGateNetwork::handleLoginVersion)
                .loginIndex(LoginVersionPacket::getAsInt, LoginVersionPacket::setLoginIndex)
                .markAsLoginPacket()
                .noResponse()
                .buildLoginPacketList(isLocal -> List.of(Pair.of(
                        "dimensionworks_version_gate:server_version",
                        new LoginVersionPacket(readServerVersion(), 0))))
                .add();
    }

    private static String readServerVersion() {
        Path gameDirectory = DistExecutor.unsafeRunForDist(
                () -> ClientVersionGate::gameDirectory,
                () -> () -> FMLPaths.GAMEDIR.get().toAbsolutePath().normalize());
        try {
            String version = PackVersion.readVersion(gameDirectory).toString();
            DimensionWorksVersionGate.LOGGER.info("DimensionWorks server VERSION: {} = {}",
                    gameDirectory.resolve("VERSION"), version);
            return version;
        } catch (IOException exception) {
            DimensionWorksVersionGate.LOGGER.error("Cannot read DimensionWorks server VERSION: {}",
                    exception.getMessage());
            return "";
        }
    }

    private static void handleLoginVersion(
            LoginVersionPacket packet,
            java.util.function.Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientVersionGate.checkServerVersion(
                        packet.serverVersion(), context.getNetworkManager())));
        context.setPacketHandled(true);
    }
}
