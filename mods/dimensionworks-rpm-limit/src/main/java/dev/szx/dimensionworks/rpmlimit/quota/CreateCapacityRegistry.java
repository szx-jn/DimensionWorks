package dev.szx.dimensionworks.rpmlimit.quota;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.GearHeartState;
import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import dev.szx.dimensionworks.rpmlimit.RpmLimitConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Weakly tracks loaded kinetic networks so each cursed owner's aggregate natural capacity can be
 * scaled without scanning unloaded chunks or recursively recalculating network capacity.
 */
public final class CreateCapacityRegistry {
    private static final Map<KineticNetwork, Map<UUID, Double>> SAMPLES =
        Collections.synchronizedMap(new WeakHashMap<>());

    /** Aggregates the cached per-network samples. Rebuilding this by walking SAMPLES on every
     * capacity read makes G networks cost G squared map visits. */
    private static final Map<UUID, Double> RAW_TOTALS = new HashMap<>();
    private static final Map<KineticNetwork, Float> NATURAL_CAPACITIES =
        Collections.synchronizedMap(new WeakHashMap<>());

    private static final java.util.Set<KineticNetwork> PENDING =
        java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean pendingAll;

    private CreateCapacityRegistry() {
    }

    public static float limitNaturalCapacity(KineticNetwork network, float naturalCapacity) {
        if (network == null || !Float.isFinite(naturalCapacity)) return naturalCapacity;
        if (naturalCapacity <= 0.0F) {
            registerOwners(network, Map.of());
            return naturalCapacity;
        }

        if (activeMember(network) == null) {
            return naturalCapacity;
        }

        Map<UUID, Double> contributions = SAMPLES.get(network);
        Float sampledNaturalCapacity = NATURAL_CAPACITIES.get(network);
        if (contributions == null || sampledNaturalCapacity == null
            || Float.compare(sampledNaturalCapacity, naturalCapacity) != 0) {
            registerOwners(network, ownerCapacities(network, naturalCapacity));
            NATURAL_CAPACITIES.put(network, naturalCapacity);
            contributions = SAMPLES.get(network);
        }

        Map<UUID, Double> raw = rawCapacities();
        double limit = RpmLimitConfig.GEAR_HEART_CREATE_CAPACITY_CURSED_SU.get();
        double sharedRaw = raw.getOrDefault(GearHeartOwnerResolver.SHARED_OWNER, 0.0D);
        double sharedScaled = sharedRaw > 0.0D
            ? scaleBucket(raw, GearHeartOwnerResolver.SHARED_OWNER,
                Math.min(limit, GearHeartQuotaPolicy.limit(
                    GearHeartQuotaType.CREATE_CAPACITY, 0)))
            : 0.0D;

        double scaledTotal = 0.0D;
        for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {
            UUID owner = entry.getKey();
            double contribution = entry.getValue();
            if (owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
                scaledTotal += GearHeartEffects.anyQuotaUnreleased(GearHeartState.CREATE_CAPACITY)
                    ? applyOwnerQuota(
                        raw, owner, (float) contribution, 0, limit, 0.0D)
                    : contribution;
                continue;
            }
            if (!GearHeartEffects.hasQuota(owner)) {
                scaledTotal += contribution;
                continue;
            }
            scaledTotal += applyOwnerQuota(
                raw, owner, (float) contribution,
                GearHeartEffects.activeQuotaMask(owner), limit, sharedScaled);
        }
        if (!Double.isFinite(scaledTotal) || scaledTotal < 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(naturalCapacity, scaledTotal);
    }

    static float applyOwnerQuota(Map<UUID, Double> raw, UUID owner, float naturalCapacity,
                                 int repairedMask, double target, double sharedScaled) {
        double policyLimit = GearHeartQuotaPolicy.limit(
            GearHeartQuotaType.CREATE_CAPACITY, repairedMask);
        if (Double.isInfinite(policyLimit)) {
            return naturalCapacity;
        }
        double effectiveTarget = Math.min(target, policyLimit);
        double ownerBudget = Math.max(0.0D, effectiveTarget - sharedScaled);
        return scaledCurrent(raw, owner, naturalCapacity, ownerBudget);
    }

    public static void register(KineticNetwork network, UUID owner, float naturalCapacity) {
        if (network == null || owner == null || !Float.isFinite(naturalCapacity)) {
            return;
        }
        registerOwners(network, Collections.singletonMap(owner, Math.max(0.0D, naturalCapacity)));
    }

    private static void registerOwners(KineticNetwork network, Map<UUID, Double> contributions) {
        if (network == null || contributions == null) return;
        Map<UUID, Double> next = Map.copyOf(contributions);
        Map<UUID, Double> previous = SAMPLES.get(network);
        if (previous != null) {
            previous.forEach((owner, value) -> addToRaw(owner, -value));
        }
        if (next.isEmpty()) {
            SAMPLES.remove(network);
            NATURAL_CAPACITIES.remove(network);
        } else {
            SAMPLES.put(network, next);
            next.forEach(CreateCapacityRegistry::addToRaw);
        }
        if (java.util.Objects.equals(previous, next) || previous == null && next.isEmpty()) return;
        synchronized (SAMPLES) {
            pendingAll = true;
            PENDING.remove(network); // This caller is already calculating its current allocation.
        }
    }

    /** Drop a network's cached owner sample after a source, topology, or RPM contribution changes. */
    public static void invalidate(KineticNetwork network) {
        if (network == null) return;
        Map<UUID, Double> previous = SAMPLES.remove(network);
        NATURAL_CAPACITIES.remove(network);
        if (previous == null) return;
        previous.forEach((owner, value) -> addToRaw(owner, -value));
        pendingAll = true;
    }

    private static void addToRaw(UUID owner, double delta) {
        double next = RAW_TOTALS.getOrDefault(owner, 0.0D) + delta;
        if (!Double.isFinite(next) || next <= 0.0D) RAW_TOTALS.remove(owner);
        else RAW_TOTALS.put(owner, next);
    }

    public static void requestReallocation() {
        pendingAll = true;
    }

    public static void clearRemoved() {
        synchronized (SAMPLES) {
            var iterator = SAMPLES.entrySet().iterator();
            Map<UUID, Double> rebuiltTotals = new HashMap<>();
            while (iterator.hasNext()) {
                Map.Entry<KineticNetwork, Map<UUID, Double>> entry = iterator.next();
                if (shouldRetainNetwork(isActive(entry.getKey()))) {
                    entry.getValue().forEach((owner, value) ->
                        rebuiltTotals.merge(owner, value, CreateCapacityRegistry::saturatedAdd));
                    continue;
                }
                NATURAL_CAPACITIES.remove(entry.getKey());
                iterator.remove();
            }
            // WeakHashMap may discard collected networks before this loop can see their values.
            // Rebuild during the existing once-per-server-tick cleanup so the owner totals do
            // not retain those samples indefinitely. Per-capacity reads remain constant-time
            // with respect to the number of other networks.
            if (materiallyDifferent(RAW_TOTALS, rebuiltTotals)) pendingAll = true;
            RAW_TOTALS.clear();
            RAW_TOTALS.putAll(rebuiltTotals);
            if (pendingAll) {
                PENDING.addAll(SAMPLES.keySet());
                pendingAll = false;
            }
        }
        java.util.List<KineticNetwork> pending = new java.util.ArrayList<>(PENDING);
        PENDING.clear();
        for (KineticNetwork network : pending) {
            if (isActive(network)) {
                try { network.updateCapacity(); }
                catch (RuntimeException exception) {
                    PENDING.add(network);
                    dev.szx.dimensionworks.rpmlimit.DimensionWorksRpmLimit.LOGGER.warn(
                        "Failed to refresh a shared Create capacity allocation; retrying next tick.", exception);
                }
            }
        }
    }

    static boolean materiallyDifferent(Map<UUID, Double> before, Map<UUID, Double> after) {
        for (Map.Entry<UUID, Double> entry : before.entrySet()) {
            if (different(entry.getValue(), after.getOrDefault(entry.getKey(), 0.0D))) return true;
        }
        for (Map.Entry<UUID, Double> entry : after.entrySet()) {
            if (!before.containsKey(entry.getKey())
                && different(0.0D, entry.getValue())) return true;
        }
        return false;
    }

    private static boolean different(double before, double after) {
        if (!Double.isFinite(before) || !Double.isFinite(after)) return before != after;
        double scale = Math.max(Math.abs(before), Math.abs(after));
        double tolerance = Math.max(Math.ulp(scale) * 8.0D, scale * 1.0e-12D);
        return Math.abs(before - after) > tolerance;
    }

    static boolean shouldRetainNetwork(boolean active) {
        return active;
    }

    public static double scaleBucket(Map<UUID, Double> capacities, UUID bucketOwner, double target) {
        if (bucketOwner == null || !Double.isFinite(target) || target <= 0.0D) {
            return 0.0D;
        }

        Double sample = capacities.get(bucketOwner);
        double total = sample != null && Double.isFinite(sample) && sample > 0.0D ? sample : 0.0D;
        if (!Double.isFinite(total) || total <= 0.0D) {
            return 0.0D;
        }
        return total <= target ? total : target;
    }

    private static float scaledCurrent(Map<UUID, Double> raw,
                                       UUID owner, float naturalCapacity, double bucketTarget) {
        Double sample = raw.get(owner);
        double rawTotal = sample != null && Double.isFinite(sample) && sample > 0.0D ? sample : 0.0D;
        if (rawTotal <= 0.0D || rawTotal > Double.MAX_VALUE) {
            return naturalCapacity;
        }

        double scaledTotal = scaleBucket(raw, owner, bucketTarget);
        double ratio = Math.max(0.0D, Math.min(1.0D, scaledTotal / rawTotal));
        double result = naturalCapacity * ratio;
        if (!Double.isFinite(result) || result < 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(naturalCapacity, result);
    }

    private static Map<UUID, Double> rawCapacities() {
        // All callers run on the server thread. The map is mutated only while applying one
        // network's result, after its previous references have been read.
        return RAW_TOTALS;
    }

    private static Map<UUID, Double> ownerCapacities(KineticNetwork network, float naturalCapacity) {
        Map<UUID, Double> result = new HashMap<>();
        double presentCapacity = 0.0D;
        synchronized (network.sources) {
            for (KineticBlockEntity source : network.sources.keySet()) {
                if (!isActiveMember(source)) {
                    continue;
                }
                float contribution = network.getActualCapacityOf(source);
                if (!Float.isFinite(contribution) || contribution <= 0.0F) {
                    continue;
                }
                UUID owner = GearHeartOwnerResolver.owner(source);
                result.merge(owner, (double) contribution, CreateCapacityRegistry::saturatedAdd);
                presentCapacity = saturatedAdd(presentCapacity, contribution);
            }
        }

        double unloaded = naturalCapacity - presentCapacity;
        if (unloaded > 0.0D) {
            result.merge(
                GearHeartOwnerResolver.SHARED_OWNER, unloaded,
                CreateCapacityRegistry::saturatedAdd);
        }
        if (result.isEmpty()) {
            result.put(GearHeartOwnerResolver.SHARED_OWNER, (double) naturalCapacity);
        }
        return result;
    }

    private static double saturatedAdd(double left, double right) {
        if (right > 0.0D && left > Double.MAX_VALUE - right) {
            return Double.MAX_VALUE;
        }
        return left + right;
    }

    private static KineticBlockEntity activeMember(KineticNetwork network) {
        synchronized (network.members) {
            for (KineticBlockEntity member : network.members.keySet()) {
                if (isActiveMember(member)) {
                    return member;
                }
            }
        }
        return null;
    }

    private static boolean isActive(KineticNetwork network) {
        return activeMember(network) != null;
    }

    private static boolean isActiveMember(KineticBlockEntity member) {
        return member != null && member.getLevel() != null && !member.getLevel().isClientSide
            && !member.isRemoved()
            && member.getLevel().getBlockEntity(member.getBlockPos()) == member;
    }

}
