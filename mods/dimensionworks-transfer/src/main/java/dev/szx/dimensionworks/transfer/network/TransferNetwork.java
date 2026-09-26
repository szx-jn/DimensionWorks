package dev.szx.dimensionworks.transfer.network;

import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.handler.TransferController;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Forge network channel for the player-controlled transfer key. */
public final class TransferNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final int TOGGLE_REQUEST_ID = 0;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(WDTransfer.MOD_ID, "toggle_transfer"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private TransferNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                TOGGLE_REQUEST_ID,
                TransferToggleRequest.class,
                (message, buffer) -> {
                },
                buffer -> new TransferToggleRequest(),
                TransferNetwork::handleToggleRequest,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToggleRequest() {
        CHANNEL.sendToServer(new TransferToggleRequest());
    }

    private static void handleToggleRequest(
            TransferToggleRequest message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> TransferController.useFromKey(player));
        }
        context.setPacketHandled(true);
    }
}
