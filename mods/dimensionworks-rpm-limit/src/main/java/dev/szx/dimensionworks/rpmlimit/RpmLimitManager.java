package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.mixin.ChunkMapAccessor;
import dev.szx.dimensionworks.rpmlimit.api.IPreclampedKineticSpeed;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpmLimitManager {
    private static final String PLAYER_KEY = "dimensionworks_rpm_limit";
    private static final String OWNER_KEY = "dimensionworks_owner";
    private static final String MACHINE_LIMIT_KEY = "dimensionworks_machine_rpm_limit";
    private static final String INIT_KEY = "dimensionworks_rpm_initialized";
    private static final String COMMAND_OVERRIDE_KEY = "dimensionworks_rpm_command_override";
    private static final String GEAR_HEART_RPM_SOURCE = "gear_heart:rpm";

    private static final String SYNC_OWNER_KEY = "DimensionWorksOwner";
    private static final String SYNC_MACHINE_LIMIT_KEY = "DimensionWorksMachineRpmLimit";

    private static final Map<UUID, Integer> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> EXTERNAL_LIMITS = new ConcurrentHashMap<>();
    private static final Set<UUID> COMMAND_OVERRIDES = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> OFFLINE_EFFECTIVE = new ConcurrentHashMap<>();
    private static final Set<UUID> CHECKED_PROFILES = ConcurrentHashMap.newKeySet();
    private static MinecraftServer activeServer;
    private static long last = Long.MIN_VALUE;

    public static void beginServer(MinecraftServer server) {
        CACHE.clear(); EXTERNAL_LIMITS.clear(); COMMAND_OVERRIDES.clear();
        OFFLINE_EFFECTIVE.clear(); CHECKED_PROFILES.clear(); last = Long.MIN_VALUE;
        activeServer = server;
    }
    public static void endServer() { beginServer(null); }

    private static void loadOfflineProfile(UUID owner, MinecraftServer server) {
        if (server == null || CACHE.containsKey(owner) || !CHECKED_PROFILES.add(owner)) return;
        java.io.File file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(owner + ".dat").toFile();
        if (!file.isFile()) return;
        try {
            CompoundTag nbt = NbtIo.readCompressed(file);
            RpmPlayerState.Profile profile = RpmPlayerState.fromPlayerNbt(nbt);
            CACHE.put(owner, profile.base());
            OFFLINE_EFFECTIVE.put(owner, profile.effective());
            if (profile.commandOverride()) COMMAND_OVERRIDES.add(owner);
        } catch (java.io.IOException | RuntimeException exception) {
            DimensionWorksRpmLimit.LOGGER.warn("Could not load an offline RPM profile; retaining machine fallback limits.", exception);
        }
    }
    public static void rememberLogout(ServerPlayer player) {
        RpmPlayerState.persist(player.getPersistentData());
        OFFLINE_EFFECTIVE.put(player.getUUID(), getEffectiveLimit(player.getUUID()));
    }

    public static void init(Player p) {
        CompoundTag d = p.getPersistentData();
        if (p instanceof ServerPlayer serverPlayer) loadOfflineProfile(p.getUUID(), serverPlayer.getServer());
        Integer recovered = CACHE.get(p.getUUID());
        boolean recoveredOverride = COMMAND_OVERRIDES.contains(p.getUUID());
        OFFLINE_EFFECTIVE.remove(p.getUUID());
        CHECKED_PROFILES.add(p.getUUID());
        RpmPlayerState.restore(d);
        if (!d.getBoolean(INIT_KEY) || !d.contains(PLAYER_KEY, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            if (!d.contains(PLAYER_KEY, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) d.putInt(PLAYER_KEY, recovered == null ? RpmLimitConfig.DEFAULT_RPM.get() : recovered);
            if (recoveredOverride) d.putBoolean(COMMAND_OVERRIDE_KEY, true);
            d.putBoolean(INIT_KEY, true);
        }
        CACHE.put(p.getUUID(), sanitize(d.getInt(PLAYER_KEY)));
        if (d.getBoolean(COMMAND_OVERRIDE_KEY))
            COMMAND_OVERRIDES.add(p.getUUID());
        else
            COMMAND_OVERRIDES.remove(p.getUUID());
        RpmPlayerState.persist(d);
    }

    public static int get(UUID id) {
        loadOfflineProfile(id, activeServer);
        return CACHE.getOrDefault(id, RpmLimitConfig.DEFAULT_RPM.get());
    }

    public static int getLimit(UUID id) {
        return get(id);
    }

    public static int getEffectiveLimit(UUID id) {
        int base = get(id);
        int effective = Math.min(base, OFFLINE_EFFECTIVE.getOrDefault(id, base));
        boolean commandOverride = hasCommandOverride(id);
        for (Map.Entry<String, Integer> limit : externalLimits(id).entrySet()) {
            if (commandOverride && GEAR_HEART_RPM_SOURCE.equals(limit.getKey()))
                continue;
            effective = Math.min(effective, sanitize(limit.getValue()));
        }
        return effective;
    }

    public static void setLimit(ServerPlayer p, int rpm) {
        setStoredLimit(p, rpm, true);
    }

    static void setGearHeartDefaultLimit(ServerPlayer p, int rpm) {
        setStoredLimit(p, rpm, false);
    }

    static boolean hasCommandOverride(ServerPlayer player) {
        return hasCommandOverride(player.getUUID());
    }

    private static boolean hasCommandOverride(UUID id) {
        return COMMAND_OVERRIDES.contains(id);
    }

    private static void setStoredLimit(ServerPlayer p, int rpm, boolean commandOverride) {
        int v = sanitize(rpm);
        p.getPersistentData().putInt(PLAYER_KEY, v);
        p.getPersistentData().putBoolean(INIT_KEY, true);
        if (commandOverride)
            p.getPersistentData().putBoolean(COMMAND_OVERRIDE_KEY, true);
        OFFLINE_EFFECTIVE.remove(p.getUUID());
        RpmPlayerState.persist(p.getPersistentData());
        if (commandOverride)
            COMMAND_OVERRIDES.add(p.getUUID());
        CACHE.put(p.getUUID(), v);
        applyLimitToOwned(p, getEffectiveLimit(p.getUUID()));
    }

    public static void setExternalLimit(ServerPlayer player, String source, int rpm) {
        Integer previous = externalLimits(player.getUUID()).put(source, sanitize(rpm));
        if (previous == null || previous != sanitize(rpm))
            applyLimitToOwned(player, getEffectiveLimit(player.getUUID()));
    }

    public static void removeExternalLimit(ServerPlayer player, String source) {
        Map<String, Integer> limits = externalLimits(player.getUUID());
        if (limits.remove(source) != null)
            applyLimitToOwned(player, getEffectiveLimit(player.getUUID()));
    }


    public static void refresh(Iterable<ServerPlayer> ps, long tick) {
        int i = Math.max(1, RpmLimitConfig.CACHE_INTERVAL_TICKS.get());
        if (last != Long.MIN_VALUE && tick >= last && tick - last < i)
            return;
        last = tick;
        for (ServerPlayer p : ps) {
            int v = sanitize(p.getPersistentData().getInt(PLAYER_KEY));
            p.getPersistentData().putInt(PLAYER_KEY, v);
            RpmPlayerState.persist(p.getPersistentData());
            Integer previous = CACHE.put(p.getUUID(), v);
            if (previous == null || previous != v)
                applyLimitToOwned(p, getEffectiveLimit(p.getUUID()));
        }
    }

    /** Caps a machine's effective speed and the value Create stores during network propagation. */
    public static float clamp(KineticBlockEntity k, float requested) {
        if (!Float.isFinite(requested) || requested == 0)
            return 0;
        if (k instanceof IPreclampedKineticSpeed)
            return requested;
        return RpmLimitMath.cap(requested, dev.szx.dimensionworks.rpmlimit.optimizer.NetworkRpmLimit.limit(k));
    }

    public static float clampIndividual(KineticBlockEntity k, float requested) {
        if (k instanceof IPreclampedKineticSpeed) return Float.isFinite(requested) ? requested : 0;
        // Unowned relay/belt segments inherit the network at the work boundary. Their raw
        // propagation speed must not be irreversibly capped to a synthetic default owner.
        return RpmLimitMath.cap(requested, directOwner(k) == null ? RpmLimitConfig.MAX_RPM.get() : individualLimit(k));
    }

    /** Ratio from an unscaled network speed to the player-limited effective speed. */
    public static float effectiveScale(KineticBlockEntity k, float raw) {
        if (!Float.isFinite(raw))
            return 0;
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
            if (!cur.hasSource() || cur.getLevel() == null || !cur.getLevel().hasChunkAt(cur.source))
                break;
            BlockEntity blockEntity = cur.getLevel().getBlockEntity(cur.source);
            cur = blockEntity instanceof KineticBlockEntity source ? source : null;
        }
        return null;
    }

    public static UUID directOwner(KineticBlockEntity k) {
        CompoundTag data = k.getPersistentData();
        return data.hasUUID(OWNER_KEY) ? data.getUUID(OWNER_KEY) : null;
    }

    public static void setOwner(KineticBlockEntity k, UUID u) {
        k.getPersistentData().putUUID(OWNER_KEY, u);
        setMachineLimit(k, getEffectiveLimit(u));
        k.setChanged();
        k.sendData();
    }

    public static void onMachineLoaded(KineticBlockEntity k) {
        if (k.getLevel() == null || k.getLevel().isClientSide)
            return;
        MinecraftServer server = k.getLevel().getServer();
        UUID direct = directOwner(k);
        if (server == null) return;
        if (direct == null) {
            if (k.hasNetwork()) k.updateSpeed = true;
            return;
        }
        loadOfflineProfile(direct, server);
        int limit = CACHE.containsKey(direct) ? getEffectiveLimit(direct) : storedMachineLimit(k);
        if (limit <= 0)
            return;

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



    public static int individualLimit(KineticBlockEntity k) {
        if (k instanceof dev.szx.dimensionworks.rpmlimit.api.IRuntimeKineticMember) return RpmLimitConfig.MAX_RPM.get();
        if (directOwner(k) == null && k.getLevel() != null && k.hasNetwork())
            return dev.szx.dimensionworks.rpmlimit.optimizer.NetworkRpmLimit.limit(k);
        return limitFor(k);
    }

    public static int fallbackIndividualLimit(KineticBlockEntity k) { return limitFor(k); }

    private static int limitFor(KineticBlockEntity k) {
        UUID owner = owner(k);
        if (owner != null && k.getLevel() != null && !k.getLevel().isClientSide)
            loadOfflineProfile(owner, k.getLevel().getServer());
        if (k.getLevel() != null && !k.getLevel().isClientSide && owner != null) {
            if (CACHE.containsKey(owner))
                return getEffectiveLimit(owner);
        }

        int stored = storedMachineLimit(k);
        if (stored > 0)
            return stored;

        KineticBlockEntity current = k;
        for (int i = 0; i < 32 && current != null; i++) {
            int inherited = storedMachineLimit(current);
            if (inherited > 0)
                return inherited;
            if (!current.hasSource() || current.getLevel() == null || !current.getLevel().hasChunkAt(current.source))
                break;
            BlockEntity blockEntity = current.getLevel().getBlockEntity(current.source);
            current = blockEntity instanceof KineticBlockEntity source ? source : null;
        }

        return owner == null ? RpmLimitConfig.DEFAULT_RPM.get() : limitForOwner(owner);
    }

    private static int limitForOwner(UUID owner) {
        return getEffectiveLimit(owner);
    }

    private static Map<String, Integer> externalLimits(UUID owner) {
        return EXTERNAL_LIMITS.computeIfAbsent(owner, ignored -> new ConcurrentHashMap<>());
    }


    private static int storedMachineLimit(KineticBlockEntity k) {
        CompoundTag data = k.getPersistentData();
        return data.contains(MACHINE_LIMIT_KEY) ? sanitize(data.getInt(MACHINE_LIMIT_KEY)) : 0;
    }

    private static boolean setMachineLimit(KineticBlockEntity k, int limit) {
        int value = sanitize(limit);
        CompoundTag data = k.getPersistentData();
        boolean changed = data.getInt(MACHINE_LIMIT_KEY) != value;
        int previousLimit = data.getInt(MACHINE_LIMIT_KEY);
        if (changed) {
            data.putInt(MACHINE_LIMIT_KEY, value);
            k.setChanged();
            // Cached stress/capacity depend on the effective RPM limit, not only Create's raw speed.
            KineticNetworkOptimizer.invalidateAndRefreshFor(k);
            if (value > previousLimit) {
                k.updateSpeed = true;
                if (k instanceof com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity generator)
                    generator.reActivateSource = true;
            }
        }

        float currentSpeed = k.getTheoreticalSpeed();
        float cappedSpeed = RpmLimitMath.cap(currentSpeed, value);
        if (currentSpeed != cappedSpeed) {
            k.setSpeed(cappedSpeed);
            k.setChanged();
            k.sendData();
            changed = true;
        }
        return changed;
    }

    public static void writeSyncData(KineticBlockEntity k, CompoundTag compound) {
        UUID direct = directOwner(k);
        if (direct != null)
            compound.putUUID(SYNC_OWNER_KEY, direct);
        int machineLimit = individualLimit(k);
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
