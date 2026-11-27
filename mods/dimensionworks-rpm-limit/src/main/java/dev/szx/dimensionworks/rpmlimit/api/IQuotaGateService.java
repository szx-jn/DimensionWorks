package dev.szx.dimensionworks.rpmlimit.api;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface IQuotaGateService {
    enum QuotaFamily {
        ROTATION_SPEED,
        CREATE_CAPACITY,
        MEK_ORE_OPERATIONS,
        AE_CRAFTING_JOBS,
        CROSS_PROCESS,
        ACTIVE_MACHINES,
        FINAL_PRODUCTION
    }

    record QuotaRequest(UUID owner, QuotaFamily family, double amount, double perSecond, long tick) {
    }

    UUID sharedOwner();

    UUID normalizeOwner(UUID owner);

    boolean hasQuota(UUID owner);

    int activeQuotaMask(UUID owner);

    Set<UUID> unreleasedOwners(QuotaFamily family);

    boolean isRepairApplied(int repairedMask, QuotaFamily family);

    double configuredRateLimit(int repairedMask, QuotaFamily family);

    boolean tryQuotaPlan(Collection<QuotaRequest> requests);

    boolean tryConsumeFinalItem(UUID owner, ItemStack output, long amount, long tick);

    boolean tryConsumeFinalFluid(UUID owner, FluidStack output, long amount, long tick);

    boolean tryConsumeFinalTagged(UUID owner, boolean taggedAsFinalProduction, long amount, long tick);
}
