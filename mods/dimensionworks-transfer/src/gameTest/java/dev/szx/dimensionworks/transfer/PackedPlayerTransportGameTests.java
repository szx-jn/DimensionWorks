package dev.szx.dimensionworks.transfer;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import dev.szx.dimensionworks.transfer.compat.*;
import dev.szx.dimensionworks.transfer.handler.TransferEvents;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

@GameTestHolder(WDTransfer.MOD_ID)
@PrefixGameTestTemplate(false)
public class PackedPlayerTransportGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "ParcelTest"));
        p.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), p);
        return p;
    }
    private static void pack(ServerPlayer p) {
        try {
            Class.forName("de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState")
                .getMethod("setPacked",ServerPlayer.class,boolean.class,boolean.class).invoke(null,p,true,false);
        } catch (Exception e) { throw new AssertionError(e); }
    }
    private static void close(ServerPlayer p) {
        ServerChainConveyorHandler.hangingPlayers.removeInt(p.getUUID());
        p.discard();
    }

    @GameTest(template="empty")
    public static void chainParcelCrossesWallAndUnpackingRestoresCollision(GameTestHelper h) {
        var p = player(h);
        BlockPos start = h.absolutePos(new BlockPos(2,2,2));
        for (int y=0;y<3;y++) h.setBlock(new BlockPos(4,2+y,2),Blocks.STONE);
        try {
            pack(p);
            h.assertTrue(p.getTags().contains(PackedPlayerTransport.TAG), "Packing must immediately add the tag");
            p.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);
            p.move(MoverType.SELF,new Vec3(2,0,0));
            h.assertTrue(p.getX() < start.getX()+2, "Ground parcel must still collide");
            p.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);
            var oldBox = p.getBoundingBox();
            ServerChainConveyorHandler.hangingPlayers.put(p.getUUID(),20);
            p.move(MoverType.SELF,new Vec3(2,0,0));
            h.assertTrue(Math.abs(p.getX()-(start.getX()+2.5))<1e-6, "Chain parcel must reach its position inside the wall");
            try {
                var m = ServerGamePacketListenerImpl.class.getDeclaredMethod("isPlayerCollidingWithAnythingNew", net.minecraft.world.level.LevelReader.class,net.minecraft.world.phys.AABB.class,double.class,double.class,double.class);
                m.setAccessible(true);
                h.assertTrue(!(boolean)m.invoke(p.connection,h.getLevel(),oldBox,p.getX(),p.getY(),p.getZ()), "Server must accept the chain movement");
            } catch (Exception e) { throw new AssertionError(e); }
            h.assertTrue(!p.noPhysics, "No permanent noclip flag may remain");
            h.assertTrue(PlayerParcel.unpack(p), "CMP unpack operation must work");
            h.assertTrue(!p.getTags().contains(PackedPlayerTransport.TAG), "Unpacking must immediately remove tag");
            h.assertTrue(!ServerChainConveyorHandler.hangingPlayers.containsKey(p.getUUID()), "Unpacking must immediately dismount");
            p.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);
            p.move(MoverType.SELF,new Vec3(2,0,0));
            h.assertTrue(p.getX()<start.getX()+2, "Unpacked player must collide again");
            p.addTag(PackedPlayerTransport.TAG);
            h.assertTrue(!PackedPlayerTransport.ignoresBlocks(p) && !p.getTags().contains(PackedPlayerTransport.TAG), "Stale tag must be repaired");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void packagerIntakeUnpacksPlayerAndPreservesInventory(GameTestHelper h) {
        var p=player(h);
        BlockPos pos=h.absolutePos(new BlockPos(3,2,3));
        h.getLevel().setBlockAndUpdate(pos,AllBlocks.PACKAGER.getDefaultState().setValue(PackagerBlock.FACING,Direction.EAST));
        var machine=(PackagerBlockEntity)h.getLevel().getBlockEntity(pos);
        try {
            pack(p);
            p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));
            p.setPos(pos.getX()-.5,pos.getY(),pos.getZ()+.5);
            PackedPlayerInteractions.tick(p);
            h.assertTrue(PlayerParcel.isPacked(p), "Back of machine must not unpack");
            p.setPos(pos.getX()+1.3,pos.getY(),pos.getZ()+.5);
            machine.animationTicks=10;
            PackedPlayerInteractions.tick(p);
            h.assertTrue(PlayerParcel.isPacked(p), "Busy machine must not unpack");
            machine.animationTicks=0;
            ServerChainConveyorHandler.hangingPlayers.put(p.getUUID(),20);
            PackedPlayerInteractions.tick(p);
            h.assertTrue(!PlayerParcel.isPacked(p) && !p.getTags().contains(PackedPlayerTransport.TAG), "Intake must immediately invoke CMP unpack");
            h.assertTrue(!ServerChainConveyorHandler.hangingPlayers.containsKey(p.getUUID()), "Machine must release chain ride");
            h.assertTrue(p.getInventory().getItem(0).is(Items.DIAMOND) && p.getInventory().getItem(0).getCount()==7, "Machine must preserve inventory");
            h.assertTrue(machine.heldBox.isEmpty(), "Machine must not receive a fake player item");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void parcelFallIsIgnoredAndDamageUnpacksRatherThanDropsInventory(GameTestHelper h) {
        var p=player(h);
        try {
            pack(p);
            var fall=new LivingAttackEvent(p,p.damageSources().fall(),10);
            TransferEvents.parcelDamage(fall);
            h.assertTrue(fall.isCanceled() && PlayerParcel.isPacked(p), "Package fall must not harm or unpack");
            var hit=new LivingAttackEvent(p,p.damageSources().generic(),10);
            TransferEvents.parcelDamage(hit);
            h.assertTrue(hit.isCanceled() && !PlayerParcel.isPacked(p), "Ordinary damage must pop package using CMP unpack");
            h.assertTrue(p.isAlive(), "Popping the package must not remove the player");
            h.succeed();
        } finally { close(p); }
    }
}
