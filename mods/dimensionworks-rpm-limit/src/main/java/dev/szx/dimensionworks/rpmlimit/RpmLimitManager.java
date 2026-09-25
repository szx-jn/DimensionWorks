package dev.szx.dimensionworks.rpmlimit;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpmLimitManager {
    private static final String PLAYER_KEY = "dimensionworks_rpm_limit";
    private static final String OWNER_KEY = "dimensionworks_owner";
    private static final String INIT_KEY = "dimensionworks_rpm_initialized";
    private static final String MISMATCH_KEY = "dimensionworks_speed_mismatch";

    private static final Map<UUID, Integer> CACHE = new ConcurrentHashMap<>();
    private static long last = Long.MIN_VALUE;

    public static void init(Player p) {
        CompoundTag d = p.getPersistentData();
        if (!d.getBoolean(INIT_KEY)) {
            d.putInt(PLAYER_KEY, RpmLimitConfig.DEFAULT_RPM.get());
            d.putBoolean(INIT_KEY, true);
        }
        CACHE.put(p.getUUID(), sanitize(d.getInt(PLAYER_KEY)));
    }

    public static int get(UUID id) {
        return CACHE.getOrDefault(id, RpmLimitConfig.DEFAULT_RPM.get());
    }

    public static int getLimit(UUID id) {
        return get(id);
    }

    public static void setLimit(ServerPlayer p, int rpm) {
        int v = sanitize(rpm);
        p.getPersistentData().putInt(PLAYER_KEY, v);
        p.getPersistentData().putBoolean(INIT_KEY, true);
        CACHE.put(p.getUUID(), v);
    }

    public static void refresh(Iterable<ServerPlayer> ps, long tick) {
        int i = Math.max(1, RpmLimitConfig.CACHE_INTERVAL_TICKS.get());
        if (tick - last < i)
            return;
        last = tick;
        for (ServerPlayer p : ps) {
            int v = sanitize(p.getPersistentData().getInt(PLAYER_KEY));
            p.getPersistentData().putInt(PLAYER_KEY, v);
            CACHE.put(p.getUUID(), v);
        }
    }

    /**
     * Caps the rotation a network's own generator is allowed to produce.
     *
     * <p>Only the driving source may be capped. Clamping a block that merely receives its speed
     * through propagation leaves it permanently slower than its neighbour, which makes Create's
     * {@code RotationPropagator.propagateNewSource} recurse into itself forever.
     */
    public static float clamp(KineticBlockEntity k, float requested) {
        if (k.getLevel() == null || k.getLevel().isClientSide)
            return requested;
        UUID o = owner(k);
        if (o == null)
            return requested;
        if (requested == 0)
            return requested;
        int l = get(o);
        return Math.abs(requested) > l ? Math.copySign(l, requested) : requested;
    }

    public static int clampInt(KineticBlockEntity k, int requested) {
        return (int) clamp(k, requested);
    }

    /**
     * Walks up the source chain looking for the block that carries an owner.
     *
     * <p>Kept iterative on purpose: the old recursive version added 32 stack frames to every
     * {@code setSpeed} call made during rotation propagation.
     */
    public static UUID owner(KineticBlockEntity k) {
        KineticBlockEntity cur = k;
        for (int i = 0; i < 32 && cur != null; i++) {
            CompoundTag d = cur.getPersistentData();
            if (d.hasUUID(OWNER_KEY))
                return d.getUUID(OWNER_KEY);
            if (!cur.hasSource() || cur.getLevel() == null)
                break;
            var be = cur.getLevel().getBlockEntity(cur.source);
            cur = be instanceof KineticBlockEntity s ? s : null;
        }
        return null;
    }

    public static void setOwner(KineticBlockEntity k, UUID u) {
        k.getPersistentData().putUUID(OWNER_KEY, u);
        k.setChanged();
    }

    public static boolean isSpeedMismatch(KineticBlockEntity k) {
        return k.getPersistentData().getBoolean(MISMATCH_KEY);
    }

    public static boolean setSpeedMismatch(KineticBlockEntity k, boolean mismatch) {
        CompoundTag data = k.getPersistentData();
        if (data.getBoolean(MISMATCH_KEY) == mismatch)
            return false;
        data.putBoolean(MISMATCH_KEY, mismatch);
        return true;
    }

    /**
     * Runs once per network sync, mirroring how Create recomputes overstress for a whole network.
     *
     * <p>If any member of the network spins faster than its owner's limit, the limit cannot be
     * honoured without splitting the network, so every member is flagged. Flagged members are
     * stalled by the mixins and recover the moment the offending machinery is removed, exactly
     * like an overstressed network recovering when its stress drops.
     */
    public static void evaluateNetwork(KineticNetwork network) {
        if (network == null || network.members == null || network.members.isEmpty())
            return;
        for (KineticBlockEntity member : network.members.keySet())
            if (member.getLevel() == null || member.getLevel().isClientSide)
                return;

        float fastest = 0;
        for (KineticBlockEntity member : network.members.keySet())
            fastest = Math.max(fastest, Math.abs(member.getTheoreticalSpeed()));

        boolean mismatch = false;
        if (fastest > 1) {
            for (KineticBlockEntity member : network.members.keySet()) {
                UUID o = owner(member);
                if (o != null && get(o) < fastest) {
                    mismatch = true;
                    break;
                }
            }
        }

        for (KineticBlockEntity member : network.members.keySet()) {
            if (setSpeedMismatch(member, mismatch)) {
                member.setChanged();
                member.sendData();
            }
        }
    }

    private static int sanitize(int v) {
        return Math.max(1, Math.min(v, Math.max(1, RpmLimitConfig.MAX_RPM.get())));
    }
}
