package dev.szx.dimensionworks.mekstress.reactor;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorComponent;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorInjector;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorStabilizer;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore.ReactorState;
import java.util.ArrayList;
import java.util.List;
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
    public static final float INPUT_STRESS_PER_RPM = ReactorPowerMath.INPUT_STRESS_PER_RPM;
    private static final double SHIELD_TARGET = 0.95D;

    private ReactorPowerBridge() {
    }

    public record Output(TileReactorCore core, int connectedPorts, double generationPerTick) {
        public float stressPerRpm() {
            return ReactorPowerMath.stressPerRpm(generationPerTick, connectedPorts);
        }
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
            int ports = outputShafts(core).size();
            if (ports > 0) {
                double generated = core.generationRate.get();
                if (!Double.isFinite(generated) || generated < 0.0D) {
                    generated = 0.0D;
                }
                return new Output(core, ports, generated);
            }
        }
        return null;
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
        ReactorInputRegistry.sync(core, input.injector(), input.shaft(), input.network());
        // The virtual input is powered only if the real Create network has enough
        // stress capacity to carry the extra 256 SU/RPM consumer.
        if (input.shaft().isOverStressed() || input.shaft().getSpeed() == 0) {
            return;
        }

        if (state == ReactorState.WARMING_UP) {
            // The three original charge stages (field, saturation, heat) are retained.
            // There is no real FE inventory; Mek-style virtual energy supplies them.
            core.injectEnergy(1_000_000_000L);
        } else if (core.maxShieldCharge.get() > 0) {
            // Avoid exactly 100%: vanilla fieldInputRate divides by (1 - field%).
            // The virtual input keeps the field charged, but loss of mechanical
            // power leaves the original drain / failure behaviour untouched.
            double minimum = core.maxShieldCharge.get() * SHIELD_TARGET;
            if (core.shieldCharge.get() < minimum) {
                core.shieldCharge.set(minimum);
            }
        }
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
            // A reactor may never recharge its own injector from its output,
            // even if several of its stabilizers join one Create network.
            boolean feedback = false;
            boolean externalSource = false;
            for (KineticBlockEntity source : network.sources.keySet()) {
                if (isOutputOf(source, core)) {
                    feedback = true;
                    break;
                }
                if (source.getGeneratedSpeed() != 0 && source.calculateAddedStressCapacity() > 0) {
                    externalSource = true;
                }
            }
            if (!feedback && externalSource) {
                return new Input(injector, shaft, network);
            }
        }
        return null;
    }
}
