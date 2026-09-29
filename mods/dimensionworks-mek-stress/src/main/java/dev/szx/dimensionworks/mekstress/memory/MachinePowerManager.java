package dev.szx.dimensionworks.mekstress.memory;

import appeng.api.config.Actionable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.core.MachineTier;
import dev.szx.dimensionworks.mekstress.core.NetworkMath;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

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
        MachinePowerRegistry.AeRoute route = MachinePowerRegistry.get(tile, gameTick);
        if (route != null) {
            int tierRpm = MekStressConfig.machineRpm(route.tier());
            if (route.effectiveRpm() <= 0 || tierRpm <= 0) {
                return 0.0D;
            }
            double q = route.q(gameTick);
            return NetworkMath.productionRate((double) route.effectiveRpm() / tierRpm, q,
                MekStressConfig.efficiencyExponent());
        }

        DirectSource direct = directSource(tile);
        if (direct == null) {
            return 0.0D;
        }
        if (direct.source().isOverStressed()) {
            return 0.0D;
        }
        int tierRpm = MekStressConfig.machineRpm(tier(tile));
        return tierRpm <= 0 ? 0.0D : Math.max(0.0D, (double) direct.rpm / tierRpm);
    }

    public static boolean consumeForProcessing(TileEntityMekanism tile, long gameTick, Actionable mode) {
        MachinePowerRegistry.AeRoute route = MachinePowerRegistry.get(tile, gameTick);
        if (route != null) {
            if (route.service() == null || route.effectiveRpm() <= 0) {
                return false;
            }
            double q = route.q(gameTick);
            if (q <= 0.0D) {
                return false;
            }
            long amount = Math.max(1L, Math.round(route.effectiveRpm() * 8.0D * q));
            return MachinePowerRegistry.consume(tile, amount, mode, gameTick) == amount;
        }
        return directSource(tile) != null;
    }

    public static boolean hasAeRoute(TileEntityMekanism tile, long gameTick) {
        return MachinePowerRegistry.get(tile, gameTick) != null;
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

    public static void updateDirectStress(TileEntityMekanism tile, DirectSource direct) {
        GlobalPos machine = GlobalPos.of(tile.getLevel().dimension(), tile.getBlockPos());
        DirectStressRegistry.Change change = direct == null
            ? DirectStressRegistry.clear(machine)
            : DirectStressRegistry.assign(machine, direct.source, dimensionworks$stressContribution(direct));
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
        MachinePowerRegistry.clear(machine);
        updateDirectStress(tile, null);
    }

    private static float dimensionworks$stressContribution(DirectSource direct) {
        float sourceSpeed = Math.abs(direct.source().getTheoreticalSpeed());
        if (sourceSpeed < 1.0E-4F) {
            return 0.0F;
        }
        float effectiveScale = RpmLimitManager.effectiveScale(direct.source(), sourceSpeed);
        if (effectiveScale <= 1.0E-4F) {
            return 0.0F;
        }
        return direct.rpm() * 8.0F / (sourceSpeed * effectiveScale);
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
