package dev.szx.dimensionworks.mekstress.integration.mek;

import dev.szx.dimensionworks.rpmlimit.api.IQuotaGateService;
import dev.szx.dimensionworks.rpmlimit.api.IQuotaGateService.QuotaFamily;
import dev.szx.dimensionworks.rpmlimit.api.IQuotaGateService.QuotaRequest;
import dev.szx.dimensionworks.rpmlimit.api.RpmLimitServices;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.SawmillRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Applies Gear Heart quotas to one Mekanism cached-recipe operation batch via rpmlimit.api. */
public final class MekProcessingGate {
    private static TagKey<Item> crossProcessTag() {
        return TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("gear_heart", "cross_process"));
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

        IQuotaGateService quota = RpmLimitServices.quota();
        UUID owner = context.owner();
        if (!quota.hasQuota(owner) && !owner.equals(quota.sharedOwner())) {
            return requested;
        }

        int repairedMask = quota.activeQuotaMask(owner);
        int activeLimit = configuredActiveLimit(quota, repairedMask);
        if (!canReserveActive(quota, context.machine(), owner, repairedMask,
            context.tick(), activeLimit)) {
            return 0;
        }

        List<QuotaRequest> requests = new ArrayList<>();
        if (context.oreProcessing()) {
            addRateRequests(quota, requests, owner, QuotaFamily.MEK_ORE_OPERATIONS,
                requested, repairedMask, context.tick());
        }
        if (hasCrossProcessInput(recipe)) {
            addRateRequests(quota, requests, owner, QuotaFamily.CROSS_PROCESS,
                requested, repairedMask, context.tick());
        }

        if (!quota.tryQuotaPlan(requests)) {
            return 0;
        }
        markActive(quota, context.machine(), owner, repairedMask, context.tick());
        return requested;
    }

    static boolean reserveActive(Object machine, UUID owner, int repairedMask,
                                 long tick, int limit) {
        IQuotaGateService quota = RpmLimitServices.quota();
        if (!canReserveActive(quota, machine, owner, repairedMask, tick, limit)) {
            return false;
        }
        markActive(quota, machine, owner, repairedMask, tick);
        return true;
    }

    static boolean canReserveActive(IQuotaGateService quota, Object machine, UUID owner, int repairedMask,
                                    long tick, int limit) {
        if (machine == null || owner == null || limit <= 0) {
            return false;
        }
        if (quota.isRepairApplied(repairedMask, QuotaFamily.ACTIVE_MACHINES)) {
            return true;
        }

        synchronized (ACTIVES) {
            if (owner.equals(quota.sharedOwner())) {
                Set<UUID> owners = quota.unreleasedOwners(QuotaFamily.ACTIVE_MACHINES);
                if (owners.isEmpty()) {
                    return true;
                }
                for (UUID cursedOwner : owners) {
                    if (!canReserveForOwner(quota, machine, cursedOwner, tick, limit)) {
                        return false;
                    }
                }
                return true;
            }
            return canReserveForOwner(quota, machine, owner, tick, limit);
        }
    }

    static void markActive(IQuotaGateService quota, Object machine, UUID owner, int repairedMask, long tick) {
        if (machine == null || owner == null
            || quota.isRepairApplied(repairedMask, QuotaFamily.ACTIVE_MACHINES)) {
            return;
        }
        synchronized (ACTIVES) {
            if (owner.equals(quota.sharedOwner())
                && quota.unreleasedOwners(QuotaFamily.ACTIVE_MACHINES).isEmpty()) {
                return;
            }
            ACTIVES.put(machine, new ActiveState(owner, tick));
        }
    }

    static int activeCount(UUID owner, long tick) {
        if (owner == null) {
            return 0;
        }
        UUID shared = RpmLimitServices.quota().sharedOwner();
        int count = 0;
        synchronized (ACTIVES) {
            for (ActiveState state : ACTIVES.values()) {
                boolean recent = state.tick() == tick || state.tick() == tick - 1L;
                boolean matches = state.owner().equals(owner)
                    || (!owner.equals(shared) && state.owner().equals(shared));
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

    private static boolean canReserveForOwner(IQuotaGateService quota, Object machine, UUID owner, long tick, int limit) {
        ActiveState state = ACTIVES.get(machine);
        if (state != null && state.owner().equals(owner)
            && state.tick() >= tick - 1L && state.tick() <= tick) {
            return true;
        }
        return activeCount(owner, tick) < limit;
    }

    private static int configuredActiveLimit(IQuotaGateService quota, int repairedMask) {
        double rate = quota.configuredRateLimit(repairedMask, QuotaFamily.ACTIVE_MACHINES);
        return Double.isInfinite(rate) ? Integer.MAX_VALUE : (int) rate;
    }

    private static void addRateRequests(
            IQuotaGateService quota,
            List<QuotaRequest> requests,
            UUID owner, QuotaFamily family, int amount, int repairedMask, long tick) {
        Set<UUID> owners;
        double rate;
        if (owner.equals(quota.sharedOwner())) {
            owners = quota.unreleasedOwners(family);
            if (owners.isEmpty()) {
                return;
            }
            rate = quota.configuredRateLimit(0, family);
        } else {
            if (!quota.hasQuota(owner) || quota.isRepairApplied(repairedMask, family)) {
                return;
            }
            owners = Set.of(owner);
            rate = quota.configuredRateLimit(repairedMask, family);
        }

        if (Double.isInfinite(rate)) {
            return;
        }
        for (UUID quotaOwner : owners) {
            requests.add(new QuotaRequest(quotaOwner, family, amount, rate, tick));
        }
    }

    private static boolean hasCrossProcessInput(MekanismRecipe recipe) {
        if (recipe instanceof ItemStackToItemStackRecipe itemRecipe) {
            return containsTag(itemRecipe.getInput().getRepresentations());
        }
        if (recipe instanceof ItemStackChemicalToItemStackRecipe itemChemicalRecipe) {
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

    private record ActiveState(UUID owner, long tick) {
    }
}
