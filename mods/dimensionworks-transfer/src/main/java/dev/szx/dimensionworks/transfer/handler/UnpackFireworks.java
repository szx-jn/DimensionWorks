package dev.szx.dimensionworks.transfer.handler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Runs the shared unpack firework burst for every packed-player state transition. */
public final class UnpackFireworks {
    static final int DURATION_TICKS = 60;
    static final int BURST_INTERVAL_TICKS = 5;
    static final int DUST_PER_BURST = 24;
    static final int FIREWORK_PER_BURST = 12;
    private static final double HALF_EXTENT = 1.5D;
    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    private UnpackFireworks() {
    }

    public static void start(ServerPlayer player) {
        long now = player.server.overworld().getGameTime();
        Active active = new Active(player.serverLevel(), player.position().add(0, player.getBbHeight() / 2, 0),
            now, now + DURATION_TICKS, now + BURST_INTERVAL_TICKS);
        ACTIVE.put(player.getUUID(), active);
        spawnBurst(active, now);
        player.serverLevel().playSound(
            null,
            player.blockPosition(),
            SoundEvents.FIREWORK_ROCKET_BLAST,
            SoundSource.PLAYERS,
            0.75F,
            0.9F + player.getRandom().nextFloat() * 0.2F
        );
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Active>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Active> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Active active = entry.getValue();
            if (player == null || !player.isAlive() || now >= active.endTick()) {
                iterator.remove();
                continue;
            }
            if (now < active.nextBurstTick()) {
                continue;
            }
            spawnBurst(active, now);
            entry.setValue(active.withNextBurst(now + BURST_INTERVAL_TICKS));
        }
    }

    public static void cancel(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static void spawnBurst(Active active, long now) {
        ServerLevel level = active.level();
        Vec3 center = active.center();
        if (!level.hasChunkAt(BlockPos.containing(center))) return;
        long burstIndex = (now - active.startTick()) / BURST_INTERVAL_TICKS;
        for (int i = 0; i < DUST_PER_BURST; i++) {
            Vec3 offset = randomOffset(level);
            float hue = (burstIndex + i / (float) DUST_PER_BURST) % 1.0F;
            Vector3f color = hsvToRgb(hue);
            DustParticleOptions dust = new DustParticleOptions(
                color,
                0.75F + level.random.nextFloat() * 0.35F
            );
            level.sendParticles(dust, center.x + offset.x, center.y + offset.y, center.z + offset.z, 0, 0, 0, 0, 0);
        }
        for (int i = 0; i < FIREWORK_PER_BURST; i++) {
            Vec3 offset = randomOffset(level);
            Vec3 direction = offset.lengthSqr() < 1.0E-6D ? new Vec3(1, 0, 0) : offset.normalize();
            double speed = 0.055D + level.random.nextDouble() * 0.045D;
            level.sendParticles(
                ParticleTypes.FIREWORK,
                center.x + offset.x,
                center.y + offset.y,
                center.z + offset.z,
                0,
                direction.x,
                direction.y,
                direction.z,
                speed
            );
        }
    }

    private static Vec3 randomOffset(ServerLevel level) {
        return new Vec3(
            (level.random.nextDouble() * 2 - 1) * HALF_EXTENT,
            (level.random.nextDouble() * 2 - 1) * HALF_EXTENT,
            (level.random.nextDouble() * 2 - 1) * HALF_EXTENT
        );
    }

    private static Vector3f hsvToRgb(float hue) {
        float scaled = hue * 6.0F;
        int sector = (int) scaled;
        float fraction = scaled - sector;
        float value = 1.0F;
        float saturation = 0.85F;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - fraction * saturation);
        float t = value * (1.0F - (1.0F - fraction) * saturation);
        return switch (sector % 6) {
            case 0 -> new Vector3f(value, t, p);
            case 1 -> new Vector3f(q, value, p);
            case 2 -> new Vector3f(p, value, t);
            case 3 -> new Vector3f(p, q, value);
            case 4 -> new Vector3f(t, p, value);
            default -> new Vector3f(value, p, q);
        };
    }

    private record Active(ServerLevel level, Vec3 center, long startTick, long endTick, long nextBurstTick) {
        private Active withNextBurst(long nextBurstTick) {
            return new Active(level, center, startTick, endTick, nextBurstTick);
        }
    }
}
