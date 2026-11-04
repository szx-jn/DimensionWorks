package dev.szx.dimensionworks.mekstress.reactor;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorComponent;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorInjector;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorStabilizer;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore.ReactorState;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Create's SU is a stress capacity, not an accumulating energy buffer.
 *
 * One reactor produces at most generationRate FE/t * 2 SU at 256 RPM, shared by
 * every equipped stabilizer port. The reactor's saturation is still an internal
 * operating variable; only its *generated* energy is discarded in place of FE export.
 */
public final class ReactorPowerBridge {
    public static final float OUTPUT_RPM = ReactorPowerMath.OUTPUT_RPM;
    private static final double SHIELD_TARGET = 0.95D;
    /** Successful bindings only; every lookup revalidates the live blocks and reactor state. */
    private static final Map<KineticBlockEntity, OutputBinding> OUTPUT_BINDINGS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private ReactorPowerBridge() {
    }

    public record Output(TileReactorCore core, int connectedPorts, double generationPerTick) {
        public float stressPerRpm() {
            return ReactorPowerMath.stressPerRpm(generationPerTick, connectedPorts);
        }
    }

    private record OutputBinding(BlockPos shaftPos, Direction side,
                                 WeakReference<TileReactorStabilizer> stabilizer, BlockPos stabilizerPos,
                                 WeakReference<TileReactorCore> core, Direction coreSide) {
    }

    @Nullable
    public static Output outputFor(KineticBlockEntity kinetic) {
        if (kinetic == null || !isShaft(kinetic)) {
            return null;
        }
        float speed = kinetic.getTheoreticalSpeed();
        if (speed != 0.0F && speed != OUTPUT_RPM) {
            return null;
        }
        Level level = kinetic.getLevel();
        BlockPos shaftPos = kinetic.getBlockPos();
        if (level == null || !level.hasChunkAt(shaftPos)) {
            return null;
        }
        if (kinetic.isRemoved() || level.getBlockEntity(shaftPos) != kinetic) {
            OUTPUT_BINDINGS.remove(kinetic);
            return null;
        }
        OutputBinding cached = OUTPUT_BINDINGS.get(kinetic);
        if (cached != null) {
            Output output = resolveCachedOutput(kinetic, level, shaftPos, cached);
            if (output != null) {
                return output;
            }
            OUTPUT_BINDINGS.remove(kinetic);
        }
        // Only the four stabilizers can export energy; injectors are consumers.
        for (Direction towardsStabilizer : Direction.values()) {
            BlockPos componentPos = shaftPos.relative(towardsStabilizer);
            if (!level.hasChunkAt(componentPos)) {
                continue;
            }
            BlockEntity candidate = level.getBlockEntity(componentPos);
            if (!(candidate instanceof TileReactorStabilizer stabilizer)
                || stabilizer.facing.get() != towardsStabilizer) {
                continue;
            }
            if (kinetic.getBlockState().getValue(ShaftBlock.AXIS) != towardsStabilizer.getAxis()) {
                continue;
            }
            TileReactorCore core = boundCore(stabilizer);
            if (core == null || !core.structureValid.get()
                || core.reactorState.get() != ReactorState.RUNNING) {
                continue;
            }
            Direction coreSide = towardsStabilizer.getOpposite();
            OutputBinding binding = new OutputBinding(shaftPos.immutable(), towardsStabilizer,
                new WeakReference<>(stabilizer), stabilizer.getBlockPos().immutable(),
                new WeakReference<>(core), coreSide);
            Output output = createOutput(core);
            if (output != null) {
                OUTPUT_BINDINGS.put(kinetic, binding);
                return output;
            }
        }
        return null;
    }

    @Nullable
    private static Output resolveCachedOutput(KineticBlockEntity kinetic, Level level, BlockPos shaftPos,
                                               OutputBinding binding) {
        TileReactorStabilizer stabilizer = binding.stabilizer().get();
        TileReactorCore core = binding.core().get();
        if (stabilizer == null || core == null || !binding.shaftPos().equals(shaftPos)
            || kinetic.getBlockState().getValue(ShaftBlock.AXIS) != binding.side().getAxis()) {
            return null;
        }
        Level stabilizerLevel = stabilizer.getLevel();
        if (stabilizerLevel != level || stabilizer.isRemoved()
            || !level.hasChunkAt(binding.stabilizerPos())
            || level.getBlockEntity(binding.stabilizerPos()) != stabilizer
            || stabilizer.facing.get() != binding.side()
            || !stabilizer.isBound.get()) {
            return null;
        }
        BlockPos corePos = core.getBlockPos();
        if (core.getLevel() != level || core.isRemoved() || !level.hasChunkAt(corePos)
            || level.getBlockEntity(corePos) != core || !core.structureValid.get()
            || core.reactorState.get() != ReactorState.RUNNING
            || boundComponent(core, binding.coreSide()) != stabilizer) {
            return null;
        }
        return createOutput(core);
    }

