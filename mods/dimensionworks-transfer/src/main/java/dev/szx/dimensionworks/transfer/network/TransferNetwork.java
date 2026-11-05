package dev.szx.dimensionworks.transfer.network;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.dispatch.DispatchController;
import dev.szx.dimensionworks.transfer.dispatch.DispatchScreen;
import dev.szx.dimensionworks.transfer.handler.TransferController;
import dev.szx.dimensionworks.transfer.tutorial.TutorialManager;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
public final class TransferNetwork {
 private static final String PROTOCOL="2";private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(WDTransfer.MOD_ID,"network"),()->PROTOCOL,PROTOCOL::equals,PROTOCOL::equals);
 private TransferNetwork(){}
 public static void register(){CHANNEL.registerMessage(0,TransferToggleRequest.class,(m,b)->{},b->new TransferToggleRequest(),TransferNetwork::toggle,Optional.of(NetworkDirection.PLAY_TO_SERVER));CHANNEL.registerMessage(1,OpenDispatch.class,(m,b)->b.writeBlockPos(m.pos),b->new OpenDispatch(b.readBlockPos()),TransferNetwork::open,Optional.of(NetworkDirection.PLAY_TO_CLIENT));CHANNEL.registerMessage(2,ChooseDestination.class,(m,b)->{b.writeBlockPos(m.pos);b.writeUtf(m.route);},b->new ChooseDestination(b.readBlockPos(),b.readUtf(64)),TransferNetwork::choose,Optional.of(NetworkDirection.PLAY_TO_SERVER));CHANNEL.registerMessage(3,TutorialState.class,(m,b)->b.writeBoolean(m.locked),b->new TutorialState(b.readBoolean()),TransferNetwork::tutorial,Optional.of(NetworkDirection.PLAY_TO_CLIENT));CHANNEL.registerMessage(4,ChainRide.class,(m,b)->{b.writeBlockPos(m.pos);b.writeFloat(m.position);b.writeBoolean(m.connection!=null);if(m.connection!=null)b.writeBlockPos(m.connection);},b->new ChainRide(b.readBlockPos(),b.readFloat(),b.readBoolean()?b.readBlockPos():null),TransferNetwork::chainRide,Optional.of(NetworkDirection.PLAY_TO_CLIENT));CHANNEL.registerMessage(5,CloseDispatch.class,(m,b)->{},b->new CloseDispatch(),TransferNetwork::close,Optional.of(NetworkDirection.PLAY_TO_SERVER));CHANNEL.registerMessage(6,CloseDispatchScreen.class,(m,b)->{},b->new CloseDispatchScreen(),TransferNetwork::closeScreen,Optional.of(NetworkDirection.PLAY_TO_CLIENT));}
 public static void sendToggleRequest(){CHANNEL.sendToServer(new TransferToggleRequest());}
 public static void open(ServerPlayer p,BlockPos pos){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new OpenDispatch(pos));}
 public static void sendChoice(BlockPos pos,String route){CHANNEL.sendToServer(new ChooseDestination(pos,route));}
 public static void sendClose(){CHANNEL.sendToServer(new CloseDispatch());}
 public static void tutorial(ServerPlayer p,boolean locked){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new TutorialState(locked));}
 public static void chainRide(ServerPlayer p,BlockPos pos,float position,BlockPos connection){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new ChainRide(pos,position,connection));}
 public static void closeScreen(ServerPlayer p){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new CloseDispatchScreen());}
 private static void toggle(TransferToggleRequest m,Supplier<NetworkEvent.Context> c){var x=c.get();var p=x.getSender();if(p!=null)x.enqueueWork(()->{if(!TutorialManager.locked(p))TransferController.useFromKey(p);});x.setPacketHandled(true);}
 private static void open(OpenDispatch m,Supplier<NetworkEvent.Context> c){var x=c.get();x.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->DispatchScreen.open(m.pos)));x.setPacketHandled(true);}
 private static void tutorial(TutorialState m,Supplier<NetworkEvent.Context> c){var x=c.get();x.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->{DispatchScreen.tutorial(m.locked);dev.szx.dimensionworks.transfer.client.TutorialOverlay.setLocked(m.locked);}));x.setPacketHandled(true);}
 private static void choose(ChooseDestination m,Supplier<NetworkEvent.Context> c){var x=c.get();var p=x.getSender();if(p!=null)x.enqueueWork(()->DispatchController.send(p,m.pos,m.route));x.setPacketHandled(true);}
 private static void chainRide(ChainRide m,Supplier<NetworkEvent.Context> c){var x=c.get();x.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler.embark(m.pos,m.position,m.connection)));x.setPacketHandled(true);}
 private static void close(CloseDispatch m,Supplier<NetworkEvent.Context> c){var x=c.get();var p=x.getSender();if(p!=null)x.enqueueWork(()->DispatchController.close(p));x.setPacketHandled(true);}
 private static void closeScreen(CloseDispatchScreen m,Supplier<NetworkEvent.Context> c){var x=c.get();x.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->net.minecraft.client.Minecraft.getInstance().setScreen(null)));x.setPacketHandled(true);}
 public record OpenDispatch(BlockPos pos){} public record ChooseDestination(BlockPos pos,String route){} public record TutorialState(boolean locked){} public record ChainRide(BlockPos pos,float position,BlockPos connection){} public record CloseDispatch(){} public record CloseDispatchScreen(){}
}
