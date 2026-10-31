package dev.szx.dimensionworks.rpmlimit.optimizer;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.DimensionWorksRpmLimit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Keeps incremental totals for Create kinetic networks.
 *
 * <p>The cache is only trusted while every change passes through a known hook. Structural changes
 * and uncertain mutations invalidate it; the next calculation then rebuilds from Create's live
 * member maps. This favours correctness over keeping a questionable cache alive.</p>
 */
public final class KineticNetworkOptimizer {
    private static final int MAX_INCREMENTAL_STRESS_UPDATES = 4096;
    private static final int MAX_INCREMENTAL_CAPACITY_UPDATES = 4096;

    private static final Map<KineticNetwork, NetworkCache> CACHES =
        Collections.synchronizedMap(new WeakHashMap<>());
    /**
     * Networks rebuilt from chunk NBT need one member sync even when their aggregate totals did
     * not change. The value records whether Create already synchronized during that refresh.
     */
    private static final Map<KineticNetwork, Boolean> PENDING_LOAD_REFRESHES =
        Collections.synchronizedMap(new WeakHashMap<>());

    private static final ThreadLocal<Batch> BATCH =
        ThreadLocal.withInitial(Batch::new);

    private KineticNetworkOptimizer() {
    }

    public static Float cachedStress(KineticNetwork network) {
        NetworkCache cache = existingCache(network);
        return cache != null && cache.stressValid ? cache.stressTotal : null;
    }

    public static Float cachedCapacity(KineticNetwork network) {
        NetworkCache cache = existingCache(network);
        return cache != null && cache.capacityValid ? cache.capacityTotal : null;
    }

    public static void beginStressRebuild(KineticNetwork network) {
        NetworkCache cache = cacheFor(network);
        cache.stressValid = false;
        cache.stressContributions.clear();
        cache.stressDeltaUpdates = 0;
    }

    public static void recordStressContribution(KineticNetwork network,
                                                KineticBlockEntity member,
                                                float contribution) {
        if (!Float.isFinite(contribution)) {
            invalidateStress(network);
            return;
        }
        NetworkCache cache = cacheFor(network);
        cache.stressContributions.put(member, contribution);
    }

    public static void finishStressRebuild(KineticNetwork network, float totalStress) {
        NetworkCache cache = cacheFor(network);
        if (!Float.isFinite(totalStress)) {
            cache.stressValid = false;
            cache.stressContributions.clear();
            return;
        }
        cache.stressTotal = totalStress;
        cache.stressValid = true;
        cache.stressDeltaUpdates = 0;
    }

    /**
     * Called after Create has completed a full capacity scan. The extra pass is only paid when
     * the cache is invalid and saves every later capacity scan until the next invalidation.
     */
    public static void finishCapacityRebuild(KineticNetwork network, float totalCapacity) {
        NetworkCache cache = cacheFor(network);
        boolean sourceSetChanged = !cache.capacityContributions.keySet().equals(network.sources.keySet());
        cache.capacityValid = false;
        cache.capacityContributions.clear();

        if (!Float.isFinite(totalCapacity)) {
            return;
        }

        for (KineticBlockEntity source : network.sources.keySet()) {
            float contribution;
            try {
                contribution = network.getActualCapacityOf(source);
            } catch (RuntimeException exception) {
                cache.capacityContributions.clear();
                return;
            }
            if (!Float.isFinite(contribution)) {
                cache.capacityContributions.clear();
                return;
            }
            cache.capacityContributions.put(source, contribution);
        }

        cache.capacityTotal = totalCapacity;
        cache.capacityValid = true;
        cache.capacityDeltaUpdates = 0;
        if (sourceSetChanged) NetworkRpmLimit.invalidate(network);
    }

    /**
     * Replaces one member's contribution after its value in Create's map has changed.
     * Returns true only when a valid cached total was incrementally updated.
     */
    public static boolean updateStressContribution(KineticNetwork network, KineticBlockEntity member) {
        KineticPersistence.invalidate(network);
        NetworkCache cache = existingCache(network);
        if (cache == null || !cache.stressValid
            || !cache.stressContributions.containsKey(member)) {
            invalidateStress(network);
            return false;
        }

        Float oldValue = cache.stressContributions.get(member);
        float newValue;
        try {
            newValue = network.getActualStressOf(member);
        } catch (RuntimeException exception) {
            invalidateStress(network);
            return false;
        }

        if (oldValue == null || !Float.isFinite(oldValue) || !Float.isFinite(newValue)) {
            invalidateStress(network);
            return false;
        }
        if (Float.compare(oldValue, newValue) == 0) {
            return false;
        }

        cache.stressTotal = applyDelta(cache.stressTotal, oldValue, newValue);
        cache.stressContributions.put(member, newValue);
        cache.stressDeltaUpdates++;

        if (!Float.isFinite(cache.stressTotal) || cache.stressTotal < 0.0F
            || cache.stressDeltaUpdates >= MAX_INCREMENTAL_STRESS_UPDATES) {
            invalidateStress(network);
            return false;
        }
        return true;
    }

