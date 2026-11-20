package dev.szx.dimensionworks.mekstress.integration.mek;

import dev.szx.dimensionworks.mekstress.core.EnergyPowerMath;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import dev.szx.dimensionworks.mekstress.mixin.mek.MachineEnergyContainerAccessor;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import mekanism.api.Upgrade;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.factory.TileEntityFactory;

/** Rates include native upgrades, factory processes, recipe-specific and adjustable usage. */
public final class MechanicalEnergy {
    private static final Map<BasicEnergyContainer, WeakReference<TileEntityMekanism>> LASERS = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private record Burst(long tick, long joules, float rpm) {}
    private static final Map<TileEntityMekanism, Burst> BURSTS = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final java.util.Set<String> INSTANT_RECIPES = java.util.Set.of(
        "TileEntityChemicalWasher", "TileEntityRotaryCondensentrator", "TileEntityElectrolyticSeparator",
        "TileEntityIsotopicCentrifuge", "TileEntityChemicalInfuser", "TileEntityPigmentMixer");

    private MechanicalEnergy() {}

    public static void bindLaser(BasicEnergyContainer energy, TileEntityMekanism tile) {
        LASERS.put(energy, new WeakReference<>(tile));
    }

    public static TileEntityMekanism owner(Object container) {
        if (container instanceof MachineEnergyContainerAccessor accessor) return accessor.dimensionworks$machineTile();
        WeakReference<TileEntityMekanism> ref = LASERS.get(container);
        return ref == null ? null : ref.get();
    }

    public static long rate(Object container, TileEntityMekanism tile) {
        if (!MekanismPowerUnits.valid()) return 0L;
        Burst burst = BURSTS.get(tile);
        if (burst != null && tile.getLevel() != null && burst.tick() == tile.getLevel().getGameTime()) return burst.joules();
        if (container instanceof MachineEnergyContainer<?> machine) {
            if (tile instanceof mekanism.common.tile.multiblock.TileEntitySPSPort) {
                return machine.getMaxEnergy() / 400;
            }
            int parallel = tile instanceof TileEntityFactory<?> factory ? factory.tier.processes : 1;
            if (INSTANT_RECIPES.contains(tile.getClass().getSimpleName()) && tile.supportsUpgrades()) {
                parallel *= 1 << Math.min(8, tile.getComponent().getUpgrades(Upgrade.SPEED));
            }
            return mekanism.api.math.MathUtils.multiplyClamped(machine.getEnergyPerTick(), parallel);
        }
        return tile.getClass().getName().equals("mekanism.common.tile.laser.TileEntityLaser")
            ? MekanismConfig.usage.laser.get() : 0L;
    }

    public static float stressPerRpm(TileEntityMekanism tile) {
        long joules = 0L;
        for (var container : tile.getEnergyContainers(null)) joules = mekanism.api.math.MathUtils.addClamped(joules, rate(container, tile));
        double su = EnergyPowerMath.su(MekanismPowerUnits.fePerTick(joules));
        Burst burst = BURSTS.get(tile);
        float rpm = burst != null && tile.getLevel() != null && burst.tick() == tile.getLevel().getGameTime()
            ? burst.rpm() : StressRules.targetRpm(tile);
        return EnergyPowerMath.demandCoefficient(su, rpm);
    }

    /** Teleport cost is known only after the native destination/entity calculation. */
    public static void reserveBurst(TileEntityMekanism tile, long joules) {
        var route = dev.szx.dimensionworks.mekstress.memory.MachinePowerManager.powerRoute(tile);
        if (route == null || tile.getLevel() == null) return;
        BURSTS.put(tile, new Burst(tile.getLevel().getGameTime(), joules, route.networkRpm()));
        dev.szx.dimensionworks.mekstress.memory.MachinePowerManager.updateDirectStress(tile, route);
    }
}
