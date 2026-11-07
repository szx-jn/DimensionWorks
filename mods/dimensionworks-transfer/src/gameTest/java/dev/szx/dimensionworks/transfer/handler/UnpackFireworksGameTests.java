package dev.szx.dimensionworks.transfer.handler;

import com.mojang.authlib.GameProfile;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WDTransfer.MOD_ID)
@PrefixGameTestTemplate(false)
public class UnpackFireworksGameTests {
    private static class RecordingConnection extends Connection {
        final List<ClientboundLevelParticlesPacket> particles = new ArrayList<>();
        RecordingConnection() { super(PacketFlow.SERVERBOUND); }
        @Override public void send(Packet<?> packet, PacketSendListener listener) {
            if (packet instanceof ClientboundLevelParticlesPacket particle) particles.add(particle);
        }
    }

    private static ServerPlayer player(GameTestHelper h, RecordingConnection connection) {
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
            new GameProfile(UUID.randomUUID(), "UnpackTest"));
        player.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, player);
        player.setNoGravity(true);
        return player;
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayer> onlinePlayers(PlayerList list) {
        try {
            var field = PlayerList.class.getDeclaredField("playersByUUID");
            field.setAccessible(true);
            return (Map<UUID, ServerPlayer>) field.get(list);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void assertOrigin(GameTestHelper h, List<ClientboundLevelParticlesPacket> particles, Vec3 origin) {
        for (var particle : particles) {
            h.assertTrue(Math.abs(particle.getX() - origin.x) <= 1.5
                && Math.abs(particle.getY() - origin.y) <= 1.5
                && Math.abs(particle.getZ() - origin.z) <= 1.5,
                "Every burst must remain inside the original 3x3x3 unpack volume");
        }
    }

    private static void later(GameTestHelper h, long tick, Runnable check, Runnable cleanup) {
        h.runAtTickTime(tick, () -> {
            try { check.run(); }
            catch (RuntimeException | Error e) { cleanup.run(); throw e; }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void unpackTriplesDensityAndStaysAtOriginAfterMovingAndTeleporting(GameTestHelper h) {
        var sourcePackets = new RecordingConnection();
        var travelerPackets = new RecordingConnection();
        var traveler = player(h, travelerPackets);
        var observer = player(h, sourcePackets);
        var online = onlinePlayers(h.getLevel().getServer().getPlayerList());
        // High above other test volumes to record only this effect, without loading extra chunks.
        var position = Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))).add(0, 1000, 0);
        traveler.setPos(position);
        observer.setPos(position);
        online.put(traveler.getUUID(), traveler);
        h.getLevel().players().add(observer);
        Runnable cleanup = () -> {
            UnpackFireworks.cancel(traveler.getUUID());
            online.remove(traveler.getUUID());
            h.getLevel().players().remove(observer);
            traveler.serverLevel().removePlayerImmediately(traveler, Entity.RemovalReason.DISCARDED);
            observer.discard();
        };
        try {
            Class.forName("de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState")
                .getMethod("setPacked", ServerPlayer.class, boolean.class, boolean.class)
                .invoke(null, traveler, true, false);
            var origin = traveler.position().add(0, traveler.getBbHeight() / 2, 0);
            h.assertTrue(PlayerParcel.unpack(traveler), "Real CMP unpack must start the shared effect");
            h.assertTrue(sourcePackets.particles.size() == 36, "Initial burst must contain three times the old 12 particles");
            h.assertTrue(sourcePackets.particles.stream().filter(p -> p.getParticle().getType() == ParticleTypes.DUST).count() == 24,
                "Dust density must triple from 8 to 24");
            h.assertTrue(sourcePackets.particles.stream().filter(p -> p.getParticle().getType() == ParticleTypes.FIREWORK).count() == 12,
                "Firework density must triple from 4 to 12");
            assertOrigin(h, sourcePackets.particles, origin);
            traveler.setPos(position.add(100, 0, 0));
            later(h, 6, () -> {
                h.assertTrue(sourcePackets.particles.size() >= 72, "Original observer must receive later bursts after the player moves away");
                assertOrigin(h, sourcePackets.particles, origin);
                traveler.teleportTo(h.getLevel().getServer().getLevel(Level.NETHER), 0.5, 1000, 0.5, 0, 0);
                travelerPackets.particles.clear();
            }, cleanup);
            later(h, 12, () -> {
                h.assertTrue(sourcePackets.particles.size() >= 108, "Bursts must continue in the original dimension");
                assertOrigin(h, sourcePackets.particles, origin);
                h.assertTrue(travelerPackets.particles.isEmpty(), "Particles must not follow the unpacked player into the new dimension");
            }, cleanup);
            later(h, 65, () -> {
                try {
                    h.assertTrue(sourcePackets.particles.size() == 432, "Effect must emit 12 bursts and expire after 60 ticks");
                    assertOrigin(h, sourcePackets.particles, origin);
                    h.succeed();
                } finally { cleanup.run(); }
            }, cleanup);
        } catch (ReflectiveOperationException | RuntimeException | Error e) {
            cleanup.run();
            throw new AssertionError(e);
        }
    }
}
