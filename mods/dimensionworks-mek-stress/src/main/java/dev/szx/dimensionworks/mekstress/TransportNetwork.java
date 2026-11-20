package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Menu-scoped settings: the server checks distance, identity and channel ownership. */
public final class TransportNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("3");
        registrar.playToServer(OpenCloudDisk.TYPE, OpenCloudDisk.CODEC, (packet, context) -> {
            var player = (ServerPlayer) context.player();
            if (player.isAlive() && !player.isSpectator() && player.containerMenu == player.inventoryMenu) {
                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, owner) -> new dev.szx.dimensionworks.mekstress.memory.CloudDiskMenu(id, inventory),
                    net.minecraft.network.chat.Component.translatable("gui.dimensionworks_mek_stress.cloud_title")));
            }
        });
        registrar.playToServer(Coefficient.TYPE, Coefficient.CODEC, (packet, context) -> {
            var player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof CrossDimensionTransportMenu menu && menu.containerId == packet.menuId())
                menu.setCoefficient(player, packet.value());
        });
        registrar.playToServer(Channel.TYPE, Channel.CODEC, (packet, context) -> {
            var player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof CrossDimensionTransportMenu menu && menu.containerId == packet.menuId())
                menu.setChannel(player, packet.value());
        });
    }
    private record Coefficient(int menuId, float value) implements CustomPacketPayload {
        static final Type<Coefficient> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DimensionWorksMekStress.MOD_ID, "transport_coefficient"));
        static final StreamCodec<RegistryFriendlyByteBuf, Coefficient> CODEC = StreamCodec.of(
            (b, p) -> { b.writeInt(p.menuId()); b.writeFloat(p.value()); }, b -> new Coefficient(b.readInt(), b.readFloat()));
        @Override public Type<Coefficient> type() { return TYPE; }
    }
    private record Channel(int menuId, int value) implements CustomPacketPayload {
        static final Type<Channel> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DimensionWorksMekStress.MOD_ID, "transport_channel"));
        static final StreamCodec<RegistryFriendlyByteBuf, Channel> CODEC = StreamCodec.of(
            (b, p) -> { b.writeInt(p.menuId()); b.writeInt(p.value()); }, b -> new Channel(b.readInt(), b.readInt()));
        @Override public Type<Channel> type() { return TYPE; }
    }
    private record OpenCloudDisk() implements CustomPacketPayload {
        static final Type<OpenCloudDisk> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DimensionWorksMekStress.MOD_ID, "open_cloud_disk"));
        static final StreamCodec<RegistryFriendlyByteBuf, OpenCloudDisk> CODEC = StreamCodec.unit(new OpenCloudDisk());
        @Override public Type<OpenCloudDisk> type() { return TYPE; }
    }
    public static void setChannel(int menuId, int value) { PacketDistributor.sendToServer(new Channel(menuId, value)); }
    public static void setCoefficient(int menuId, float value) { PacketDistributor.sendToServer(new Coefficient(menuId, value)); }
    public static void openCloudDisk() { PacketDistributor.sendToServer(new OpenCloudDisk()); }
    private TransportNetwork() {}
}
