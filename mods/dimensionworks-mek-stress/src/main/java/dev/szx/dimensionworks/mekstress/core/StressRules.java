package dev.szx.dimensionworks.mekstress.core;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.parts.automation.ExportBusPart;
import com.loliball.appliedcreate.storage.StressKeyType;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.StressFormula;
import dev.szx.dimensionworks.mekstress.api.StressPoweredMachine;
import dev.szx.dimensionworks.mekstress.card.StressOutputCardItem;
import dev.szx.dimensionworks.mekstress.card.StressOutputSettings;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import java.util.Set;
import java.util.UUID;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Shared runtime rules and lookups. */
public final class StressRules {

    private static final Set<String> DISABLED_TILE_CLASSES = Set.of(
        "mekanism.common.tile.TileEntityChargepad",
        "mekanism.common.tile.TileEntityEnergyCube",
        "mekanism.common.tile.TileEntityQuantumEntangloporter",
        "mekanism.common.tile.multiblock.TileEntityInductionCasing",
        "mekanism.common.tile.multiblock.TileEntityInductionCell",
        "mekanism.common.tile.multiblock.TileEntityInductionPort",
        "mekanism.common.tile.multiblock.TileEntityInductionProvider",
        "mekanism.common.tile.transmitter.TileEntityThermodynamicConductor",
        "mekanism.common.tile.transmitter.TileEntityUniversalCable"
    );

    private StressRules() {
    }

    public static StressPowerState powerState(TileEntityMekanism tile) {
        return ((StressPoweredMachine) tile).dimensionworks$stressPowerState();
    }

    @Nullable
    public static StressPowerState powerStateOrNull(@Nullable TileEntityMekanism tile) {
        if (tile == null || isDisabledEnergyInfrastructure(tile)) {
            return null;
        }
        return powerState(tile);
    }

    public static boolean isDisabledEnergyInfrastructure(TileEntityMekanism tile) {
        return DISABLED_TILE_CLASSES.contains(tile.getClass().getName())
            || tile.getClass().getName().startsWith("mekanismgenerators.");
    }

    public static boolean canAcceptStress(TileEntityMekanism tile) {
        return !isDisabledEnergyInfrastructure(tile);
    }

    public static boolean hasStressCard(IUpgradeableObject upgradeable) {
        return !findStressCard(upgradeable).isEmpty();
    }

    public static ItemStack findStressCard(IUpgradeableObject upgradeable) {
        StressOutputCardItem cardItem = DimensionWorksMekStress.STRESS_OUTPUT_CARD.get();
        for (ItemStack stack : upgradeable.getUpgrades()) {
            if (stack.getItem() == cardItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasConfiguredStress(ExportBusPart bus) {
        for (int slot = 0; slot < bus.getConfig().size(); slot++) {
            var key = bus.getConfig().getKey(slot);
            if (key != null && key.getType() == StressKeyType.getTYPE()) {
                return true;
            }
        }
        return false;
    }

    public static StressOutputSettings settings(ItemStack cardStack) {
        if (cardStack.getItem() instanceof StressOutputCardItem cardItem) {
            return cardItem.getSettings(cardStack);
        }
        return new StressOutputSettings(
            MekStressConfig.CARD_DEFAULT_RPM.get(),
            MekStressConfig.CARD_DEFAULT_STRESS_PER_TICK.get()
        );
    }

    public static StressOutputSettings clampSettings(@Nullable UUID owner, StressOutputSettings requested) {
        int requestedRpm = StressFormula.clampRpm(
            requested.rpm(),
            requested.rpm(),
            MekStressConfig.CARD_MAX_RPM.get()
        );
        int effectiveLimit;
        if (owner == null) {
            effectiveLimit = MekStressConfig.OWNER_FALLBACK_RPM.get();
        } else {
            effectiveLimit = RpmLimitManager.getEffectiveLimit(owner);
        }
        int rpm = StressFormula.clampRpm(requestedRpm, effectiveLimit, MekStressConfig.CARD_MAX_RPM.get());
        long stress = StressFormula.clampStress(
            requested.stressPerTick(),
            MekStressConfig.CARD_MAX_STRESS_PER_TICK.get()
        );
        return new StressOutputSettings(rpm, stress);
    }

    public static StressOutputSettings effectiveSettings(@Nullable UUID owner, ItemStack cardStack) {
        return clampSettings(owner, settings(cardStack));
    }

    public static boolean applySettings(IUpgradeableObject upgradeable, @Nullable UUID owner, StressOutputSettings requested) {
        ItemStack stack = findStressCard(upgradeable);
        if (stack.isEmpty() || !(stack.getItem() instanceof StressOutputCardItem cardItem)) {
            return false;
        }
        cardItem.setSettings(stack, clampSettings(owner, requested));
        return true;
    }
}
