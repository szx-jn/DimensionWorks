package dev.szx.dimensionworks.transfer.elastic;

import dev.szx.dimensionworks.transfer.WDTransfer;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ElasticNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(WDTransfer.MOD_ID, "elastic"), () -> "2", "2"::equals, "2"::equals);

    private ElasticNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, State.class, State::write, State::new, ElasticNetwork::state,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, Input.class, Input::write, Input::new, ElasticNetwork::input,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, Launch.class, Launch::write, Launch::new, ElasticNetwork::launch,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), snapshot(player));
    }

    public static void syncTo(ServerPlayer player, ServerPlayer observer) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> observer), snapshot(player));
    }

    private static State snapshot(ServerPlayer player) {
        return new State(player.getId(), player.getUUID(), ElasticParcelState.isElastic(player));
    }

    public static void sendInput(boolean down, float pitch, float yaw, boolean cancel) { CHANNEL.sendToServer(new Input(down, pitch, yaw, cancel)); }

    public static void launch(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Launch(player.getUUID(), player.getDeltaMovement()));
    }

    private static void launch(Launch packet, Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> dev.szx.dimensionworks.transfer.client.ElasticParcelClient.launch(packet.uuid, packet.velocity)));
        ctx.setPacketHandled(true);
    }

    private static void state(State packet, Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> dev.szx.dimensionworks.transfer.client.ElasticParcelClient.receive(packet.id, packet.uuid, packet.elastic)));
        ctx.setPacketHandled(true);
    }

    private static void input(Input packet, Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                if (packet.cancel) ElasticParcelState.resetCharge(player);
                else ElasticParcelState.input(player, packet.down, packet.pitch, packet.yaw);
            }
        });
        ctx.setPacketHandled(true);
    }

    public record State(int id, UUID uuid, boolean elastic) {
        public State(FriendlyByteBuf buffer) { this(buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean()); }
        public void write(FriendlyByteBuf buffer) { buffer.writeVarInt(id); buffer.writeUUID(uuid); buffer.writeBoolean(elastic); }
    }

    public record Input(boolean down, float pitch, float yaw, boolean cancel) {
        public Input(FriendlyByteBuf buffer) { this(buffer.readBoolean(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()); }
        public void write(FriendlyByteBuf buffer) { buffer.writeBoolean(down); buffer.writeFloat(pitch); buffer.writeFloat(yaw); buffer.writeBoolean(cancel); }
    }

    /** Full launches exceed the vanilla motion packet's 3.9 blocks/tick component cap. */
    public record Launch(UUID uuid, Vec3 velocity) {
        public Launch(FriendlyByteBuf buffer) {
            this(buffer.readUUID(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
        }
        public void write(FriendlyByteBuf buffer) {
            buffer.writeUUID(uuid);
            buffer.writeDouble(velocity.x);
            buffer.writeDouble(velocity.y);
            buffer.writeDouble(velocity.z);
        }
    }
}
