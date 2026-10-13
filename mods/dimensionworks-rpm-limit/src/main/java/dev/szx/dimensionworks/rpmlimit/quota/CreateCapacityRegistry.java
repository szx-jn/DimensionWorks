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
    private static final Map<KineticNetwork, Sample> SAMPLES =
        Collections.synchronizedMap(new WeakHashMap<>());

    private CreateCapacityRegistry() {
    }

    public static float limitNaturalCapacity(KineticNetwork network, float naturalCapacity) {
        if (network == null || !Float.isFinite(naturalCapacity) || naturalCapacity <= 0.0F) {
            return naturalCapacity;
        }

        KineticBlockEntity member = activeMember(network);
        if (member == null) {
            return naturalCapacity;
        }

        UUID owner = GearHeartOwnerResolver.owner(member);
        register(network, owner, naturalCapacity);
        clearRemoved();

        Map<UUID, Double> raw = rawCapacities();
        double limit = RpmLimitConfig.GEAR_HEART_CREATE_CAPACITY_CURSED_SU.get();
        int repairedMask = GearHeartEffects.activeQuotaMask(owner);

        if (owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
            if (!GearHeartEffects.anyQuotaUnreleased(GearHeartState.CREATE_CAPACITY)) {
                return naturalCapacity;
            }
            return applyOwnerQuota(raw, owner, naturalCapacity, 0, limit, 0.0D);
        }

        double sharedRaw = raw.getOrDefault(GearHeartOwnerResolver.SHARED_OWNER, 0.0D);
        double sharedScaled = sharedRaw > 0.0D
            ? scaleBucket(raw, GearHeartOwnerResolver.SHARED_OWNER,
                Math.min(limit, GearHeartQuotaPolicy.limit(
                    GearHeartQuotaType.CREATE_CAPACITY, 0)))
            : 0.0D;
        return applyOwnerQuota(raw, owner, naturalCapacity, repairedMask, limit, sharedScaled);
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
        SAMPLES.put(network, new Sample(owner, Math.max(0.0D, naturalCapacity)));
    }

    public static void clearRemoved() {
        synchronized (SAMPLES) {
            SAMPLES.keySet().removeIf(CreateCapacityRegistry::isActive);
        }
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
            for (Sample sample : SAMPLES.values()) {
                result.merge(sample.owner(), sample.capacity(), Double::sum);
            }
        }
        return result;
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

    private record Sample(UUID owner, double capacity) {
    }
}
