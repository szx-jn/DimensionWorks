package dev.szx.dimensionworks.transfer.elastic;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ElasticNetwork {
    private ElasticNetwork() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("3");
        registrar.playToServer(Input.TYPE,Input.CODEC,(packet,context)->{
            if(context.player() instanceof ServerPlayer player) {
                if(packet.cancel)ElasticParcelState.resetCharge(player);
                else ElasticParcelState.input(player,packet.down,packet.pitch,packet.yaw);
            }
        });
        registrar.playToClient(State.TYPE,State.CODEC,(packet,context)->dev.szx.dimensionworks.transfer.client.ElasticParcelClient.receive(packet.id,packet.uuid,packet.elastic));
        registrar.playToClient(Launch.TYPE,Launch.CODEC,(packet,context)->dev.szx.dimensionworks.transfer.client.ElasticParcelClient.launch(packet.uuid,packet.velocity));
    }
    public static void sync(ServerPlayer player){PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,snapshot(player));}
    public static void syncTo(ServerPlayer player,ServerPlayer observer){PacketDistributor.sendToPlayer(observer,snapshot(player));}
    private static State snapshot(ServerPlayer player){return new State(player.getId(),player.getUUID(),ElasticParcelState.isElastic(player));}
    public static void sendInput(boolean down,float pitch,float yaw,boolean cancel){PacketDistributor.sendToServer(new Input(down,pitch,yaw,cancel));}
    public static void launch(ServerPlayer player){PacketDistributor.sendToPlayer(player,new Launch(player.getUUID(),player.getDeltaMovement()));}

    public record State(int id, UUID uuid, boolean elastic) implements CustomPacketPayload {
        public static final Type<State> TYPE=TransferNetwork.type("elastic_state");
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((buf,p)->p.write(buf),State::new);
        @Override public Type<State> type(){return TYPE;}
        public State(FriendlyByteBuf buffer) { this(buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean()); }
        public void write(FriendlyByteBuf buffer) { buffer.writeVarInt(id); buffer.writeUUID(uuid); buffer.writeBoolean(elastic); }
    }

    public record Input(boolean down, float pitch, float yaw, boolean cancel) implements CustomPacketPayload {
        public static final Type<Input> TYPE=TransferNetwork.type("elastic_input");
        public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=StreamCodec.of((buf,p)->p.write(buf),Input::new);
        @Override public Type<Input> type(){return TYPE;}
        public Input(FriendlyByteBuf buffer) { this(buffer.readBoolean(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()); }
        public void write(FriendlyByteBuf buffer) { buffer.writeBoolean(down); buffer.writeFloat(pitch); buffer.writeFloat(yaw); buffer.writeBoolean(cancel); }
    }

    /** Full launches exceed the vanilla motion packet's 3.9 blocks/tick component cap. */
    public record Launch(UUID uuid, Vec3 velocity) implements CustomPacketPayload {
        public static final Type<Launch> TYPE=TransferNetwork.type("elastic_launch");
        public static final StreamCodec<RegistryFriendlyByteBuf,Launch> CODEC=StreamCodec.of((buf,p)->p.write(buf),Launch::new);
        @Override public Type<Launch> type(){return TYPE;}
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
