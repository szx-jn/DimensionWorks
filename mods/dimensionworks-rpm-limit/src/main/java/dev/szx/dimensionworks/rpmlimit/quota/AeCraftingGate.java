package dev.szx.dimensionworks.rpmlimit.quota;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEKey;
import appeng.me.service.CraftingService;
import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import dev.szx.dimensionworks.rpmlimit.GearHeartState;
import dev.szx.dimensionworks.rpmlimit.RpmLimitConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Set;
import java.util.UUID;

/** Applies AE2 crafting, active-CPU, and final-output quotas. */
public final class AeCraftingGate {
    private AeCraftingGate() {
    }

    public static boolean allowSubmission(CraftingService service, ICraftingPlan job,
                                           UUID owner, long tick) {
        if (service == null || job == null || owner == null) {
            return false;
        }
        if (!GearHeartEffects.hasQuota(owner) && !owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
            return true;
        }

        if (owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
            return allowSharedCounts(
                GearHeartEffects.unreleasedOwners(GearHeartState.AE_CRAFTING_JOBS),
                GearHeartQuotaType.AE_CRAFTING_JOBS,
                configuredJobLimit(0,
                    RpmLimitConfig.GEAR_HEART_AE_CRAFTING_JOBS_CURSED.get(),
                    RpmLimitConfig.GEAR_HEART_AE_CRAFTING_JOBS_REPAIRED.get()))
                && allowSharedCounts(
                    GearHeartEffects.unreleasedOwners(GearHeartState.ACTIVE_MACHINES),
                    GearHeartQuotaType.ACTIVE_MACHINES,
                    configuredActiveLimit(0,
                        RpmLimitConfig.GEAR_HEART_ACTIVE_MACHINES_CURSED.get(),
                        RpmLimitConfig.GEAR_HEART_ACTIVE_MACHINES_REPAIRED.get()));
        }

        int repairedMask = GearHeartEffects.activeQuotaMask(owner);
        int busy = AeCraftingRegistry.busyCountForOwner(owner);
        if (!GearHeartState.isRepairApplied(repairedMask, GearHeartState.AE_CRAFTING_JOBS)) {
            int jobLimit = configuredJobLimit(repairedMask,
                RpmLimitConfig.GEAR_HEART_AE_CRAFTING_JOBS_CURSED.get(),
                RpmLimitConfig.GEAR_HEART_AE_CRAFTING_JOBS_REPAIRED.get());
            if (busy >= jobLimit) {
                return false;
            }
        }
        if (!GearHeartState.isRepairApplied(repairedMask, GearHeartState.ACTIVE_MACHINES)) {
            int activeLimit = configuredActiveLimit(repairedMask,
                RpmLimitConfig.GEAR_HEART_ACTIVE_MACHINES_CURSED.get(),
                RpmLimitConfig.GEAR_HEART_ACTIVE_MACHINES_REPAIRED.get());
            return busy < activeLimit;
        }
        return true;
    }

    public static boolean allowFinalOutput(UUID owner, AEKey key, long amount, long tick) {
        if (amount <= 0L || key == null || !isFinalProduction(key)) {
            return true;
        }
        if (!GearHeartEffects.hasQuota(owner) && !owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
            return true;
        }

        if (owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
            Set<UUID> owners = GearHeartEffects.unreleasedOwners(GearHeartState.FINAL_PRODUCTION);
            if (owners.isEmpty()) {
                return true;
            }
            double rate = configuredFinalRate(0,
                RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_CURSED_PER_SECOND.get(),
                RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_REPAIRED_PER_SECOND.get());
            return Double.isInfinite(rate)
                || GearHeartEffects.tryQuotaForOwners(
                    owners, GearHeartQuotaType.FINAL_PRODUCTION, amount, rate, tick);
        }

        int repairedMask = GearHeartEffects.activeQuotaMask(owner);
        if (GearHeartState.isRepairApplied(repairedMask, GearHeartState.FINAL_PRODUCTION)) {
            return true;
        }
        double rate = configuredFinalRate(repairedMask,
            RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_CURSED_PER_SECOND.get(),
            RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_REPAIRED_PER_SECOND.get());
        return Double.isInfinite(rate) || GearHeartEffects.tryQuota(
            owner, GearHeartQuotaType.FINAL_PRODUCTION, amount, rate, tick);
    }

    static int configuredJobLimit(int repairedMask, int cursedLimit, int repairedLimit) {
        double policy = GearHeartQuotaPolicy.limit(GearHeartQuotaType.AE_CRAFTING_JOBS, repairedMask);
        if (Double.isInfinite(policy)) {
            return Integer.MAX_VALUE;
        }
        int configured = GearHeartState.isRepairApplied(
            repairedMask, GearHeartState.AE_CRAFTING_JOBS) ? repairedLimit : cursedLimit;
        return (int) Math.min(configured, policy);
    }

    static int configuredActiveLimit(int repairedMask, int cursedLimit, int repairedLimit) {
        double policy = GearHeartQuotaPolicy.limit(GearHeartQuotaType.ACTIVE_MACHINES, repairedMask);
        if (Double.isInfinite(policy)) {
            return Integer.MAX_VALUE;
        }
        int configured = GearHeartState.isRepairApplied(
            repairedMask, GearHeartState.ACTIVE_MACHINES) ? repairedLimit : cursedLimit;
        return (int) Math.min(configured, policy);
    }

    static double configuredFinalRate(int repairedMask, int cursedRate, int repairedRate) {
        double policy = GearHeartQuotaPolicy.limit(GearHeartQuotaType.FINAL_PRODUCTION, repairedMask);
        if (Double.isInfinite(policy)) {
            return policy;
        }
        int configured = GearHeartState.isRepairApplied(
            repairedMask, GearHeartState.FINAL_PRODUCTION) ? repairedRate : cursedRate;
        return Math.min(configured, policy);
    }

    private static boolean allowSharedCounts(Set<UUID> owners, GearHeartQuotaType type, int limit) {
        if (owners.isEmpty()) {
            return true;
        }
        int repairBit = repairBit(type);
        for (UUID owner : owners) {
            if (!GearHeartEffects.hasQuota(owner)) {
                continue;
            }
            int mask = GearHeartEffects.activeQuotaMask(owner);
            if (GearHeartState.isRepairApplied(mask, repairBit)) {
                continue;
            }
            if (AeCraftingRegistry.busyCountForOwner(owner) >= limit) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFinalProduction(AEKey key) {
        return key.isTagged(TagKey.create(Registries.ITEM,
            new ResourceLocation("gear_heart", "final_production")));
    }

    private static int repairBit(GearHeartQuotaType type) {
        return switch (type) {
            case ROTATION_SPEED -> GearHeartState.ROTATION_SPEED;
            case CREATE_CAPACITY -> GearHeartState.CREATE_CAPACITY;
            case MEK_ORE_OPERATIONS -> GearHeartState.MEK_ORE_OPERATIONS;
            case AE_CRAFTING_JOBS -> GearHeartState.AE_CRAFTING_JOBS;
            case CROSS_PROCESS -> GearHeartState.CROSS_PROCESS;
            case ACTIVE_MACHINES -> GearHeartState.ACTIVE_MACHINES;
            case FINAL_PRODUCTION -> GearHeartState.FINAL_PRODUCTION;
        };
    }
}
