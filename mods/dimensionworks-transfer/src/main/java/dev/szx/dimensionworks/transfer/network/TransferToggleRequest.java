package dev.szx.dimensionworks.transfer.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Empty client-to-server request to toggle the transfer dimension. */
public record TransferToggleRequest() implements CustomPacketPayload {
    public static final Type<TransferToggleRequest> TYPE=TransferNetwork.type("transfer_toggle");
    public static final StreamCodec<RegistryFriendlyByteBuf,TransferToggleRequest> CODEC=StreamCodec.unit(new TransferToggleRequest());
    @Override public Type<TransferToggleRequest> type(){return TYPE;}
}
