package dev.szx.dimensionworks.rpmlimit.optimizer;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
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

    /** Networks whose aggregate changed without an overload transition are published once at tick end. */
    private static final Set<KineticNetwork> PENDING_PUBLICATIONS =
        Collections.synchronizedSet(Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    /** Explicit batches are synchronous API boundaries and publish before returning. */
    private static final ThreadLocal<Set<KineticNetwork>> FORCE_PUBLICATION = ThreadLocal.withInitial(
        () -> Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    private static final ThreadLocal<Batch> BATCH =
        ThreadLocal.withInitial(Batch::new);

    private KineticNetworkOptimizer() {
    }

    public static Float cachedStress(KineticNetwork network) {
        NetworkCache cache = existingCache(network);
        if (cache == null || !cache.stressValid) {
            return null;
        }
        // A third-party add/remove that bypasses our topology hooks must not expose a total
        // assembled for a different member set. This O(1) guard preserves that fallback.
        if (cache.stressContributions.size() != network.members.size()) {
            invalidateStress(network);
            cache.publishedOverloadKnown = false;
            return null;
        }
        return cache.stressTotal;
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
        cache.stressRebuildFailed = false;
    }

    public static void recordStressContribution(KineticNetwork network,
                                                KineticBlockEntity member,
                                                float contribution) {
        if (!Float.isFinite(contribution)) {
            NetworkCache cache = cacheFor(network);
            cache.stressRebuildFailed = true;
            return;
        }
        NetworkCache cache = cacheFor(network);
        cache.stressContributions.put(member, contribution);
    }

    public static void finishStressRebuild(KineticNetwork network, float totalStress) {
        NetworkCache cache = cacheFor(network);
        if (!Float.isFinite(totalStress) || cache.stressRebuildFailed
            || cache.stressContributions.size() != network.members.size()) {
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
        if (sourceSetChanged) {
            NetworkRpmLimit.invalidate(network);
            dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry.invalidate(network);
        }
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
        dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry.invalidate(network);
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
        cacheFor(network).publishedOverloadKnown = false;
        if (isBatching() && BATCH.get().preservedTopologyNetworks.contains(network)) {
            Batch batch = BATCH.get();
            if (!batch.invalidatedTopologyNetworks.add(network)) {
                queueNetwork(network);
                return;
            }
        }
        invalidate(network);
        // Create.add already marks the added member dirty, and remove marks one remaining member
        // dirty. During a synchronous propagation batch, defer the full member publication to
        // the batch boundary instead of finding another member after every individual mutation.
        if (isBatching()) {
            // invalidate(network) above already queued the refresh. Create.add/remove also marks
            // the member it needs to refresh dirty, so walking the whole map here is redundant.
            return;
        }
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
        if (network != null && member != null) {
            cacheFor(network).publishedOverloadKnown = false;
            invalidate(network);
        }
    }

    /**
     * Create restores chunk members with {@code addSilently}, which deliberately avoids syncing
     * them. Mark the network dirty so one loaded member drives a full refresh on its next tick.
     */
    public static void onMemberLoadedSilently(KineticNetwork network, KineticBlockEntity member) {
        if (network == null || member == null) {
            return;
        }
        cacheFor(network).publishedOverloadKnown = false;
        invalidate(network);
        PENDING_LOAD_REFRESHES.put(network, false);
        member.networkDirty = true;
    }

    /** Called by Create's aggregate refresh methods where it would normally sync every member. */
    public static void syncDuringNetworkRefresh(KineticNetwork network) {
        if (network == null) {
            return;
        }
        if (PENDING_LOAD_REFRESHES.containsKey(network)) {
            PENDING_LOAD_REFRESHES.put(network, true);
            network.sync();
            return;
        }
        if (FORCE_PUBLICATION.get().contains(network) || overloadStateNeedsRefresh(network)) {
            PENDING_PUBLICATIONS.remove(network);
            network.sync();
            return;
        }
        // KineticBlockEntity.updateFromNetwork only changes the costly running state when the
        // overload flag flips. For ordinary load/capacity changes, publish the same snapshot once
        // after all block entities have ticked rather than once per changing member.
        PENDING_PUBLICATIONS.add(network);
    }

    private static boolean overloadStateNeedsRefresh(KineticNetwork network) {
        boolean overloaded = StressImpact.isEnabled() && network.calculateStress() > network.calculateCapacity();
        NetworkCache cache = existingCache(network);
        if (cache != null && cache.publishedOverloadKnown) {
            return cache.publishedOverloaded != overloaded;
        }

        for (KineticBlockEntity member : network.members.keySet()) {
            if (member.isOverStressed() != overloaded) {
                return true;
            }
        }
        if (cache != null) {
            cache.publishedOverloaded = overloaded;
            cache.publishedOverloadKnown = true;
        }
        return false;
    }

    /** Record the aggregate state after Create publishes a full member snapshot. */
    public static void onNetworkSynced(KineticNetwork network) {
        if (network == null) {
            return;
        }
        float stress = network.calculateStress();
        float capacity = network.calculateCapacity();
        NetworkCache cache = cacheFor(network);
        cache.publishedOverloaded = StressImpact.isEnabled() && stress > capacity;
        cache.publishedOverloadKnown = true;
    }

    /** Publish deferred, non-overload-changing snapshots once after the server tick's BE updates. */
    public static void flushPendingPublications() {
        List<KineticNetwork> pending;
        synchronized (PENDING_PUBLICATIONS) {
            pending = new ArrayList<>(PENDING_PUBLICATIONS);
            PENDING_PUBLICATIONS.clear();
        }
        for (KineticNetwork network : pending) {
            if (network != null) {
                network.sync();
            }
        }
    }

    private static void updateNetworkAndPublishNow(KineticNetwork network) {
        Set<KineticNetwork> forced = FORCE_PUBLICATION.get();
        forced.add(network);
        try {
            network.updateNetwork();
        } finally {
            forced.remove(network);
            if (forced.isEmpty()) {
                FORCE_PUBLICATION.remove();
            }
        }
    }

    /**
     * Finish a load-triggered refresh. Create only syncs when aggregate totals change, so force one
     * sync when its normal updateNetwork path found equal totals and skipped its usual sync.
     */
    public static void finishLoadedNetworkRefresh(KineticNetwork network) {
        Boolean alreadySynced = PENDING_LOAD_REFRESHES.remove(network);
        if (alreadySynced != null && !alreadySynced) {
            PENDING_PUBLICATIONS.remove(network);
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
        NetworkCache cache = cacheFor(network);
        cache.stressValid = false;
        cache.capacityValid = false;
        // Keep the old contribution tables until a rebuild replaces them. WeakHashMap.clear()
        // walks its full bucket table, making repeated invalidations scale with the largest
        // network ever cached. A valid flag is sufficient to reject the stale values.
        dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry.invalidate(network);
        if (isBatching()) {
            queueNetwork(network);
        }
    }

    public static void beginBatch() {
        BATCH.get().depth++;
    }

    /**
     * Create tears down and rebuilds a whole speed tree when an independent generator changes RPM,
     * even though the physical network ID stays unchanged. Preserve loaded membership during
     * that synchronous walk so members do not receive transient remove/add snapshots.
     */
    public static void beginGeneratorBatch(KineticBlockEntity generator) {
        beginBatch();
        if (generator == null || generator.getLevel() == null || generator.getLevel().isClientSide
            || generator.hasSource() || generator.getGeneratedSpeed() == 0
            || generator.network == null || generator.network.longValue() != generator.getBlockPos().asLong()) {
            return;
        }
        KineticNetwork network = generator.getOrCreateNetwork();
        if (network != null) {
            BATCH.get().preservedTopologyNetworks.add(network);
        }
    }

    /**
     * Retains the accounting-map entry while presenting a temporarily unassigned network ID to
     * Create's cycle detector. The new source will restore the same ID; a real topology split or
     * source loss still follows Create's ordinary remove path.
     */
    public static boolean preserveGeneratorMembership(KineticBlockEntity member, Long targetNetwork) {
        if (targetNetwork != null || !isBatching() || member == null || member.network == null) {
            return false;
        }
        for (KineticNetwork network : BATCH.get().preservedTopologyNetworks) {
            if (network.id != null && network.id.equals(member.network)) {
                // RotationPropagator checks network IDs before assigning a new source. Clearing
                // only the member's ID lets its unchanged members be rebound without rebuilding
                // the network's member map; the batch rebuilds totals after the walk.
                Batch batch = BATCH.get();
                batch.temporarilyUnassigned.put(member, network);
                member.network = null;
                if (batch.invalidatedTopologyNetworks.add(network)) invalidate(network);
                else queueNetwork(network);
                return true;
            }
        }
        return false;
    }

    /** Resolves a temporarily unassigned member before Create attaches it to its next network. */
    public static void prepareGeneratorRebind(KineticBlockEntity member, Long requestedNetwork) {
        if (requestedNetwork == null || !isBatching() || member == null) return;
        KineticNetwork previous = BATCH.get().temporarilyUnassigned.remove(member);
        if (previous != null && previous.id != null && !previous.id.equals(requestedNetwork)
            && previous.members.containsKey(member)) {
            // This branch truly changed network. Restore Create's normal remove behavior before
            // the destination network receives the member.
            previous.remove(member);
        }
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
        List<Map.Entry<KineticBlockEntity, KineticNetwork>> detached =
            new ArrayList<>(batch.temporarilyUnassigned.entrySet());
        BATCH.remove();
        for (Map.Entry<KineticBlockEntity, KineticNetwork> entry : detached) {
            if (entry.getKey().network == null && entry.getValue().members.containsKey(entry.getKey())) {
                entry.getValue().remove(entry.getKey());
            }
        }
        for (KineticNetwork network : pending) {
            try {
                updateNetworkAndPublishNow(network);
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
    }

    private static void invalidateCapacity(KineticNetwork network) {
        NetworkCache cache = cacheFor(network);
        cache.capacityValid = false;
        dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry.invalidate(network);
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
        private boolean stressRebuildFailed;
        private boolean publishedOverloadKnown;
        private boolean publishedOverloaded;
    }

    private static final class Batch {
        private int depth;
        private final Set<KineticNetwork> dirtyNetworks =
            Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        private final Set<KineticNetwork> preservedTopologyNetworks =
            Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        private final Set<KineticNetwork> invalidatedTopologyNetworks =
            Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        private final Map<KineticBlockEntity, KineticNetwork> temporarilyUnassigned =
            new java.util.IdentityHashMap<>();
    }
}
