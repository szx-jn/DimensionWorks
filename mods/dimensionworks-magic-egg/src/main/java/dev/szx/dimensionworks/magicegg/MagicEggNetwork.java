package dev.szx.dimensionworks.magicegg;

import dev.szx.dimensionworks.magicegg.client.MagicEggScreen;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public final class MagicEggNetwork {
    // 协议中的集合上限，用于限制网络包分配；超过此规模的效果注册表不支持。
    private static final int MAX_EFFECTS = 4096;

    record Open(UUID token, List<String> catalogue, boolean hasPrevious, List<String> previous) implements CustomPacketPayload {
        static final Type<Open> TYPE = new Type<>(id("open"));
        static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of((buffer, message) -> {
            buffer.writeUUID(message.token());
            writeIds(buffer, message.catalogue());
            buffer.writeBoolean(message.hasPrevious());
            writeIds(buffer, message.previous());
        }, buffer -> new Open(buffer.readUUID(), readIds(buffer), buffer.readBoolean(), readIds(buffer)));
        @Override public Type<Open> type() { return TYPE; }
    }

    record Submit(UUID token, boolean reuse, List<String> selected) implements CustomPacketPayload {
        static final Type<Submit> TYPE = new Type<>(id("submit"));
        static final StreamCodec<RegistryFriendlyByteBuf, Submit> CODEC = StreamCodec.of((buffer, message) -> {
            buffer.writeUUID(message.token());
            buffer.writeBoolean(message.reuse());
            writeIds(buffer, message.selected());
        }, buffer -> new Submit(buffer.readUUID(), buffer.readBoolean(), readIds(buffer)));
        @Override public Type<Submit> type() { return TYPE; }
    }

    record Cancel(UUID token) implements CustomPacketPayload {
        static final Type<Cancel> TYPE = new Type<>(id("cancel"));
        static final StreamCodec<RegistryFriendlyByteBuf, Cancel> CODEC = StreamCodec.of(
                (buffer, message) -> buffer.writeUUID(message.token()), buffer -> new Cancel(buffer.readUUID()));
        @Override public Type<Cancel> type() { return TYPE; }
    }

    private MagicEggNetwork() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DimensionWorksMagicEgg.MOD_ID, path);
    }

    public static void register(IEventBus modBus, Dist dist) {
        IPayloadHandler<Open> openHandler = dist == Dist.CLIENT
                ? (message, context) -> MagicEggScreen.open(message.token(), message.catalogue(),
                        message.hasPrevious(), message.previous())
                : (message, context) -> { throw new IllegalStateException("Client payload received on a dedicated server"); };
        modBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("2");
            registrar.playToClient(Open.TYPE, Open.CODEC, openHandler);
            registrar.playToServer(Submit.TYPE, Submit.CODEC, (message, context) ->
                    MagicEggSelection.submit((ServerPlayer) context.player(), message.token(), message.reuse(), message.selected()));
            registrar.playToServer(Cancel.TYPE, Cancel.CODEC, (message, context) ->
                    MagicEggSelection.cancel((ServerPlayer) context.player(), message.token()));
        });
    }

    private static void writeIds(FriendlyByteBuf buffer, List<String> ids) {
        if (ids.size() > MAX_EFFECTS) throw new IllegalArgumentException("Too many magic egg effects");
        buffer.writeVarInt(ids.size());
        ids.forEach(id -> buffer.writeResourceLocation(ResourceLocation.parse(id)));
    }

    private static List<String> readIds(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_EFFECTS) throw new DecoderException("Invalid magic egg effect count");
        List<String> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) ids.add(buffer.readResourceLocation().toString());
        return List.copyOf(ids);
    }

    public static void open(ServerPlayer player, SelectionSession session) {
        PacketDistributor.sendToPlayer(player, new Open(session.token(),
                session.catalogue(), session.hasPrevious(), session.previous()));
    }

    public static void submit(UUID token, boolean reuse, List<String> selected) {
        PacketDistributor.sendToServer(new Submit(token, reuse, selected));
    }

    public static void cancel(UUID token) {
        PacketDistributor.sendToServer(new Cancel(token));
    }
}
