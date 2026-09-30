package dev.szx.dimensionworks.mekstress.memory;

import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.api.IMEGearboxExportState;
import dev.szx.dimensionworks.mekstress.core.MachinePowerMath;
import dev.szx.dimensionworks.mekstress.core.MachineTier;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Routes eligible Mekanism machines through one adjacent Create kinetic source. */
public final class MachinePowerManager {
    private static final Map<TileEntityMekanism, DirectSource> ROUTE_CACHE =
        Collections.synchronizedMap(new WeakHashMap<>());

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
        return currentRpm(tile) > 0;
    }

    public static double workRate(TileEntityMekanism tile) {
        return workRate(tile, powerRoute(tile));
    }

    public static double workRate(TileEntityMekanism tile, @Nullable DirectSource route) {
        int rpm = currentRpm(route);
        int tierRpm = MekStressConfig.machineRpm(tier(tile));
        return rpm <= 0 || tierRpm <= 0 ? 0.0D : (double) rpm / tierRpm;
    }

    public static boolean consumeForProcessing(TileEntityMekanism tile) {
        DirectSource route = powerRoute(tile);
        return route != null && currentRpm(route) > 0 && !route.source().isOverStressed();
    }

    @Nullable
    public static DirectSource powerRoute(TileEntityMekanism tile) {
        Level level = tile.getLevel();
        if (level == null || level.isClientSide) {
            return null;
        }

        DirectSource best = null;
        float bestRpm = -1.0F;
        for (Direction direction : Direction.values()) {
            BlockPos pos = tile.getBlockPos().relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof KineticBlockEntity kinetic)) {
                continue;
            }
            MEGearboxBlockEntity gearbox = findExportGearbox(kinetic);
            if (gearbox == null) {
                continue;
            }

            boolean running = isExportRunning(gearbox);
            float rpm = running ? effectiveKineticRpm(kinetic, gearbox) : 0.0F;
            if (best == null || rpm > bestRpm) {
                best = new DirectSource(kinetic, gearbox, rpm, running && rpm > 0.0F);
                bestRpm = rpm;
            }
        }
        if (best != null) {
            ROUTE_CACHE.put(tile, best);
            return best;
        }

        DirectSource cached = ROUTE_CACHE.get(tile);
        if (cached != null && routeStillPresent(cached)) {
            DirectSource stalled = new DirectSource(cached.source(), cached.gearbox(), 0.0F, false);
            ROUTE_CACHE.put(tile, stalled);
            return stalled;
        }
        ROUTE_CACHE.remove(tile);
        return null;
    }

    public static void updateDirectStress(TileEntityMekanism tile, @Nullable DirectSource route) {
        Level level = tile.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        GlobalPos machine = GlobalPos.of(level.dimension(), tile.getBlockPos());
        DirectStressRegistry.Change change = route == null
            ? DirectStressRegistry.clear(machine)
            : DirectStressRegistry.assign(machine, route.source(), MachinePowerMath.stressPerRpm(1.0F));
        if (!change.changed()) {
            return;
        }
        refreshStress(change.previous());
        if (change.current() != change.previous()) {
            refreshStress(change.current());
        }
    }

    public static void clearForRemoval(TileEntityMekanism tile) {
        ROUTE_CACHE.remove(tile);
        Level level = tile.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        updateDirectStress(tile, null);
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
        double fullSpeedLoad = fullSpeedLoad(gearbox, effectiveRpm);
        if (fullSpeedLoad <= 0.0D) {
            return new GearboxDemand(0, 0L, 0.0D);
        }
        return new GearboxDemand(effectiveRpm, (long) Math.ceil(fullSpeedLoad), fullSpeedLoad);
    }

    public static int effectiveGearboxRpm(MEGearboxBlockEntity gearbox) {
        int configuredRpm = Math.abs(gearbox.getConfiguredSpeed());
        if (configuredRpm <= 0) {
            return 0;
        }
        float limited = Math.abs(RpmLimitManager.clamp(gearbox, configuredRpm));
        return Math.min(Math.max(0, Math.round(limited)), MekStressConfig.directMaxRpm());
    }

    private static boolean isExportRunning(MEGearboxBlockEntity gearbox) {
        return (Object) gearbox instanceof IMEGearboxExportState state
            && state.dimensionworks$isExportRunning()
            && state.dimensionworks$getExportChargedSu() > 0L;
    }

    private static float effectiveKineticRpm(KineticBlockEntity kinetic, MEGearboxBlockEntity gearbox) {
        if (kinetic.isOverStressed()) {
            return 0.0F;
        }
        float propagated = Math.abs(RpmLimitManager.clamp(kinetic, kinetic.getTheoreticalSpeed()));
        int outputRpm = (Object) gearbox instanceof IMEGearboxExportState state
            ? Math.max(0, state.dimensionworks$getExportOutputRpm())
            : 0;
        return Math.min(Math.min(propagated, outputRpm), MekStressConfig.directMaxRpm());
    }

    @Nullable
    private static MEGearboxBlockEntity findExportGearbox(KineticBlockEntity kinetic) {
        if (kinetic instanceof MEGearboxBlockEntity gearbox
            && gearbox.getMode() == MEGearboxBlockEntity.Mode.EXPORT) {
            return gearbox;
        }
        if (!kinetic.hasNetwork()) {
            return null;
        }
        KineticNetwork network = kinetic.getOrCreateNetwork();
        for (KineticBlockEntity source : network.sources.keySet()) {
            if (source instanceof MEGearboxBlockEntity gearbox
                && gearbox.getMode() == MEGearboxBlockEntity.Mode.EXPORT) {
                return gearbox;
            }
        }
        for (KineticBlockEntity member : network.members.keySet()) {
            if (member instanceof MEGearboxBlockEntity gearbox
                && gearbox.getMode() == MEGearboxBlockEntity.Mode.EXPORT) {
                return gearbox;
            }
        }
        return null;
    }

    private static double fullSpeedLoad(MEGearboxBlockEntity gearbox, int effectiveRpm) {
        double load = 0.0D;
        Set<KineticNetwork> networks = new HashSet<>();
        Set<KineticBlockEntity> representedMembers = new HashSet<>();
        if (gearbox.hasNetwork()) {
            networks.add(gearbox.getOrCreateNetwork());
        }
        collectCachedNetworks(gearbox, networks);
        for (KineticNetwork network : networks) {
            for (var entry : network.members.entrySet()) {
                KineticBlockEntity member = entry.getKey();
                representedMembers.add(member);
                load += Math.max(0.0F, entry.getValue()) * effectiveMemberRpm(member, effectiveRpm);
            }
        }
        return load + cachedMachineLoad(gearbox, effectiveRpm, representedMembers);
    }

    private static void collectCachedNetworks(MEGearboxBlockEntity gearbox, Set<KineticNetwork> networks) {
        synchronized (ROUTE_CACHE) {
            for (DirectSource route : ROUTE_CACHE.values()) {
                if (route.gearbox() != gearbox || !route.source().hasNetwork()) {
                    continue;
                }
                networks.add(route.source().getOrCreateNetwork());
            }
        }
    }

    private static double cachedMachineLoad(MEGearboxBlockEntity gearbox, int effectiveRpm,
                                           Set<KineticBlockEntity> representedMembers) {
        List<TileEntityMekanism> stale = new ArrayList<>();
        double load = 0.0D;
        synchronized (ROUTE_CACHE) {
            for (var entry : ROUTE_CACHE.entrySet()) {
                DirectSource route = entry.getValue();
                if (route.gearbox() != gearbox) {
                    continue;
                }
                if (representedMembers.contains(route.source())) {
                    continue;
                }
                if (!routeStillPresent(route)) {
                    stale.add(entry.getKey());
                    continue;
                }
                float memberRpm = effectiveMemberRpm(route.source(), effectiveRpm);
                load += MachinePowerMath.stressPerRpm(1.0F) * memberRpm;
            }
            for (TileEntityMekanism machine : stale) {
                ROUTE_CACHE.remove(machine);
            }
        }
        return load;
    }

    private static float effectiveMemberRpm(KineticBlockEntity member, int networkRpm) {
        if (networkRpm <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.abs(RpmLimitManager.clamp(member, networkRpm)));
    }

    private static boolean routeStillPresent(DirectSource route) {
        KineticBlockEntity source = route.source();
        MEGearboxBlockEntity gearbox = route.gearbox();
        Level level = source.getLevel();
        if (level == null || level.isClientSide || source.isRemoved() || gearbox.isRemoved()
            || gearbox.getLevel() != level || gearbox.getMode() != MEGearboxBlockEntity.Mode.EXPORT) {
            return false;
        }
        return level.getBlockEntity(source.getBlockPos()) == source
            && level.getBlockEntity(gearbox.getBlockPos()) == gearbox;
    }

    public record DirectSource(
        KineticBlockEntity source,
        MEGearboxBlockEntity gearbox,
        float rpm,
        boolean running
    ) {
    }

    public record GearboxDemand(int requestedRpm, long requestedSuPerTick, double fullSpeedLoad) {
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
