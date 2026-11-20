package dev.szx.dimensionworks.spectatortoggle.network;

import dev.szx.dimensionworks.spectatortoggle.DimensionWorksSpectatorToggle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Empty client request; the server determines the player and return point. */
public record SpectatorToggleRequest() implements CustomPacketPayload {
    public static final SpectatorToggleRequest INSTANCE = new SpectatorToggleRequest();
    public static final Type<SpectatorToggleRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            DimensionWorksSpectatorToggle.MOD_ID, "toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpectatorToggleRequest> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<SpectatorToggleRequest> type() {
        return TYPE;
    }
}
