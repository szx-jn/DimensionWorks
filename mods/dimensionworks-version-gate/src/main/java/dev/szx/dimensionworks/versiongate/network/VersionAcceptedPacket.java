package dev.szx.dimensionworks.versiongate.network;

import dev.szx.dimensionworks.versiongate.DimensionWorksVersionGate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Only sent after the client has accepted the server's pack version. */
public record VersionAcceptedPacket() implements CustomPacketPayload {
    public static final VersionAcceptedPacket INSTANCE = new VersionAcceptedPacket();
    public static final Type<VersionAcceptedPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            DimensionWorksVersionGate.MOD_ID, "version_accepted"));
    public static final StreamCodec<FriendlyByteBuf, VersionAcceptedPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<VersionAcceptedPacket> type() {
        return TYPE;
    }
}
