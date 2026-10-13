package dev.szx.dimensionworks.rpmlimit.quota;

import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import dev.szx.dimensionworks.rpmlimit.GearHeartState;
import dev.szx.dimensionworks.rpmlimit.RpmLimitConfig;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.chemical.ItemStackChemicalToItemStackRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Applies Gear Heart quotas to one Mekanism cached-recipe operation batch. */
public final class MekProcessingGate {
    private static TagKey<Item> crossProcessTag() {
        return TagKey.create(Registries.ITEM,
            new ResourceLocation("gear_heart", "cross_process"));
    }
    private static final Map<Object, ActiveState> ACTIVES =
        Collections.synchronizedMap(new WeakHashMap<>());

    private MekProcessingGate() {
    }

    public static int allowOperations(int requested, MekanismRecipe recipe,
                                      MekProcessingContext.Context context) {
        if (requested <= 0) {
            return 0;
        }
        if (context == null) {
            return requested;
        }

        UUID owner = context.owner();
        if (!GearHeartEffects.hasQuota(owner) && !owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
            return requested;
        }


        int repairedMask = GearHeartEffects.activeQuotaMask(owner);
        int activeLimit = configuredActiveLimit(repairedMask);
        if (!canReserveActive(context.machine(), owner, repairedMask,
            context.tick(), activeLimit)) {
            return 0;
        }

        java.util.List<FixedIntervalTokenBucket.QuotaRequest> requests =
            new java.util.ArrayList<>();
        if (context.oreProcessing()) {
            addRateRequests(requests, owner, GearHeartQuotaType.MEK_ORE_OPERATIONS,
                requested, repairedMask,
                RpmLimitConfig.GEAR_HEART_MEK_ORE_OPERATIONS_CURSED_PER_SECOND.get(),
                RpmLimitConfig.GEAR_HEART_MEK_ORE_OPERATIONS_REPAIRED_PER_SECOND.get(),
                context.tick());
        }
        if (hasCrossProcessInput(recipe)) {
            addRateRequests(requests, owner, GearHeartQuotaType.CROSS_PROCESS,
                requested, repairedMask,
                RpmLimitConfig.GEAR_HEART_CROSS_PROCESS_CURSED_PER_SECOND.get(),
                RpmLimitConfig.GEAR_HEART_CROSS_PROCESS_REPAIRED_PER_SECOND.get(),
                context.tick());
        }

        if (!GearHeartEffects.tryQuotaPlan(requests)) {
            return 0;
        }
        markActive(context.machine(), owner, repairedMask, context.tick());
        return requested;
    }

    static double configuredRateLimit(int repairedMask, GearHeartQuotaType type,
                                      int cursedRate, int repairedRate) {
        double policy = GearHeartQuotaPolicy.limit(type, repairedMask);
        if (type == GearHeartQuotaType.CREATE_CAPACITY || Double.isInfinite(policy)) {
            return policy;
        }

        int repairBit = repairBit(type);
        int configured = GearHeartState.isRepairApplied(repairedMask, repairBit)
            ? repairedRate : cursedRate;
        return Math.min(configured, policy);
    }

    static boolean reserveActive(Object machine, UUID owner, int repairedMask,
                                 long tick, int limit) {
        if (!canReserveActive(machine, owner, repairedMask, tick, limit)) {
            return false;
        }
        markActive(machine, owner, repairedMask, tick);
        return true;
    }

    static boolean canReserveActive(Object machine, UUID owner, int repairedMask,
                                    long tick, int limit) {
        if (machine == null || owner == null || limit <= 0) {
            return false;
        }
        if (GearHeartState.isRepairApplied(repairedMask, GearHeartState.ACTIVE_MACHINES)) {
            return true;
        }

        synchronized (ACTIVES) {
            if (owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
                Set<UUID> owners = GearHeartEffects.unreleasedOwners(GearHeartState.ACTIVE_MACHINES);
                if (owners.isEmpty()) {
                    return true;
                }
                for (UUID cursedOwner : owners) {
                    if (!canReserveForOwner(machine, cursedOwner, tick, limit)) {
                        return false;
                    }
                }
                return true;
            }
            return canReserveForOwner(machine, owner, tick, limit);
        }
    }

