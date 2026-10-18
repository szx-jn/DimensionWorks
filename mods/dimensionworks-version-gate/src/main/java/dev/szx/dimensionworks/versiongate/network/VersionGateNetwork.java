package dev.szx.dimensionworks.versiongate.network;

import dev.szx.dimensionworks.versiongate.DimensionWorksVersionGate;
import dev.szx.dimensionworks.versiongate.PackVersion;
import dev.szx.dimensionworks.versiongate.client.ClientVersionGate;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
                        new LoginVersionPacket(PackVersion.readPackVersion(), 0))))
                .add();
    }

    private static void handleLoginVersion(
            LoginVersionPacket packet,
            java.util.function.Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            String localText = PackVersion.readLocalVersion();
            PackVersion local = PackVersion.parse(localText);
            PackVersion server = PackVersion.parse(packet.serverVersion());
            if (server.compareTo(PackVersion.UNKNOWN) <= 0) {
                DimensionWorksVersionGate.LOGGER.warn(
                        "Server sent an invalid DimensionWorks version: {}", packet.serverVersion());
                return;
            }
            if (local.compareTo(server) < 0) {
                ClientVersionGate.setMismatch(localText, packet.serverVersion());
                context.getNetworkManager().disconnect(Component.literal("DW_VERSION_TOO_OLD"));
            }
        });
        context.setPacketHandled(true);
    }
}
