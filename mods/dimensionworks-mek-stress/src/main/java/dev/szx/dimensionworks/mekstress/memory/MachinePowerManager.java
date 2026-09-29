package dev.szx.dimensionworks.mekstress.memory;

import appeng.api.config.Actionable;
import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.core.MachineTier;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Routes eligible Mekanism machines through one adjacent Create kinetic source. */
public final class MachinePowerManager {

    private MachinePowerManager() {
    }

    public static boolean eligible(TileEntityMekanism tile) {
        return StressRules.isEligible(tile);
    }

    public static MachineTier tier(TileEntityMekanism tile) {
        return StressRules.machineTier(tile);
    }

    public static double workRate(TileEntityMekanism tile, long gameTick) {
        DirectSource direct = directSource(tile);
        if (direct == null || direct.source().isOverStressed()) {
            return 0.0D;
        }
        int tierRpm = MekStressConfig.machineRpm(tier(tile));
        return tierRpm <= 0 ? 0.0D : Math.max(0.0D, (double) direct.rpm / tierRpm);
    }

    public static boolean consumeForProcessing(TileEntityMekanism tile, long gameTick, Actionable mode) {
        DirectSource direct = directSource(tile);
        return direct != null && !direct.source().isOverStressed();
    }

    public static long convertedSuDemand(TileEntityMekanism tile, long gameTick) {
        if (tile.getLevel() == null || tile.getLevel().isClientSide) {
            return 0L;
        }
        GlobalPos machine = GlobalPos.of(tile.getLevel().dimension(), tile.getBlockPos());
        long current = ProcessingEnergyRegistry.convertedSu(machine, gameTick, MekStressConfig.fePerSu());
        return current > 0L
            ? current
            : ProcessingEnergyRegistry.convertedSu(machine, gameTick - 1L, MekStressConfig.fePerSu());
    }

    @Nullable
    public static DirectSource directSource(TileEntityMekanism tile) {
        Level level = tile.getLevel();
        if (level == null || level.isClientSide) {
            return null;
        }
        KineticBlockEntity best = null;
        float bestRpm = 0.0F;
        for (Direction direction : Direction.values()) {
            BlockPos pos = tile.getBlockPos().relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof KineticBlockEntity kinetic)) {
                continue;
            }
            if (!dimensionworks$isGearboxPowered(kinetic)) {
                continue;
            }
            float rpm = Math.abs(RpmLimitManager.clamp(kinetic, kinetic.getTheoreticalSpeed()));
            rpm = Math.min(rpm, MekStressConfig.directMaxRpm());
            if (rpm > bestRpm) {
                best = kinetic;
                bestRpm = rpm;
            }
        }
        if (best == null || bestRpm <= 0.0F) {
            return null;
        }
        return new DirectSource(best, bestRpm);
    }

    public static void updateDirectStress(TileEntityMekanism tile, @Nullable DirectSource direct, long gameTick) {
        GlobalPos machine = GlobalPos.of(tile.getLevel().dimension(), tile.getBlockPos());
        long convertedSu = direct == null
            ? 0L
            : ProcessingEnergyRegistry.convertedSu(machine, gameTick - 1L, MekStressConfig.fePerSu());
        DirectStressRegistry.Change change = direct == null || convertedSu <= 0L
            ? DirectStressRegistry.clear(machine)
            : DirectStressRegistry.assign(machine, direct.source(), dimensionworks$stressContribution(direct, convertedSu));
        if (!change.changed()) {
            return;
        }
        refreshStress(change.previous());
        if (change.current() != change.previous()) {
            refreshStress(change.current());
        }
    }

    public static void clearForRemoval(TileEntityMekanism tile) {
        Level level = tile.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        GlobalPos machine = GlobalPos.of(level.dimension(), tile.getBlockPos());
        ProcessingEnergyRegistry.clear(machine);
        updateDirectStress(tile, null, level.getGameTime());
    }

    private static boolean dimensionworks$isGearboxPowered(KineticBlockEntity kinetic) {
        if (kinetic instanceof MEGearboxBlockEntity gearbox) {
            return gearbox.getMode() == MEGearboxBlockEntity.Mode.EXPORT;
        }
        if (!kinetic.hasNetwork()) {
            return false;
        }
        for (KineticBlockEntity source : kinetic.getOrCreateNetwork().sources.keySet()) {
            if (source instanceof MEGearboxBlockEntity gearbox
                && gearbox.getMode() == MEGearboxBlockEntity.Mode.EXPORT) {
                return true;
            }
        }
        return false;
    }

    private static float dimensionworks$stressContribution(DirectSource direct, long convertedSu) {
        float sourceSpeed = Math.abs(direct.source().getTheoreticalSpeed());
        if (sourceSpeed < 1.0E-4F) {
            return 0.0F;
        }
        float effectiveScale = RpmLimitManager.effectiveScale(direct.source(), sourceSpeed);
        if (effectiveScale <= 1.0E-4F) {
            return 0.0F;
        }
        return (float) (convertedSu / (sourceSpeed * effectiveScale));
    }

    public record DirectSource(KineticBlockEntity source, float rpm) {
    }

    private static void refreshStress(@Nullable KineticBlockEntity source) {
        if (source == null || source.isRemoved() || source.getLevel() == null || source.getLevel().isClientSide) {
            return;
        }
        KineticNetwork network = source.getOrCreateNetwork();
        network.updateStressFor(source, source.calculateStressApplied());
        network.updateStress();
    }
}