    static void markActive(Object machine, UUID owner, int repairedMask, long tick) {
        if (machine == null || owner == null
            || GearHeartState.isRepairApplied(repairedMask, GearHeartState.ACTIVE_MACHINES)) {
            return;
        }
        synchronized (ACTIVES) {
            if (owner.equals(GearHeartOwnerResolver.SHARED_OWNER)
                && GearHeartEffects.unreleasedOwners(GearHeartState.ACTIVE_MACHINES).isEmpty()) {
                return;
            }
            ACTIVES.put(machine, new ActiveState(owner, tick));
        }
    }

    static int activeCount(UUID owner, long tick) {
        if (owner == null) {
            return 0;
        }
        int count = 0;
        synchronized (ACTIVES) {
            for (ActiveState state : ACTIVES.values()) {
                boolean recent = state.tick() == tick || state.tick() == tick - 1L;
                boolean matches = state.owner().equals(owner)
                    || (!owner.equals(GearHeartOwnerResolver.SHARED_OWNER)
                        && state.owner().equals(GearHeartOwnerResolver.SHARED_OWNER));
                if (recent && matches) {
                    count++;
                }
            }
        }
        return count;
    }

    static void clearActives() {
        ACTIVES.clear();
    }

    private static boolean canReserveForOwner(Object machine, UUID owner, long tick, int limit) {
        ActiveState state = ACTIVES.get(machine);
        if (state != null && state.owner().equals(owner)
            && state.tick() >= tick - 1L && state.tick() <= tick) {
            return true;
        }
        return activeCount(owner, tick) < limit;
    }

    private static int configuredActiveLimit(int repairedMask) {
        double policy = GearHeartQuotaPolicy.limit(GearHeartQuotaType.ACTIVE_MACHINES, repairedMask);
        if (Double.isInfinite(policy)) {
            return Integer.MAX_VALUE;
        }
        int configured = GearHeartState.isRepairApplied(repairedMask, GearHeartState.ACTIVE_MACHINES)
            ? RpmLimitConfig.GEAR_HEART_ACTIVE_MACHINES_REPAIRED.get()
            : RpmLimitConfig.GEAR_HEART_ACTIVE_MACHINES_CURSED.get();
        return (int) Math.min(configured, policy);
    }

    private static void addRateRequests(
            java.util.List<FixedIntervalTokenBucket.QuotaRequest> requests,
            UUID owner, GearHeartQuotaType type, int amount, int repairedMask,
            int cursedRate, int repairedRate, long tick) {
        int repairBit = repairBit(type);
        Set<UUID> owners;
        double rate;
        if (owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
            owners = GearHeartEffects.unreleasedOwners(repairBit);
            if (owners.isEmpty()) {
                return;
            }
            rate = configuredRateLimit(0, type, cursedRate, repairedRate);
        } else {
            if (!GearHeartEffects.hasQuota(owner)
                || GearHeartState.isRepairApplied(repairedMask, repairBit)) {
                return;
            }
            owners = Set.of(owner);
            rate = configuredRateLimit(repairedMask, type, cursedRate, repairedRate);
        }

        if (Double.isInfinite(rate)) {
            return;
        }
        for (UUID quotaOwner : owners) {
            requests.add(new FixedIntervalTokenBucket.QuotaRequest(
                quotaOwner, type, amount, rate, tick));
        }
    }

    private static boolean hasCrossProcessInput(MekanismRecipe recipe) {
        if (recipe instanceof ItemStackToItemStackRecipe itemRecipe) {
            return containsTag(itemRecipe.getInput().getRepresentations());
        }
        if (recipe instanceof ItemStackChemicalToItemStackRecipe<?, ?, ?> itemChemicalRecipe) {
            return containsTag(itemChemicalRecipe.getItemInput().getRepresentations());
        }
        if (recipe instanceof SawmillRecipe sawmillRecipe) {
            return containsTag(sawmillRecipe.getInput().getRepresentations());
        }
        if (recipe instanceof CombinerRecipe combinerRecipe) {
            return containsTag(combinerRecipe.getMainInput().getRepresentations())
                || containsTag(combinerRecipe.getExtraInput().getRepresentations());
        }
        if (recipe instanceof PressurizedReactionRecipe reactionRecipe) {
            return containsTag(reactionRecipe.getInputSolid().getRepresentations());
        }
        return false;
    }

    private static boolean containsTag(Iterable<ItemStack> representations) {
        for (ItemStack stack : representations) {
            if (stack.is(crossProcessTag())) {
                return true;
            }
        }
        return false;
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

    private record ActiveState(UUID owner, long tick) {
    }
}