    /**
     * Replaces one source's contribution after its capacity entry has changed.
     * Returns true only when a valid cached total was incrementally updated.
     */
    public static boolean updateCapacityContribution(KineticNetwork network, KineticBlockEntity source) {
        KineticPersistence.invalidate(network);
        NetworkCache cache = existingCache(network);
        if (cache == null || !cache.capacityValid
            || !cache.capacityContributions.containsKey(source)) {
            invalidateCapacity(network);
            return false;
        }

        Float oldValue = cache.capacityContributions.get(source);
        float newValue;
        try {
            newValue = network.getActualCapacityOf(source);
        } catch (RuntimeException exception) {
            invalidateCapacity(network);
            return false;
        }

        if (oldValue == null || !Float.isFinite(oldValue) || !Float.isFinite(newValue)) {
            invalidateCapacity(network);
            return false;
        }

        if (Float.compare(oldValue, newValue) == 0) {
            return true;
        }

        cache.capacityTotal = applyDelta(cache.capacityTotal, oldValue, newValue);
        cache.capacityContributions.put(source, newValue);
        cache.capacityDeltaUpdates++;

        if (!Float.isFinite(cache.capacityTotal) || cache.capacityTotal < 0.0F
            || cache.capacityDeltaUpdates >= MAX_INCREMENTAL_CAPACITY_UPDATES) {
            invalidateCapacity(network);
            return false;
        }
        return true;
    }

    public static void onMemberSpeedChanged(KineticBlockEntity member) {
        if (member == null || member.getLevel() == null || member.getLevel().isClientSide
            || !member.hasNetwork()) {
            return;
        }

        KineticNetwork network = member.getOrCreateNetwork();
        NetworkCache before = existingCache(network);
        boolean cacheWasValid = before != null && before.stressValid;
        boolean updated = updateStressContribution(network, member);

        if (updated) {
            if (isBatching()) {
                queueNetwork(network);
            } else {
                // Create's existing tick path performs the refresh at the end of this member's tick.
                member.networkDirty = true;
            }
            return;
        }

        NetworkCache after = existingCache(network);
        if (!cacheWasValid || after == null || !after.stressValid) {
            // A member can change speed while the cache is already invalid. Its BlockEntity still
            // needs to drive the ordinary full recalculation on its next tick.
            if (isBatching()) {
                queueNetwork(network);
            } else {
                member.networkDirty = true;
            }
        }
    }

    /**
     * Invalidates all currently tracked networks after a configuration reload changes effective
     * RPM or stress rules, then schedules one real member to perform the normal full refresh.
     */
    public static void invalidateAllLoadedNetworks() {
        List<KineticNetwork> networks;
        synchronized (CACHES) {
            networks = new ArrayList<>(CACHES.keySet());
        }

        for (KineticNetwork network : networks) {
            NetworkCache cache = existingCache(network);
            if (cache == null) {
                continue;
            }
            boolean needsRefresh = cache.stressValid || cache.capacityValid;
            invalidate(network);
            if (!needsRefresh || isBatching()) {
                continue;
            }

            for (KineticBlockEntity member : network.members.keySet()) {
                if (member.getLevel() == null || member.getLevel().isClientSide
                    || member.getLevel().getBlockEntity(member.getBlockPos()) != member) {
                    continue;
                }
                member.networkDirty = true;
                break;
            }
        }
    }

    /** Invalidates totals without forcing work during block removal or chunk unload. */
    public static void invalidateFor(KineticBlockEntity member) {
        if (member == null || member.getLevel() == null || member.getLevel().isClientSide
            || !member.hasNetwork()) {
            return;
        }
        invalidate(member.getOrCreateNetwork());
    }

    /**
     * Invalidates and schedules one live member to perform the ordinary full refresh. Use this for
     * mutable rules such as a player's RPM limit; do not use it from a removal lifecycle hook.
     */
    public static void invalidateAndRefreshFor(KineticBlockEntity member) {
        if (member == null || member.getLevel() == null || member.getLevel().isClientSide
            || !member.hasNetwork()) {
            return;
        }

        KineticNetwork network = member.getOrCreateNetwork();
        NetworkCache before = existingCache(network);
        boolean needsRefresh = before == null || before.stressValid || before.capacityValid;
        invalidate(network);
        if (isBatching() || !needsRefresh) {
            return;
        }

        // Prefer a different live member because the changed member may be part of a removal path.
        // If it is still live and is the only member, it is a valid fallback.
        KineticBlockEntity fallback = null;
        for (KineticBlockEntity candidate : network.members.keySet()) {
            if (candidate.getLevel() == null || candidate.getLevel().isClientSide
                || candidate.getLevel().getBlockEntity(candidate.getBlockPos()) != candidate) {
                continue;
            }
            if (candidate != member) {
                candidate.networkDirty = true;
                return;
            }
            fallback = candidate;
        }
        if (fallback != null) {
            fallback.networkDirty = true;
        }
    }

