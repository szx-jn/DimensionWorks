package dev.szx.dimensionworks.magicegg;

import dev.szx.dimensionworks.magicegg.client.MagicEggScreen;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class MagicEggNetwork {
    // 协议中的集合上限，用于限制网络包分配；超过此规模的效果注册表不支持。
    private static final int MAX_EFFECTS = 4096;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DimensionWorksMagicEgg.MOD_ID, "selection"),
            () -> "1", "1"::equals, "1"::equals);

    private record Open(UUID token, List<String> catalogue, boolean hasPrevious, List<String> previous) {}
    private record Submit(UUID token, boolean reuse, List<String> selected) {}
    private record Cancel(UUID token) {}

    private MagicEggNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Open.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {
                    buffer.writeUUID(message.token());
                    writeIds(buffer, message.catalogue());
                    buffer.writeBoolean(message.hasPrevious());
                    writeIds(buffer, message.previous());
                })
                .decoder(buffer -> new Open(buffer.readUUID(), readIds(buffer), buffer.readBoolean(), readIds(buffer)))
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> MagicEggScreen.open(message.token(), message.catalogue(),
                                message.hasPrevious(), message.previous())))
                .add();
        CHANNEL.messageBuilder(Submit.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> {
                    buffer.writeUUID(message.token());
                    buffer.writeBoolean(message.reuse());
                    writeIds(buffer, message.selected());
                })
                .decoder(buffer -> new Submit(buffer.readUUID(), buffer.readBoolean(), readIds(buffer)))
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) MagicEggSelection.submit(player, message.token(), message.reuse(), message.selected());
                }).add();
        CHANNEL.messageBuilder(Cancel.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> buffer.writeUUID(message.token()))
                .decoder(buffer -> new Cancel(buffer.readUUID()))
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) MagicEggSelection.cancel(player, message.token());
                }).add();
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
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Open(session.token(),
                session.catalogue(), session.hasPrevious(), session.previous()));
    }

    public static void submit(UUID token, boolean reuse, List<String> selected) {
        CHANNEL.sendToServer(new Submit(token, reuse, selected));
    }

    public static void cancel(UUID token) {
        CHANNEL.sendToServer(new Cancel(token));
    }
}