    @Nullable
    private static Output createOutput(TileReactorCore core) {
        int ports = outputShafts(core).size();
        if (ports == 0) {
            return null;
        }
        double generated = core.generationRate.get();
        if (!Double.isFinite(generated) || generated < 0.0D) {
            generated = 0.0D;
        }
        return new Output(core, ports, generated);
    }

    public static boolean isOutputOf(KineticBlockEntity kinetic, TileReactorCore expectedCore) {
        Output output = outputFor(kinetic);
        return output != null && output.core() == expectedCore;
    }

    private static boolean isShaft(KineticBlockEntity kinetic) {
        return ShaftBlock.isShaft(kinetic.getBlockState());
    }

    // Resolve only loaded positions: a Create shaft must never load the core's chunk.
    @Nullable
    private static TileReactorCore boundCore(TileReactorStabilizer stabilizer) {
        Level level = stabilizer.getLevel();
        if (level == null || !stabilizer.isBound.get()) {
            return null;
        }
        Direction towardCore = stabilizer.facing.get();
        for (int distance = 4; distance < TileReactorCore.COMPONENT_MAX_DISTANCE; distance++) {
            BlockPos pos = stabilizer.getBlockPos().relative(towardCore, distance);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(pos);
            if (!(neighbor instanceof TileReactorCore core)) {
                continue;
            }
            Direction side = towardCore.getOpposite();
            BlockPos linked = core.getBlockPos()
                .subtract(core.componentPositions[side.get3DDataValue()].get().getPos());
            return linked.equals(stabilizer.getBlockPos()) ? core : null;
        }
        return null;
    }

