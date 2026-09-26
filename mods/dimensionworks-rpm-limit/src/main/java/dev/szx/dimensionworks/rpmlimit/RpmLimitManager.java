package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.mixin.ChunkMapAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpmLimitManager {
    private static final String PLAYER_KEY = "dimensionworks_rpm_limit";
    private static final String OWNER_KEY = "dimensionworks_owner";
    private static final String MACHINE_LIMIT_KEY = "dimensionworks_machine_rpm_limit";
    private static final String INIT_KEY = "dimensionworks_rpm_initialized";

    private static final String SYNC_OWNER_KEY = "DimensionWorksOwner";
    private static final String SYNC_MACHINE_LIMIT_KEY = "DimensionWorksMachineRpmLimit";

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
        applyLimitToOwned(p, v);
    }

    public static void refresh(Iterable<ServerPlayer> ps, long tick) {
        int i = Math.max(1, RpmLimitConfig.CACHE_INTERVAL_TICKS.get());
        if (tick - last < i)
            return;
        last = tick;
        for (ServerPlayer p : ps) {
            int v = sanitize(p.getPersistentData().getInt(PLAYER_KEY));
            p.getPersistentData().putInt(PLAYER_KEY, v);
            Integer previous = CACHE.put(p.getUUID(), v);
            if (previous == null || previous != v)
                applyLimitToOwned(p, v);
        }
    }

    /**
     * Caps the effective rotation a machine uses for work, movement and rendering.
     *
     * <p>This intentionally must not be used by Create's rotation propagation. Create compares
     * theoretical speeds while building a network, and changing that value makes a valid source
     * chain look inconsistent. The raw network speed stays intact; only consumers read this cap.
     */
    public static float clamp(KineticBlockEntity k, float requested) {
        if (requested == 0)
            return 0;
        return RpmLimitMath.cap(requested, limitFor(k));
    }

    /** Ratio from an unscaled network speed to the player-limited effective speed. */
    public static float effectiveScale(KineticBlockEntity k, float raw) {
        if (raw == 0)
            return 1;
        float effective = clamp(k, raw);
        return Math.min(1, Math.abs(effective / raw));
    }


    /**
     * Walks up the source chain looking for the block that carries an owner.
     *
     * <p>Kept iterative on purpose because consumers call this on every effective-speed read.
     */
    public static UUID owner(KineticBlockEntity k) {
        KineticBlockEntity cur = k;
        for (int i = 0; i < 32 && cur != null; i++) {
            UUID direct = directOwner(cur);
            if (direct != null)
                return direct;
            if (!cur.hasSource() || cur.getLevel() == null)
                break;
            BlockEntity blockEntity = cur.getLevel().getBlockEntity(cur.source);
            cur = blockEntity instanceof KineticBlockEntity source ? source : null;
        }
        return null;
    }

    private static UUID directOwner(KineticBlockEntity k) {
        CompoundTag data = k.getPersistentData();
        return data.hasUUID(OWNER_KEY) ? data.getUUID(OWNER_KEY) : null;
    }

    public static void setOwner(KineticBlockEntity k, UUID u) {
        k.getPersistentData().putUUID(OWNER_KEY, u);
        setMachineLimit(k, get(u));
        k.setChanged();
        k.sendData();
    }

    public static void onMachineLoaded(KineticBlockEntity k) {
        if (k.getLevel() == null || k.getLevel().isClientSide)
            return;
        MinecraftServer server = k.getLevel().getServer();
        UUID direct = directOwner(k);
        if (server == null || direct == null)
            return;
        ServerPlayer player = server.getPlayerList().getPlayer(direct);
        if (player == null)
            return;

        int limit = get(direct);
        boolean changed = setMachineLimit(k, limit);
        if (!changed)
            return;

        k.setChanged();
        k.sendData();
    }

    /**
     * Updates every currently loaded kinetic block placed by the player. Queued chunks and
     * unloaded chunks are updated by {@link #onMachineLoaded(KineticBlockEntity)} when they load.
     */
    public static int applyLimitToOwned(ServerPlayer player, int rpm) {
        MinecraftServer server = player.getServer();
        if (server == null)
            return 0;

        int limit = sanitize(rpm);
        int changed = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (ChunkHolder holder : ((ChunkMapAccessor) level.getChunkSource().chunkMap)
                .dimensionworks$getChunks()) {
                LevelChunk chunk = holder.getFullChunk();
                if (chunk == null)
                    continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof KineticBlockEntity kinetic))
                        continue;
                    UUID direct = directOwner(kinetic);
                    if (!player.getUUID().equals(direct))
                        continue;

                    if (setMachineLimit(kinetic, limit)) {
                        kinetic.setChanged();
                        kinetic.sendData();
                        changed++;
                    }
                }
            }
        }

        return changed;
    }


    private static int limitFor(KineticBlockEntity k) {
        UUID owner = owner(k);
        if (k.getLevel() != null && !k.getLevel().isClientSide && owner != null) {
            Integer live = CACHE.get(owner);
            if (live != null)
                return live;
        }

        int stored = storedMachineLimit(k);
        if (stored > 0)
            return stored;

        KineticBlockEntity current = k;
        for (int i = 0; i < 32 && current != null; i++) {
            int inherited = storedMachineLimit(current);
            if (inherited > 0)
                return inherited;
            if (!current.hasSource() || current.getLevel() == null)
                break;
            BlockEntity blockEntity = current.getLevel().getBlockEntity(current.source);
            current = blockEntity instanceof KineticBlockEntity source ? source : null;
        }

        return owner == null ? RpmLimitConfig.MAX_RPM.get() : limitForOwner(owner);
    }

    private static int limitForOwner(UUID owner) {
        Integer live = CACHE.get(owner);
        return live == null ? RpmLimitConfig.DEFAULT_RPM.get() : live;
    }

    private static int storedMachineLimit(KineticBlockEntity k) {
        CompoundTag data = k.getPersistentData();
        return data.contains(MACHINE_LIMIT_KEY) ? sanitize(data.getInt(MACHINE_LIMIT_KEY)) : 0;
    }

    private static boolean setMachineLimit(KineticBlockEntity k, int limit) {
        int value = sanitize(limit);
        CompoundTag data = k.getPersistentData();
        if (data.getInt(MACHINE_LIMIT_KEY) == value)
            return false;
        data.putInt(MACHINE_LIMIT_KEY, value);
        k.setChanged();
        return true;
    }

    public static void writeSyncData(KineticBlockEntity k, CompoundTag compound) {
        UUID direct = directOwner(k);
        if (direct != null)
            compound.putUUID(SYNC_OWNER_KEY, direct);
        int machineLimit = storedMachineLimit(k);
        if (machineLimit > 0)
            compound.putInt(SYNC_MACHINE_LIMIT_KEY, machineLimit);
    }

    public static void readSyncData(KineticBlockEntity k, CompoundTag compound) {
        CompoundTag data = k.getPersistentData();
        if (compound.hasUUID(SYNC_OWNER_KEY))
            data.putUUID(OWNER_KEY, compound.getUUID(SYNC_OWNER_KEY));
        if (compound.contains(SYNC_MACHINE_LIMIT_KEY))
            data.putInt(MACHINE_LIMIT_KEY, sanitize(compound.getInt(SYNC_MACHINE_LIMIT_KEY)));
    }

    private static int sanitize(int v) {
        return Math.max(1, Math.min(v, Math.max(1, RpmLimitConfig.MAX_RPM.get())));
    }
}
