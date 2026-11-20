package dev.szx.dimensionworks.versiongate.network;

import dev.szx.dimensionworks.versiongate.DimensionWorksVersionGate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Pack version sent before the configuration phase can finish. */
public record LoginVersionPacket(String serverVersion) implements CustomPacketPayload {
    public static final Type<LoginVersionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            DimensionWorksVersionGate.MOD_ID, "server_version"));
    public static final StreamCodec<FriendlyByteBuf, LoginVersionPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> buffer.writeUtf(packet.serverVersion(), 32),
            buffer -> new LoginVersionPacket(buffer.readUtf(32)));

    @Override
    public Type<LoginVersionPacket> type() {
        return TYPE;
    }
}
