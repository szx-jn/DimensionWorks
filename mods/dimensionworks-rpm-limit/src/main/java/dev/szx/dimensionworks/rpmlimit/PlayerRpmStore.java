package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Manages per-player RPM limit state, offline profile recovery, and periodic sync. */
final class PlayerRpmStore {
    private static final String PLAYER_KEY = "dimensionworks_rpm_limit";
    private static final String INIT_KEY = "dimensionworks_rpm_initialized";
    private static final String COMMAND_OVERRIDE_KEY = "dimensionworks_rpm_command_override";

    private final Map<UUID, Integer> cache = new ConcurrentHashMap<>();
    private final Set<UUID> commandOverrides = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> offlineEffective = new ConcurrentHashMap<>();
    private final Set<UUID> checkedProfiles = ConcurrentHashMap.newKeySet();
    private MinecraftServer activeServer;
    private long lastRefreshTick = Long.MIN_VALUE;

    void reset(MinecraftServer server) {
        cache.clear();
        commandOverrides.clear();
        offlineEffective.clear();
        checkedProfiles.clear();
        lastRefreshTick = Long.MIN_VALUE;
        activeServer = server;
    }

    void loadOfflineProfileIfNeeded(UUID owner) {
        loadOfflineProfile(owner, activeServer);
    }

    void loadOfflineProfile(UUID owner, MinecraftServer server) {
        if (server == null || cache.containsKey(owner) || !checkedProfiles.add(owner)) {
            return;
        }
        File file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(owner + ".dat").toFile();
        if (!file.isFile()) {
            return;
        }
        try {
            CompoundTag nbt = NbtIo.readCompressed(file.toPath(), NbtAccounter.create(16L * 1024 * 1024));
            RpmPlayerState.Profile profile = RpmPlayerState.fromPlayerNbt(nbt);
            cache.put(owner, profile.base());
            offlineEffective.put(owner, profile.effective());
            if (profile.commandOverride()) {
                commandOverrides.add(owner);
            }
        } catch (IOException | RuntimeException exception) {
            DimensionWorksRpmLimit.LOGGER.warn("Could not load an offline RPM profile; retaining machine fallback limits.", exception);
        }
    }

    void init(Player player) {
        CompoundTag d = player.getPersistentData();
        if (player instanceof ServerPlayer serverPlayer) {
            loadOfflineProfile(player.getUUID(), serverPlayer.getServer());
        }
        Integer recovered = cache.get(player.getUUID());
        boolean recoveredOverride = commandOverrides.contains(player.getUUID());
        offlineEffective.remove(player.getUUID());
        checkedProfiles.add(player.getUUID());
        RpmPlayerState.restore(d);
        if (!d.getBoolean(INIT_KEY) || !d.contains(PLAYER_KEY, Tag.TAG_ANY_NUMERIC)) {
            if (!d.contains(PLAYER_KEY, Tag.TAG_ANY_NUMERIC)) {
                d.putInt(PLAYER_KEY, recovered == null ? RpmLimitConfig.DEFAULT_RPM.get() : recovered);
            }
            if (recoveredOverride) {
                d.putBoolean(COMMAND_OVERRIDE_KEY, true);
            }
            d.putBoolean(INIT_KEY, true);
        }
        cache.put(player.getUUID(), sanitize(d.getInt(PLAYER_KEY)));
        if (d.getBoolean(COMMAND_OVERRIDE_KEY)) {
            commandOverrides.add(player.getUUID());
        } else {
            commandOverrides.remove(player.getUUID());
        }
        RpmPlayerState.persist(d);
    }

    void rememberLogout(ServerPlayer player, int currentEffectiveLimit) {
        RpmPlayerState.persist(player.getPersistentData());
        offlineEffective.put(player.getUUID(), currentEffectiveLimit);
    }

    int getBase(UUID id) {
        loadOfflineProfile(id, activeServer);
        return cache.getOrDefault(id, RpmLimitConfig.DEFAULT_RPM.get());
    }

    boolean isCached(UUID id) {
        return cache.containsKey(id);
    }

    int offlineEffectiveOrDefault(UUID id, int fallback) {
        return offlineEffective.getOrDefault(id, fallback);
    }

    boolean hasCommandOverride(UUID id) {
        return commandOverrides.contains(id);
    }

    void setStoredLimit(ServerPlayer player, int sanitizedRpm, boolean commandOverride) {
        CompoundTag data = player.getPersistentData();
        data.putInt(PLAYER_KEY, sanitizedRpm);
        data.putBoolean(INIT_KEY, true);
        if (commandOverride) {
            data.putBoolean(COMMAND_OVERRIDE_KEY, true);
        }
        offlineEffective.remove(player.getUUID());
        RpmPlayerState.persist(data);
        if (commandOverride) {
            commandOverrides.add(player.getUUID());
        }
        cache.put(player.getUUID(), sanitizedRpm);
    }

    void refreshChangedPlayers(Iterable<ServerPlayer> players, long tick, java.util.function.Consumer<ServerPlayer> onChanged) {
        int interval = Math.max(1, RpmLimitConfig.CACHE_INTERVAL_TICKS.get());
        if (lastRefreshTick != Long.MIN_VALUE && tick >= lastRefreshTick && tick - lastRefreshTick < interval) {
            return;
        }
        lastRefreshTick = tick;
        for (ServerPlayer player : players) {
            int value = sanitize(player.getPersistentData().getInt(PLAYER_KEY));
            player.getPersistentData().putInt(PLAYER_KEY, value);
            RpmPlayerState.persist(player.getPersistentData());
            Integer previous = cache.put(player.getUUID(), value);
            if (previous == null || previous != value) {
                onChanged.accept(player);
            }
        }
    }

    static int sanitize(int v) {
        return Math.max(1, Math.min(v, Math.max(1, RpmLimitConfig.MAX_RPM.get())));
    }
}
