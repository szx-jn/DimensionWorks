package dev.szx.dimensionworks.rpmlimit;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(DimensionWorksRpmLimit.MOD_ID)
@PrefixGameTestTemplate(false)
public class GearHeartEffectGameTests {
    private static class RecordingConnection extends Connection {
        final ArrayList<Packet<?>> packets = new ArrayList<>();
        RecordingConnection() { super(PacketFlow.SERVERBOUND); }
        @Override public void send(Packet<?> packet, PacketSendListener listener) { packets.add(packet); }
    }
    private static ServerPlayer equipped(GameTestHelper h, RecordingConnection connection) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"HeartTest"));
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,p);
        h.assertTrue(GearHeartEffects.ensureGearHeartSlots(p), "Test player must receive the existing Gear Heart slot/item");
        GearHeartEffects.tickPlayer(p);
        return p;
    }
    private static void close(ServerPlayer p) {
        GearHeartEffects.clearPlayer(p);
        p.serverLevel().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }
    @GameTest(template="empty")
    public static void teleportResendsInfiniteEffectsAndReachAfterRespawn(GameTestHelper h) {
        var connection=new RecordingConnection();var p=equipped(h,connection);
        var haste=p.getEffect(MobEffects.DIG_SPEED);
        var night=p.getEffect(MobEffects.NIGHT_VISION);
        try {
            connection.packets.clear();
            var destination=h.getLevel().getServer().getLevel(Level.NETHER);
            p.teleportTo(destination,0.5,80,0.5,0,0);
            h.assertTrue(p.serverLevel()==destination, "Actual cross-dimension command teleport must succeed");
            h.assertTrue(p.getEffect(MobEffects.DIG_SPEED)==haste && p.getEffect(MobEffects.NIGHT_VISION)==night,
                "Sync must preserve the existing infinite server instances");
            int respawn=-1, effects=0, attrs=0;
            for(int i=0;i<connection.packets.size();i++) {
                var packet=connection.packets.get(i);
                if(packet instanceof ClientboundRespawnPacket) respawn=i;
                if(packet instanceof ClientboundUpdateMobEffectPacket) {
                    h.assertTrue(respawn>=0 && i>respawn,"Effects must be resent after the client respawn packet"); effects++;
                }
                if(packet instanceof ClientboundUpdateAttributesPacket && i>respawn && respawn>=0) attrs++;
            }
            h.assertTrue(effects>=2 && attrs>=1,"Both heart effects and reach attributes must be resent");
            h.assertTrue(!haste.isVisible() && !night.isVisible() && haste.showIcon() && night.showIcon(),"Heart particles must be hidden while icons remain");
            h.assertTrue(p.getAttribute(ForgeMod.BLOCK_REACH.get()).getValue()>=6.5,"Reach bonus must survive teleport");
            h.succeed();
        } finally { close(p); }
    }
    @GameTest(template="empty")
    public static void hidesOldHeartParticlesAndPreservesStrongerExternalEffect(GameTestHelper h) {
        var connection=new RecordingConnection();var p=equipped(h,connection);
        try {
            p.removeEffect(MobEffects.DIG_SPEED);
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,-1,1,false,true,true));
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,200,2,false,true,true));
            GearHeartEffects.tickPlayer(p);
            h.assertTrue(!p.getEffect(MobEffects.DIG_SPEED).isVisible(),"Existing infinite heart effect must stop emitting particles");
            var stronger=p.getEffect(MobEffects.NIGHT_VISION);
            h.assertTrue(stronger.getAmplifier()==2 && stronger.getDuration()==200 && stronger.isVisible(),"Stronger external potion must retain level, duration and particles");
            p.removeEffect(MobEffects.NIGHT_VISION);
            GearHeartEffects.tickPlayer(p);
            h.assertTrue(p.getEffect(MobEffects.NIGHT_VISION).isInfiniteDuration() && !p.getEffect(MobEffects.NIGHT_VISION).isVisible(),"Lost effect must be restored without particles");
            h.succeed();
        } finally { close(p); }
    }
}
