package dev.szx.dimensionworks.mekstress.memory;

import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.core.MachineTier;
import java.util.Set;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.tile.base.TileEntityMekanism;

/** Structural eligibility and tier rules for the Mekanism bridge. */
public final class StressRules {
    private static final Set<String> EXCLUDED_CLASSES = Set.of(
        "mekanism.common.tile.TileEntityChargepad",
        "mekanism.common.tile.TileEntityEnergyCube",
        "mekanism.common.tile.TileEntityQuantumEntangloporter",
        "mekanism.common.tile.TileEntityCardboardBox",
        "mekanism.common.tile.multiblock.TileEntityInductionCasing",
        "mekanism.common.tile.multiblock.TileEntityInductionCell",
        "mekanism.common.tile.multiblock.TileEntityInductionPort",
        "mekanism.common.tile.multiblock.TileEntityInductionProvider",
        "mekanism.common.tile.transmitter.TileEntityThermodynamicConductor",
        "mekanism.common.tile.transmitter.TileEntityUniversalCable"
    );

    private StressRules() {
    }

    public static boolean isEligible(TileEntityMekanism tile) {
        if (tile == null || !tile.canHandleEnergy()) {
            return false;
        }
        String className = tile.getClass().getName();
        if (EXCLUDED_CLASSES.contains(className)
            || className.startsWith("mekanismgenerators.")
            || className.startsWith("mekanism.common.tile.multiblock.")
            || className.contains("transmitter")
            || className.contains("EnergyCube")
            || className.contains("QuantumEntangloporter")
            || className.contains("Chargepad")) {
            return false;
        }
        return className.startsWith("mekanism.common.tile.");
    }

    public static MachineTier machineTier(TileEntityMekanism tile) {
        BaseTier baseTier = Attribute.getBaseTier(tile.getBlockType());
        int ordinal = baseTier == null ? 0 : Math.min(baseTier.ordinal(), MachineTier.ULTIMATE.mekanismOrdinal());
        return MachineTier.fromMekanismOrdinal(ordinal);
    }

    public static int targetRpm(TileEntityMekanism tile) {
        return MekStressConfig.machineRpm(machineTier(tile));
    }
}
