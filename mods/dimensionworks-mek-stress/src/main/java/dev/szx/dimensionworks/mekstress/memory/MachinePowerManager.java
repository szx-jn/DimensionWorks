package dev.szx.dimensionworks.mekstress.memory;

import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.core.MachinePowerMath;
import dev.szx.dimensionworks.mekstress.core.MachineFootprint;
import dev.szx.dimensionworks.mekstress.core.MachineTier;
import dev.szx.dimensionworks.mekstress.core.PowerRouteSafety;
import dev.szx.dimensionworks.mekstress.core.MachinePowerRouteScope;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import java.util.List;
import java.util.Map;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeHasBounding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Routes eligible Mekanism machines through any adjacent Create kinetic network member. */
public final class MachinePowerManager {
    private MachinePowerManager() {
    }

    public static boolean eligible(TileEntityMekanism tile) {
        return StressRules.isEligible(tile);
    }

    public static MachineTier tier(TileEntityMekanism tile) {
        return StressRules.machineTier(tile);
    }

    public static int currentRpm(TileEntityMekanism tile) {
        return currentRpm(powerRoute(tile));
    }

    public static int currentRpm(@Nullable DirectSource route) {
        return route == null || !route.running() ? 0 : Math.max(0, Math.round(route.rpm()));
    }

    public static long suPerTick(TileEntityMekanism tile) {
        return suPerTick(powerRoute(tile));
    }

    public static long suPerTick(@Nullable DirectSource route) {
        return MachinePowerMath.suForRpm(currentRpm(route));
    }

    public static boolean hasMechanicalPower(TileEntityMekanism tile) {
        return hasMechanicalPower(powerRoute(tile));
    }

    public static boolean hasMechanicalPower(@Nullable DirectSource route) {
        if (route == null) {
            return false;
        }
        return PowerRouteSafety.hasMechanicalPower(
            true, route.running(), route.rpm(), routeStillPresent(route), route.source().isOverStressed());
    }

    public static double workRate(TileEntityMekanism tile) {
        return workRate(tile, powerRoute(tile));
    }

    public static double workRate(TileEntityMekanism tile, @Nullable DirectSource route) {
        int rpm = currentRpm(route);
        int tierRpm = MekStressConfig.machineRpm(tier(tile));
        return hasMechanicalPower(route) ? MachinePowerMath.workRate(rpm, tierRpm) : 0.0D;
    }

    public static double productionMultiplier(TileEntityMekanism tile) {
        return productionMultiplier(tile, powerRoute(tile));
    }

    public static double productionMultiplier(TileEntityMekanism tile, @Nullable DirectSource route) {
        int rpm = currentRpm(route);
        int tierRpm = MekStressConfig.machineRpm(tier(tile));
        return hasMechanicalPower(route) ? MachinePowerMath.productionMultiplier(rpm, tierRpm) : 0.0D;
    }

    public static double throughputMultiplier(TileEntityMekanism tile) {
        return throughputMultiplier(tile, powerRoute(tile));
    }

    public static double throughputMultiplier(TileEntityMekanism tile, @Nullable DirectSource route) {
        int rpm = currentRpm(route);
        int tierRpm = MekStressConfig.machineRpm(tier(tile));
        return hasMechanicalPower(route) ? MachinePowerMath.throughputMultiplier(rpm, tierRpm) : 0.0D;
    }

    public static boolean consumeForProcessing(TileEntityMekanism tile) {
        return hasMechanicalPower(powerRoute(tile));
    }

