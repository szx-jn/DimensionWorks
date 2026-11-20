package dev.szx.dimensionworks.transfer.network;

import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.dispatch.DispatchController;
import dev.szx.dimensionworks.transfer.handler.TransferController;
import dev.szx.dimensionworks.transfer.tutorial.TutorialManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Direction-specific native payloads; dispatch authorization stays on the server. */
public final class TransferNetwork {
    private TransferNetwork() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("4");
        registrar.playToServer(TransferToggleRequest.TYPE, TransferToggleRequest.CODEC, (packet,context)->{
            if(context.player() instanceof ServerPlayer player && !TutorialManager.locked(player))TransferController.useFromKey(player);
        });
        registrar.playToServer(ChooseDestination.TYPE, ChooseDestination.CODEC, (packet,context)->{
            if(context.player() instanceof ServerPlayer player)DispatchController.send(player,packet.pos,packet.route);
        });
        registrar.playToServer(CloseDispatch.TYPE, CloseDispatch.CODEC, (packet,context)->{
            if(context.player() instanceof ServerPlayer player)DispatchController.close(player);
        });
        registrar.playToClient(OpenDispatch.TYPE, OpenDispatch.CODEC, (packet,context)->TransferClientPayloads.open(packet));
        registrar.playToClient(TutorialState.TYPE, TutorialState.CODEC, (packet,context)->TransferClientPayloads.tutorial(packet));
        registrar.playToClient(ChainRide.TYPE, ChainRide.CODEC, (packet,context)->TransferClientPayloads.chainRide(packet));
        registrar.playToClient(CloseDispatchScreen.TYPE, CloseDispatchScreen.CODEC, (packet,context)->TransferClientPayloads.close());
    }
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WDTransfer.MOD_ID,name));
    }
    public static void sendToggleRequest(){PacketDistributor.sendToServer(new TransferToggleRequest());}
    public static void open(ServerPlayer player,BlockPos pos){PacketDistributor.sendToPlayer(player,new OpenDispatch(pos,DispatchController.alexCavesUnlocked(player)));}
    public static void sendChoice(BlockPos pos,String route){PacketDistributor.sendToServer(new ChooseDestination(pos,route));}
    public static void sendClose(){PacketDistributor.sendToServer(new CloseDispatch());}
    public static void tutorial(ServerPlayer player,boolean locked){PacketDistributor.sendToPlayer(player,new TutorialState(locked));}
    public static void chainRide(ServerPlayer player,BlockPos pos,float position,BlockPos connection){PacketDistributor.sendToPlayer(player,new ChainRide(pos,position,connection));}
    public static void closeScreen(ServerPlayer player){PacketDistributor.sendToPlayer(player,new CloseDispatchScreen());}

    public record OpenDispatch(BlockPos pos,boolean alexCavesUnlocked) implements CustomPacketPayload {
        public static final Type<OpenDispatch> TYPE=TransferNetwork.type("open_dispatch");
        public static final StreamCodec<RegistryFriendlyByteBuf,OpenDispatch> CODEC=StreamCodec.of((buf,p)->{buf.writeBlockPos(p.pos);buf.writeBoolean(p.alexCavesUnlocked);},buf->new OpenDispatch(buf.readBlockPos(),buf.readBoolean()));
        @Override public Type<OpenDispatch> type(){return TYPE;}
    }
    public record ChooseDestination(BlockPos pos,String route) implements CustomPacketPayload {
        public static final Type<ChooseDestination> TYPE=TransferNetwork.type("choose_destination");
        public static final StreamCodec<RegistryFriendlyByteBuf,ChooseDestination> CODEC=StreamCodec.of((buf,p)->{buf.writeBlockPos(p.pos);buf.writeUtf(p.route,64);},buf->new ChooseDestination(buf.readBlockPos(),buf.readUtf(64)));
        @Override public Type<ChooseDestination> type(){return TYPE;}
    }
    public record TutorialState(boolean locked) implements CustomPacketPayload {
        public static final Type<TutorialState> TYPE=TransferNetwork.type("tutorial_state");
        public static final StreamCodec<RegistryFriendlyByteBuf,TutorialState> CODEC=StreamCodec.of((buf,p)->buf.writeBoolean(p.locked),buf->new TutorialState(buf.readBoolean()));
        @Override public Type<TutorialState> type(){return TYPE;}
    }
    public record ChainRide(BlockPos pos,float position,BlockPos connection) implements CustomPacketPayload {
        public static final Type<ChainRide> TYPE=TransferNetwork.type("chain_ride");
        public static final StreamCodec<RegistryFriendlyByteBuf,ChainRide> CODEC=StreamCodec.of((buf,p)->{
            buf.writeBlockPos(p.pos);buf.writeFloat(p.position);buf.writeBoolean(p.connection!=null);if(p.connection!=null)buf.writeBlockPos(p.connection);
        },buf->new ChainRide(buf.readBlockPos(),buf.readFloat(),buf.readBoolean()?buf.readBlockPos():null));
        @Override public Type<ChainRide> type(){return TYPE;}
    }
    public record CloseDispatch() implements CustomPacketPayload {
        public static final Type<CloseDispatch> TYPE=TransferNetwork.type("close_dispatch");
        public static final StreamCodec<RegistryFriendlyByteBuf,CloseDispatch> CODEC=StreamCodec.unit(new CloseDispatch());
        @Override public Type<CloseDispatch> type(){return TYPE;}
    }
    public record CloseDispatchScreen() implements CustomPacketPayload {
        public static final Type<CloseDispatchScreen> TYPE=TransferNetwork.type("close_dispatch_screen");
        public static final StreamCodec<RegistryFriendlyByteBuf,CloseDispatchScreen> CODEC=StreamCodec.unit(new CloseDispatchScreen());
        @Override public Type<CloseDispatchScreen> type(){return TYPE;}
    }
}
