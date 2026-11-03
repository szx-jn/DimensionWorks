package dev.szx.dimensionworks.spectatortoggle.network;

import dev.szx.dimensionworks.spectatortoggle.DimensionWorksSpectatorToggle;
import dev.szx.dimensionworks.spectatortoggle.handler.SpectatorToggleController;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SpectatorToggleNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final int TOGGLE_REQUEST_ID = 0;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DimensionWorksSpectatorToggle.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private SpectatorToggleNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                TOGGLE_REQUEST_ID,
                SpectatorToggleRequest.class,
                (message, buffer) -> {
                },
                buffer -> new SpectatorToggleRequest(),
                SpectatorToggleNetwork::handleToggleRequest,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToggleRequest() {
        CHANNEL.sendToServer(new SpectatorToggleRequest());
    }

    private static void handleToggleRequest(
            SpectatorToggleRequest message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> SpectatorToggleController.toggle(player));
        }
        context.setPacketHandled(true);
    }
}
