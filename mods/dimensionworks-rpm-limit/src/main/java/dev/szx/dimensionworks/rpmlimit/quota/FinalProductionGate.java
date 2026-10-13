package dev.szx.dimensionworks.rpmlimit.quota;

import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import dev.szx.dimensionworks.rpmlimit.GearHeartState;
import dev.szx.dimensionworks.rpmlimit.RpmLimitConfig;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalTags;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.slurry.Slurry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Set;
import java.util.UUID;

/** Applies the seventh Gear Heart quota at Create/Mek output boundaries. */
public final class FinalProductionGate {
    private FinalProductionGate() {
    }

    public static boolean tryConsume(UUID owner, ItemStack output, long amount, long tick) {
        if (amount <= 0L || output == null || output.isEmpty() || !isFinalProduction(output)) {
            return true;
        }
        return consume(owner, amount, tick);
    }

    public static boolean tryConsume(UUID owner, FluidStack output, long amount, long tick) {
        if (amount <= 0L || output == null || output.isEmpty()
            || !output.getFluid().is(TagKey.create(
                Registries.FLUID, new ResourceLocation("gear_heart", "final_production")))) {
            return true;
        }
        return consume(owner, amount, tick);
    }

    public static boolean tryConsume(UUID owner, ChemicalStack<?> output, long amount, long tick) {
        if (amount <= 0L || output == null || output.isEmpty()) {
            return amount <= 0L;
        }

        ResourceLocation name = new ResourceLocation("gear_heart", "final_production");
        boolean tagged = false;
        if (output.getType() instanceof Gas gas) {
            tagged = gas.is(ChemicalTags.GAS.tag(name));
        } else if (output.getType() instanceof InfuseType infuseType) {
            tagged = infuseType.is(ChemicalTags.INFUSE_TYPE.tag(name));
        } else if (output.getType() instanceof Pigment pigment) {
            tagged = pigment.is(ChemicalTags.PIGMENT.tag(name));
        } else if (output.getType() instanceof Slurry slurry) {
            tagged = slurry.is(ChemicalTags.SLURRY.tag(name));
        }
        return !tagged || consume(owner, amount, tick);
    }

    public static boolean tryConsumeOutputs(UUID owner, Iterable<ItemStack> outputs, long tick) {
        if (outputs == null) {
            return true;
        }
        long amount = 0L;
        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty() || !isFinalProduction(output)) {
                continue;
            }
            long count = output.getCount();
            if (count <= 0L || amount > Long.MAX_VALUE - count) {
                amount = Long.MAX_VALUE;
            } else {
                amount += count;
            }
        }
        return amount == 0L || consume(owner, amount, tick);
    }

    static double configuredRate(int repairedMask, int cursedRate, int repairedRate) {
        double policy = GearHeartQuotaPolicy.limit(GearHeartQuotaType.FINAL_PRODUCTION, repairedMask);
        if (Double.isInfinite(policy)) {
            return policy;
        }
        int configured = GearHeartState.isRepairApplied(
            repairedMask, GearHeartState.FINAL_PRODUCTION) ? repairedRate : cursedRate;
        return Math.min(configured, policy);
    }

    private static boolean consume(UUID owner, long amount, long tick) {
        if (!GearHeartEffects.hasQuota(owner) && !owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
            return true;
        }

        if (owner.equals(GearHeartOwnerResolver.SHARED_OWNER)) {
            Set<UUID> owners = GearHeartEffects.unreleasedOwners(GearHeartState.FINAL_PRODUCTION);
            if (owners.isEmpty()) {
                return true;
            }
            double rate = configuredRate(0,
                RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_CURSED_PER_SECOND.get(),
                RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_REPAIRED_PER_SECOND.get());
            return Double.isInfinite(rate) || GearHeartEffects.tryQuotaForOwners(
                owners, GearHeartQuotaType.FINAL_PRODUCTION, amount, rate, tick);
        }

        int repairedMask = GearHeartEffects.activeQuotaMask(owner);
        if (GearHeartState.isRepairApplied(repairedMask, GearHeartState.FINAL_PRODUCTION)) {
            return true;
        }
        double rate = configuredRate(repairedMask,
            RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_CURSED_PER_SECOND.get(),
            RpmLimitConfig.GEAR_HEART_FINAL_PRODUCTION_REPAIRED_PER_SECOND.get());
        return Double.isInfinite(rate) || GearHeartEffects.tryQuota(
            owner, GearHeartQuotaType.FINAL_PRODUCTION, amount, rate, tick);
    }

    private static boolean isFinalProduction(ItemStack output) {
        return output.is(TagKey.create(Registries.ITEM,
            new ResourceLocation("gear_heart", "final_production")));
    }
}