    /** Call after a network's membership or unloaded-member accounting changes. */
    public static void onTopologyChanged(KineticNetwork network) {
        invalidate(network);
        for (KineticBlockEntity candidate : network.members.keySet()) {
            if (candidate.getLevel() != null && !candidate.getLevel().isClientSide && !candidate.isRemoved()
                && candidate.getLevel().hasChunkAt(candidate.getBlockPos())
                && candidate.getLevel().getBlockEntity(candidate.getBlockPos()) == candidate) {
                candidate.networkDirty = true;
                break;
            }
        }
    }

    /** Invalidates cached limits after a recalculation prunes a stale member from Create's map. */
    public static void onMemberPruned(KineticNetwork network, KineticBlockEntity member) {
        if (network != null && member != null) invalidate(network);
    }

    /**
     * Create restores chunk members with {@code addSilently}, which deliberately avoids syncing
     * them. Mark the network dirty so one loaded member drives a full refresh on its next tick.
     */
    public static void onMemberLoadedSilently(KineticNetwork network, KineticBlockEntity member) {
        if (network == null || member == null) {
            return;
        }
        invalidate(network);
        PENDING_LOAD_REFRESHES.put(network, false);
        member.networkDirty = true;
    }

    /** Called by the mixin around the sync already present in Create's updateNetwork method. */
    public static void syncDuringNetworkRefresh(KineticNetwork network) {
        if (PENDING_LOAD_REFRESHES.containsKey(network)) {
            PENDING_LOAD_REFRESHES.put(network, true);
        }
        network.sync();
    }

    /**
     * Finish a load-triggered refresh. Create only syncs when aggregate totals change, so force one
     * sync when its normal updateNetwork path found equal totals and skipped its usual sync.
     */
    public static void finishLoadedNetworkRefresh(KineticNetwork network) {
        Boolean alreadySynced = PENDING_LOAD_REFRESHES.remove(network);
        if (alreadySynced != null && !alreadySynced) {
            network.sync();
        }
    }

    /**
     * Invalidates both totals after code outside Create changes its public member/source maps.
     * In a batch, the network will be refreshed exactly once when the batch closes.
     */
    public static void invalidate(KineticNetwork network) {
        if (network == null) {
            return;
        }
        KineticPersistence.invalidate(network);
        NetworkRpmLimit.invalidate(network);
        PENDING_LOAD_REFRESHES.put(network, false);
        NetworkCache cache = cacheFor(network);
        cache.stressValid = false;
        cache.capacityValid = false;
        cache.stressContributions.clear();
        cache.capacityContributions.clear();
        if (isBatching()) {
            queueNetwork(network);
        }
    }

    public static void beginBatch() {
        BATCH.get().depth++;
    }

    public static boolean isBatching() {
        return BATCH.get().depth > 0;
    }

    public static void queueNetwork(KineticNetwork network) {
        if (network != null && isBatching()) {
            BATCH.get().dirtyNetworks.add(network);
        }
    }

    public static void endBatch() {
        Batch batch = BATCH.get();
        if (batch.depth <= 0) {
            return;
        }
        batch.depth--;
        if (batch.depth != 0) {
            return;
        }

        List<KineticNetwork> pending = new ArrayList<>(batch.dirtyNetworks);
        BATCH.remove();
        for (KineticNetwork network : pending) {
            try {
                network.updateNetwork();
            } catch (RuntimeException exception) {
                DimensionWorksRpmLimit.LOGGER.warn(
                    "Failed to refresh one kinetic network; continuing with remaining networks.",
                    exception);
            }
        }
    }

    /** Clears a batch leaked by an exception in third-party kinetic code. */
    public static void abortBatch() {
        Batch batch = BATCH.get();
        if (batch.depth > 0) {
            batch.depth = 1;
            endBatch();
        }
    }

    static float applyDelta(float total, float previousContribution, float newContribution) {
        return total - previousContribution + newContribution;
    }

    private static void invalidateStress(KineticNetwork network) {
        NetworkCache cache = cacheFor(network);
        cache.stressValid = false;
        cache.stressContributions.clear();
    }

    private static void invalidateCapacity(KineticNetwork network) {
        NetworkCache cache = cacheFor(network);
        cache.capacityValid = false;
        cache.capacityContributions.clear();
    }

    private static NetworkCache existingCache(KineticNetwork network) {
        synchronized (CACHES) {
            return CACHES.get(network);
        }
    }

    private static NetworkCache cacheFor(KineticNetwork network) {
        synchronized (CACHES) {
            return CACHES.computeIfAbsent(network, ignored -> new NetworkCache());
        }
    }

    private static final class NetworkCache {
        private final Map<KineticBlockEntity, Float> stressContributions = new WeakHashMap<>();
        private final Map<KineticBlockEntity, Float> capacityContributions = new WeakHashMap<>();
        private boolean stressValid;
        private boolean capacityValid;
        private float stressTotal;
        private float capacityTotal;
        private int stressDeltaUpdates;
        private int capacityDeltaUpdates;
    }

    private static final class Batch {
        private int depth;
        private final Set<KineticNetwork> dirtyNetworks =
            Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    }
}
