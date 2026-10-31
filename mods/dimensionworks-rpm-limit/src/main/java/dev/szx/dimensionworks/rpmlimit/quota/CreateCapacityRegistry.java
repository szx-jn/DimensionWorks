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

    private static final java.util.Set<KineticNetwork> PENDING =
        java.util.Collections.newSetFromMap(new WeakHashMap<>());

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

        Map<UUID, Double> contributions = ownerCapacities(network, naturalCapacity);
        registerOwners(network, contributions);

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
        Map<UUID, Double> previous = next.isEmpty() ? SAMPLES.remove(network) : SAMPLES.put(network, next);
        if (java.util.Objects.equals(previous, next) || previous == null && next.isEmpty()) return;
        synchronized (SAMPLES) {
            PENDING.addAll(SAMPLES.keySet());
            PENDING.remove(network); // This caller is already calculating its current allocation.
        }
    }

    public static void requestReallocation() {
        synchronized (SAMPLES) { PENDING.addAll(SAMPLES.keySet()); }
    }

    public static void clearRemoved() {
        synchronized (SAMPLES) {
            boolean changed = SAMPLES.keySet().removeIf(network -> !shouldRetainNetwork(isActive(network)));
            if (changed) PENDING.addAll(SAMPLES.keySet());
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

    static boolean shouldRetainNetwork(boolean active) {
        return active;
    }

    public static double scaleBucket(Map<UUID, Double> capacities, UUID bucketOwner, double target) {
        if (bucketOwner == null || !Double.isFinite(target) || target <= 0.0D) {
            return 0.0D;
        }

        double total = 0.0D;
        for (Map.Entry<UUID, Double> entry : capacities.entrySet()) {
            Double value = entry.getValue();
            if (bucketOwner.equals(entry.getKey()) && value != null
                && Double.isFinite(value) && value > 0.0D) {
                total += value;
            }
        }
        if (!Double.isFinite(total) || total <= 0.0D) {
            return 0.0D;
        }
        return total <= target ? total : target;
    }

    private static float scaledCurrent(Map<UUID, Double> raw,
                                       UUID owner, float naturalCapacity, double bucketTarget) {
        double rawTotal = 0.0D;
        for (Map.Entry<UUID, Double> entry : raw.entrySet()) {
            Double value = entry.getValue();
            if (owner.equals(entry.getKey()) && value != null
                && Double.isFinite(value) && value > 0.0D) {
                rawTotal += value;
            }
        }
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
        Map<UUID, Double> result = new HashMap<>();
        synchronized (SAMPLES) {
            for (Map<UUID, Double> contributions : SAMPLES.values()) {
                contributions.forEach((owner, capacity) ->
                    result.merge(owner, capacity, CreateCapacityRegistry::saturatedAdd));
            }
        }
        return result;
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