    @Nullable
    private static TileReactorComponent boundComponent(TileReactorCore core, Direction side) {
        Level level = core.getLevel();
        if (level == null) {
            return null;
        }
        BlockPos pos = core.getBlockPos()
            .subtract(core.componentPositions[side.get3DDataValue()].get().getPos());
        if (pos.equals(core.getBlockPos()) || !level.hasChunkAt(pos)) {
            return null;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof TileReactorComponent component && component.isBound.get()
            && component.facing.get() == side.getOpposite() ? component : null;
    }

    private static List<KineticBlockEntity> outputShafts(TileReactorCore core) {
        List<KineticBlockEntity> result = new ArrayList<>(4);
        Level level = core.getLevel();
        if (level == null) {
            return result;
        }
        for (Direction direction : Direction.values()) {
            TileReactorComponent component = boundComponent(core, direction);
            if (!(component instanceof TileReactorStabilizer stabilizer)) {
                continue;
            }
            BlockPos shaftPos = stabilizer.getBlockPos().relative(stabilizer.facing.get().getOpposite());
            if (!level.hasChunkAt(shaftPos)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(shaftPos);
            if (neighbor instanceof KineticBlockEntity shaft && isShaft(shaft)
                && shaft.getBlockState().getValue(ShaftBlock.AXIS) == stabilizer.facing.get().getAxis()
                && (shaft.getTheoreticalSpeed() == 0.0F || shaft.getTheoreticalSpeed() == OUTPUT_RPM)) {
                result.add(shaft);
            }
        }
        return result;
    }

    /**
     * Called after the original reactor tick, so temperature, fuel use and explosion
     * checks are never substituted. Only virtual SU input is allowed to recharge it.
     */
    public static void afterCoreTick(TileReactorCore core) {
        Level level = core.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        ReactorState state = core.reactorState.get();
        if (state == ReactorState.RUNNING && !outputShafts(core).isEmpty()) {
            // Equivalent to the original stabilizers extracting this tick's FE.
            // Without draining generated energy, saturation would rapidly become full,
            // and generationRate would fall to zero even with a Create shaft attached.
            long drain = ReactorPowerMath.drainGenerated(core.generationRate.get(),
                core.saturation.get());
            if (drain > 0) {
                core.saturation.subtract(drain);
            }
        }

        if (state != ReactorState.WARMING_UP && state != ReactorState.RUNNING
            && state != ReactorState.STOPPING) {
            ReactorInputRegistry.detach(core);
            return;
        }

        Input input = findPoweredInput(core);
        if (input == null) {
            ReactorInputRegistry.detach(core);
            return;
        }
        long availableFe = (long) Math.min(1_000_000_000D, Math.floor(ReactorInputRegistry.availableSu(core, input.network()) / 2.0D));
        long requestedFe = state == ReactorState.WARMING_UP ? warmingInput(core) : shieldInput(core);
        long suppliedFe = Math.min(availableFe, requestedFe);
        ReactorInputRegistry.sync(core, input.injector(), input.shaft(), input.network(), suppliedFe);
        // The virtual input is powered only if the real Create network has enough
        // stress capacity to carry this tick's real FE-equivalent input.
        if (input.shaft().isOverStressed() || input.shaft().getSpeed() == 0) {
            return;
        }

        if (suppliedFe > 0) core.injectEnergy(suppliedFe);
    }

    private static long shieldInput(TileReactorCore core) {
        double max = core.maxShieldCharge.get();
        double shield = core.shieldCharge.get();
        if (!Double.isFinite(max) || max <= 0 || !Double.isFinite(shield)) return 0;
        double deficit = Math.max(0, max * SHIELD_TARGET - shield);
        double temperatureEfficiency = Math.max(0, 1 - Math.max(0, core.temperature.get() - 15_000) / 10_000);
        double efficiency = Math.max(0, 1 - shield / max) * temperatureEfficiency;
        if (!Double.isFinite(efficiency) || efficiency <= 0) return 0;
        return (long) Math.min(1_000_000_000D, Math.ceil(deficit / efficiency));
    }

    private static long warmingInput(TileReactorCore core) {
        if (!core.startupInitialized.get()) return 0;
        double halfShield = core.maxShieldCharge.get() / 2;
        if (core.shieldCharge.get() < halfShield)
            return Math.max(0L, (long) Math.ceil(halfShield - core.shieldCharge.get()) + 1);
        long saturationDeficit = core.maxSaturation.get() / 2 - core.saturation.get();
        if (saturationDeficit > 0) return saturationDeficit;
        double heat = Math.max(0, 2000 - core.temperature.get()) * (1000 + core.reactableFuel.get() * 10);
        return Double.isFinite(heat) ? (long) Math.min(1_000_000_000D, Math.ceil(heat)) : 0;
    }

    private record Input(TileReactorInjector injector, KineticBlockEntity shaft, KineticNetwork network) {
    }

    @Nullable
    private static Input findPoweredInput(TileReactorCore core) {
        Level level = core.getLevel();
        if (level == null || !core.structureValid.get()) {
            return null;
        }
        for (Direction direction : Direction.values()) {
            TileReactorComponent component = boundComponent(core, direction);
            if (!(component instanceof TileReactorInjector injector)) {
                continue;
            }
            BlockPos shaftPos = injector.getBlockPos().relative(injector.facing.get().getOpposite());
            if (!level.hasChunkAt(shaftPos)) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(shaftPos);
            if (!(neighbor instanceof KineticBlockEntity shaft) || !isShaft(shaft)
                || shaft.getBlockState().getValue(ShaftBlock.AXIS) != injector.facing.get().getAxis()
                || shaft.getTheoreticalSpeed() == 0.0F) {
                continue;
            }
            KineticNetwork network = shaft.getOrCreateNetwork();
            if (network == null) {
                continue;
            }
            // Source and consumer may share a network: the virtual member pays
            // the full SU cost, so this is the intentional reactor self-sustain
            // loop, not free energy. WARMING_UP still requires an external source
            // because the reactor cannot generate until activated.
            boolean hasGenerator = false;
            for (KineticBlockEntity source : network.sources.keySet()) {
                if (source.getGeneratedSpeed() != 0
                    && source.calculateAddedStressCapacity() > 0.0F) {
                    hasGenerator = true;
                    break;
                }
            }
            if (hasGenerator) {
                return new Input(injector, shaft, network);
            }
        }
        return null;
    }
}
