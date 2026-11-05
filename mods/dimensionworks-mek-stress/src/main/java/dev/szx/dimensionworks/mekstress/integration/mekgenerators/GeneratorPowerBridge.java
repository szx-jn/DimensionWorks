package dev.szx.dimensionworks.mekstress.integration.mekgenerators;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.core.EnergyPowerMath;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.FloatingLong;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.generators.common.content.fusion.FusionReactorMultiblockData;
import mekanism.generators.common.content.turbine.TurbineMultiblockData;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekanism.generators.common.tile.fusion.TileEntityFusionReactorPort;
import mekanism.generators.common.tile.turbine.TileEntityTurbineValve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Measures a complete production tick, including subclasses, in Mekanism's native J. */
public final class GeneratorPowerBridge {
    private record Production(long tick, double su, Set<BlockPos> shafts) {}
    private static final Map<Object, Production> PRODUCTION = new WeakHashMap<>();
    public static float outputRpm() { return MekStressConfig.generatorRpm(); }

    private GeneratorPowerBridge() {}

    public static void tick(TileEntityGenerator generator, Runnable original) {
        IEnergyContainer energy = generator.getEnergyContainer();
        energy.setEnergy(FloatingLong.ZERO);
        try {
            original.run();
            Level level = generator.getLevel();
            if (level != null && !level.isClientSide) {
                Set<BlockPos> shafts = new LinkedHashSet<>();
                collectPorts(level, generator, shafts);
                record(generator, level, energy, shafts);
            }
        } finally {
            energy.setEnergy(FloatingLong.ZERO);
        }
    }

    public static IEnergyContainer energy(MultiblockData data) {
        if (data instanceof TurbineMultiblockData turbine) return turbine.energyContainer;
        if (data instanceof FusionReactorMultiblockData fusion) return fusion.energyContainer;
        throw new IllegalArgumentException("Unsupported mechanical producer");
    }

    public static void beforeMultiblock(MultiblockData data) {
        energy(data).setEnergy(FloatingLong.ZERO);
    }

    public static void afterMultiblock(MultiblockData data, Level level) {
        IEnergyContainer energy = energy(data);
        try {
            Set<BlockPos> shafts = new LinkedHashSet<>();
            if (data.isFormed()) {
                for (var valveData : data.valves) {
                    BlockPos pos = valveData.location;
                    if (!level.hasChunkAt(pos)) continue;
                    BlockEntity tile = level.getBlockEntity(pos);
                    if (tile instanceof TileEntityTurbineValve valve && valve.getMultiblock() == data) {
                        collectPorts(level, valve, shafts);
                    } else if (tile instanceof TileEntityFusionReactorPort port
                        && port.getMultiblock() == data && port.getActive()) {
                        collectPorts(level, port, shafts);
                    }
                }
            }
            record(data, level, energy, shafts);
        } finally {
            energy.setEnergy(FloatingLong.ZERO);
        }
    }

    private static void record(Object owner, Level level, IEnergyContainer energy, Set<BlockPos> shafts) {
        double fe = dev.szx.dimensionworks.mekstress.integration.mek.MekanismPowerUnits.fePerTick(energy.getEnergy());
        Production previous = PRODUCTION.put(owner,
            new Production(level.getGameTime(), EnergyPowerMath.su(fe), Set.copyOf(shafts)));
        Set<BlockPos> changed = new LinkedHashSet<>(shafts);
        if (previous != null) changed.addAll(previous.shafts());
        // A multiblock output can feed several Create shafts on one kinetic network. Update each
        // source contribution immediately, but publish the completed network totals only once.
        dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer.beginBatch();
        try {
            for (BlockPos pos : changed) {
                if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof dev.szx.dimensionworks.mekstress.api.IMechanicalSourceShaft shaft)
                    shaft.dimensionworks$refreshMechanicalSource();
            }
        } finally {
            dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer.endBatch();
        }
    }

    private static void collectPorts(Level level, TileEntityMekanism port, Set<BlockPos> shafts) {
        for (Direction direction : Direction.values()) {
            if (port.getEnergyContainers(direction).isEmpty()) continue;
            BlockPos pos = port.getBlockPos().relative(direction);
            if (port instanceof TileEntityTurbineValve valve && !valve.getMultiblock().isPositionOutsideBounds(pos)) continue;
            if (port instanceof TileEntityFusionReactorPort fusion && !fusion.getMultiblock().isPositionOutsideBounds(pos)) continue;
            if (!level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof KineticBlockEntity shaft
                && connectsTowards(shaft, direction.getOpposite())
                && (shaft.getTheoreticalSpeed() == 0 || shaft.getTheoreticalSpeed() == outputRpm())) {
                shafts.add(pos.immutable());
            }
        }
    }

    public static boolean connectsTowards(KineticBlockEntity kinetic, Direction face) {
        Level level = kinetic.getLevel();
        return level != null && !(kinetic instanceof GeneratingKineticBlockEntity)
            && kinetic.getBlockState().getBlock() instanceof IRotate rotate
            && rotate.hasShaftTowards(level, kinetic.getBlockPos(), kinetic.getBlockState(), face);
    }

    public static float stressPerRpm(KineticBlockEntity shaft) {
        Level level = shaft.getLevel();
        if (level == null || level.isClientSide) return 0;
        Set<Object> owners = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        double coefficient = 0;
        for (Direction side : Direction.values()) {
            if (!connectsTowards(shaft, side)) continue;
            BlockPos pos = shaft.getBlockPos().relative(side);
            if (!level.hasChunkAt(pos)) continue;
            BlockEntity tile = level.getBlockEntity(pos);
            Object owner = null;
            if (tile instanceof TileEntityGenerator generator && !generator.isRemoved()
                && !generator.getEnergyContainers(side.getOpposite()).isEmpty()) owner = generator;
            else if (tile instanceof TileEntityTurbineValve valve && valve.getMultiblock().isFormed()) owner = valve.getMultiblock();
            else if (tile instanceof TileEntityFusionReactorPort port && port.getActive()
                && port.getMultiblock().isFormed()) owner = port.getMultiblock();
            if (owner == null || !owners.add(owner)) continue;
            Production production = PRODUCTION.get(owner);
            if (production != null && EnergyPowerMath.isLive(production.tick(), level.getGameTime())
                && production.shafts().contains(shaft.getBlockPos())) {
                coefficient += EnergyPowerMath.coefficient(production.su(), outputRpm(), production.shafts().size());
            }
        }
        return EnergyPowerMath.coefficient(coefficient, 1, 1);
    }
}