    @Nullable
    public static DirectSource powerRoute(TileEntityMekanism tile) {
        Level level = tile.getLevel();
        if (level == null || level.isClientSide) {
            return null;
        }
        if (MachinePowerRouteScope.contains(tile)) {
            return MachinePowerRouteScope.current(tile);
        }

        DirectSource best = null;
        Map<BlockPos, List<Direction>> candidates = MachineFootprint.kineticCandidates(
            tile.getBlockPos(), boundingPositions(tile));
        for (Map.Entry<BlockPos, List<Direction>> entry : candidates.entrySet()) {
            // Route discovery must never synchronously load a neighboring chunk. Besides
            // causing surprise chunk work during normal ticks, doing so during serialization
            // can recurse into ChunkSerializer through Mekanism's energy container hooks.
            if (!level.hasChunkAt(entry.getKey())) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
            if (!(blockEntity instanceof KineticBlockEntity kinetic) || !facesMachine(level, kinetic, entry.getValue())) {
                continue;
            }

            KineticNetwork network = kinetic.getOrCreateNetwork();
            if (network == null) {
                continue;
            }

            float networkRpm = effectiveNetworkRpm(kinetic);
            boolean running = networkRpm > 0.0F
                && !kinetic.isOverStressed()
                && (kinetic.hasSource() || kinetic.isSource());
            float availableRpm = running ? networkRpm : 0.0F;
            DirectSource candidate = new DirectSource(kinetic, network, networkRpm, availableRpm, running);
            if (isBetterRoute(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    public static void updateDirectStress(TileEntityMekanism tile, @Nullable DirectSource route) {
        Level level = tile.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        boolean assign = route != null && PowerRouteSafety.shouldAssignStress(
            true, route.running(), route.rpm(), routeStillPresent(route));
        if (assign) {
            MachineKineticRegistry.sync(tile, route.network(), memberRpm(route));
        } else {
            MachineKineticRegistry.detach(tile);
        }
    }

    private static float memberRpm(DirectSource route) {
        return route.networkRpm();
    }

    public static void clearForRemoval(TileEntityMekanism tile) {
        MachineKineticRegistry.detach(tile);
    }

    @Nullable
    public static GearboxDemand inspectGearbox(MEGearboxBlockEntity gearbox) {
        if (gearbox.getLevel() == null || gearbox.getLevel().isClientSide
            || gearbox.getMode() != MEGearboxBlockEntity.Mode.EXPORT) {
            return null;
        }
        int effectiveRpm = effectiveGearboxRpm(gearbox);
        if (effectiveRpm <= 0) {
            return new GearboxDemand(0, 0L, 0.0D);
        }
        double fullSpeedLoad = MachinePowerMath.gearboxLoad(downstreamLoad(gearbox), effectiveRpm);
        return new GearboxDemand(effectiveRpm, (long) Math.ceil(fullSpeedLoad), fullSpeedLoad);
    }

    public static int effectiveGearboxRpm(MEGearboxBlockEntity gearbox) {
        int configuredRpm = Math.abs(gearbox.getConfiguredSpeed());
        if (configuredRpm <= 0) {
            return 0;
        }
        float limitedRpm = RpmLimitManager.clamp(gearbox, configuredRpm);
        return Math.round(MachinePowerMath.directRpm(limitedRpm, MekStressConfig.directMaxRpm()));
    }

    private static float effectiveNetworkRpm(KineticBlockEntity kinetic) {
        float limitedRpm = RpmLimitManager.clamp(kinetic, kinetic.getTheoreticalSpeed());
        return MachinePowerMath.directRpm(limitedRpm, MekStressConfig.directMaxRpm());
    }

    private static List<BlockPos> boundingPositions(TileEntityMekanism tile) {
        AttributeHasBounding bounding = Attribute.get(tile.getBlockState(), AttributeHasBounding.class);
        return bounding == null
            ? List.of()
            : bounding.getPositions(tile.getBlockPos(), tile.getBlockState()).toList();
    }

    private static boolean facesMachine(Level level, KineticBlockEntity kinetic, List<Direction> machineToKinetic) {
        for (Direction direction : machineToKinetic) {
            if (shaftFacesMachine(level, kinetic, direction)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBetterRoute(@Nullable DirectSource candidate, @Nullable DirectSource current) {
        if (current == null) {
            return true;
        }
        if (candidate.rpm() != current.rpm()) {
            return candidate.rpm() > current.rpm();
        }
        return candidate.networkRpm() > current.networkRpm();
    }

    private static double downstreamLoad(MEGearboxBlockEntity gearbox) {
        if (!gearbox.hasNetwork()) {
            return 0.0D;
        }
        KineticNetwork network = gearbox.getOrCreateNetwork();
        if (network == null) {
            return 0.0D;
        }
        double load = 0.0D;
        for (KineticBlockEntity member : network.members.keySet()) {
            load += Math.max(0.0D, network.getActualStressOf(member));
        }
        return load;
    }

    private static boolean shaftFacesMachine(Level level, KineticBlockEntity kinetic, Direction machineToKinetic) {
        if (!(kinetic.getBlockState().getBlock() instanceof IRotate rotate)) {
            return false;
        }
        Direction kineticFace = machineToKinetic.getOpposite();
        return rotate.hasShaftTowards(level, kinetic.getBlockPos(), kinetic.getBlockState(), kineticFace);
    }

    private static boolean routeStillPresent(DirectSource route) {
        KineticBlockEntity source = route.source();
        Level level = source.getLevel();
        if (level == null || level.isClientSide || source.isRemoved() || !source.hasNetwork()) {
            return false;
        }
        return level.getBlockEntity(source.getBlockPos()) == source
            && source.getOrCreateNetwork() == route.network();
    }

    public record DirectSource(
        KineticBlockEntity source,
        KineticNetwork network,
        float networkRpm,
        float rpm,
        boolean running
    ) {
    }

    public record GearboxDemand(int requestedRpm, long requestedSuPerTick, double fullSpeedLoad) {
    }
}
