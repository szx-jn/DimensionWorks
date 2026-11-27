package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Facade delegating RPM limit persistence, external modifiers, kinetic clamping, and chunk updates
 * to dedicated single-responsibility collaborators while preserving legacy entrypoints.
 */
public final class RpmLimitManager {
    private static final PlayerRpmStore PLAYER_STORE = new PlayerRpmStore();
    private static final ExternalRpmRegistry EXTERNAL_REGISTRY = new ExternalRpmRegistry();

    private RpmLimitManager() {
    }

    public static void beginServer(MinecraftServer server) {
        PLAYER_STORE.reset(server);
        EXTERNAL_REGISTRY.clear();
    }

    public static void endServer() {
        beginServer(null);
    }

    public static void rememberLogout(ServerPlayer player) {
        PLAYER_STORE.rememberLogout(player, getEffectiveLimit(player.getUUID()));
    }

    public static void init(Player p) {
        PLAYER_STORE.init(p);
    }

    public static int get(UUID id) {
        return PLAYER_STORE.getBase(id);
    }

    public static int getLimit(UUID id) {
        return get(id);
    }

    public static int getEffectiveLimit(UUID id) {
        int base = get(id);
        int effective = Math.min(base, PLAYER_STORE.offlineEffectiveOrDefault(id, base));
        return EXTERNAL_REGISTRY.applyModifiers(id, effective, PLAYER_STORE.hasCommandOverride(id));
    }

    public static void setLimit(ServerPlayer p, int rpm) {
        setStoredLimit(p, rpm, true);
    }

    static void setGearHeartDefaultLimit(ServerPlayer p, int rpm) {
        setStoredLimit(p, rpm, false);
    }

    static boolean hasCommandOverride(ServerPlayer player) {
        return PLAYER_STORE.hasCommandOverride(player.getUUID());
    }

    private static void setStoredLimit(ServerPlayer p, int rpm, boolean commandOverride) {
        PLAYER_STORE.setStoredLimit(p, PlayerRpmStore.sanitize(rpm), commandOverride);
        applyLimitToOwned(p, getEffectiveLimit(p.getUUID()));
    }

    public static void setExternalLimit(ServerPlayer player, String source, int rpm) {
        int sanitized = PlayerRpmStore.sanitize(rpm);
        if (EXTERNAL_REGISTRY.put(player.getUUID(), source, sanitized)) {
            applyLimitToOwned(player, getEffectiveLimit(player.getUUID()));
        }
    }

    public static void removeExternalLimit(ServerPlayer player, String source) {
        if (EXTERNAL_REGISTRY.remove(player.getUUID(), source)) {
            applyLimitToOwned(player, getEffectiveLimit(player.getUUID()));
        }
    }

    public static void refresh(Iterable<ServerPlayer> ps, long tick) {
        PLAYER_STORE.refreshChangedPlayers(ps, tick, player -> applyLimitToOwned(player, getEffectiveLimit(player.getUUID())));
    }

    /** Caps a machine's effective speed and the value Create stores during network propagation. */
    public static float clamp(KineticBlockEntity k, float requested) {
        return KineticClampService.clamp(k, requested);
    }

    public static float clampIndividual(KineticBlockEntity k, float requested) {
        return KineticClampService.clampIndividual(k, requested, RpmLimitManager::cachedOrZeroEffectiveLimit);
    }

    /** Ratio from an unscaled network speed to the player-limited effective speed. */
    public static float effectiveScale(KineticBlockEntity k, float raw) {
        return KineticClampService.effectiveScale(k, raw);
    }

    public static UUID owner(KineticBlockEntity k) {
        return KineticClampService.owner(k);
    }

    public static UUID directOwner(KineticBlockEntity k) {
        return KineticClampService.directOwner(k);
    }

    public static void setOwner(KineticBlockEntity k, UUID u) {
        KineticClampService.setOwner(k, u, getEffectiveLimit(u));
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
        PLAYER_STORE.loadOfflineProfile(direct, server);
        int limit = PLAYER_STORE.isCached(direct) ? getEffectiveLimit(direct) : KineticClampService.storedMachineLimit(k);
        if (limit <= 0)
            return;

        if (!KineticClampService.setMachineLimit(k, limit))
            return;

        k.setChanged();
        k.sendData();
    }

    /**
     * Updates every currently loaded kinetic block placed by the player. Queued chunks and
     * unloaded chunks are updated by {@link #onMachineLoaded(KineticBlockEntity)} when they load.
     */
    public static int applyLimitToOwned(ServerPlayer player, int rpm) {
        return LoadedKineticScanner.applyLimitToOwned(player, PlayerRpmStore.sanitize(rpm));
    }

    public static int individualLimit(KineticBlockEntity k) {
        return KineticClampService.individualLimit(k, RpmLimitManager::cachedOrZeroEffectiveLimit);
    }

    public static int fallbackIndividualLimit(KineticBlockEntity k) {
        return KineticClampService.limitFor(k, RpmLimitManager::cachedOrZeroEffectiveLimit);
    }

    private static int cachedOrZeroEffectiveLimit(UUID owner) {
        PLAYER_STORE.loadOfflineProfileIfNeeded(owner);
        return PLAYER_STORE.isCached(owner) ? getEffectiveLimit(owner) : 0;
    }

    public static void writeSyncData(KineticBlockEntity k, CompoundTag compound) {
        KineticClampService.writeSyncData(k, compound, individualLimit(k));
    }

    public static void readSyncData(KineticBlockEntity k, CompoundTag compound) {
        KineticClampService.readSyncData(k, compound);
    }
}
