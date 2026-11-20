package dev.szx.dimensionworks.spectatortoggle.network;

import dev.szx.dimensionworks.spectatortoggle.handler.SpectatorToggleController;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SpectatorToggleNetwork {
    private SpectatorToggleNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(SpectatorToggleRequest.TYPE, SpectatorToggleRequest.STREAM_CODEC,
                (packet, context) -> SpectatorToggleController.toggle((ServerPlayer) context.player()));
    }

    public static void sendToggleRequest() {
        PacketDistributor.sendToServer(SpectatorToggleRequest.INSTANCE);
    }
}
