package dev.szx.dimensionworks.mekstress.integration.mekgenerators;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import mekanism.generators.common.tile.TileEntityGenerator;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Turns a Mekanism generator's front energy port into a Create stress source. */
public final class GeneratorPowerBridge {
    public static float outputRpm() { return MekStressConfig.generatorRpm(); }

    private GeneratorPowerBridge() {}

    public static float stressPerRpm(KineticBlockEntity shaft) {
        if (shaft.getLevel() == null || shaft.getLevel().isClientSide
            || !(shaft.getBlockState().getBlock() instanceof IRotate rotate)) return 0;
        for (Direction side : Direction.values()) {
            BlockEntity adjacent = shaft.getLevel().getBlockEntity(shaft.getBlockPos().relative(side));
            if (!(adjacent instanceof TileEntityGenerator generator) || generator.isRemoved()) continue;
            Direction generatorToShaft = side.getOpposite();
            if (generator.getDirection() != generatorToShaft) continue;
            if (!rotate.hasShaftTowards(shaft.getLevel(), shaft.getBlockPos(), shaft.getBlockState(), side)) continue;
            double fePerTick = generator.getMaxOutput().doubleValue();
            if (!Double.isFinite(fePerTick) || fePerTick <= 0) return 0;
            return (float) Math.min(Float.MAX_VALUE, fePerTick * 2.0 / outputRpm());
        }
        return 0;
    }
}
