package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.content.kinetics.press.PressingBehaviour.PressingBehaviourSpecifics;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/**
 * Turns "this machine spins far faster than it can use" into extra work per game tick.
 *
 * <p>Extra batches are applied only at the points where a machine would normally finish one
 * operation. The machine's full {@code tick()} is never re-run, so running state and render
 * animations advance exactly once per game tick. Visual speed is capped separately at each
 * machine's native saturation point.
 *
 * <p>The multiplier uses the player-limited effective speed but reads the raw theoretical value
 * directly, so an overstressed network does not drop to 0 RPM and make its own stress oscillate.
 */
public final class OverspeedBonus {

    /** Saturation points read from Create 6.0.8's own speed maths. */
    public static final double SATURATION_MIXER = 512;
    public static final double SATURATION_PRESS = 512;
    public static final double SATURATION_SAW = 3072;
    public static final double SATURATION_FAN = 256;
    /** {@code MillstoneBlockEntity#getProcessingSpeed()} clamps at 512 progress per tick. */
    public static final double SATURATION_MILLSTONE = 8192;

    private static final ThreadLocal<Boolean> BASIN_REENTRY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> MILLSTONE_REENTRY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> FAN_REENTRY = ThreadLocal.withInitial(() -> false);

    private OverspeedBonus() {}

    public static void reloadCurve() {
        OverspeedCurve.reload(
            RpmLimitConfig.OVERSPEED_STEP_TABLE.get(),
            message -> DimensionWorksRpmLimit.LOGGER.warn(message)
        );
    }

    public static double cap() {
        return Math.max(1, RpmLimitConfig.OVERSPEED_CAP.get());
    }

    /** The machine's own saturation point, or the configured fallback for whitelisted machines. */
    public static double saturationOf(KineticBlockEntity be) {
        if (be instanceof MechanicalMixerBlockEntity)
            return SATURATION_MIXER;
        if (be instanceof MechanicalPressBlockEntity)
            return SATURATION_PRESS;
        if (be instanceof SawBlockEntity)
            return SATURATION_SAW;
        if (be instanceof MillstoneBlockEntity)
            return SATURATION_MILLSTONE;
        if (be instanceof EncasedFanBlockEntity)
            return SATURATION_FAN;
        return Math.max(1, RpmLimitConfig.OVERSPEED_DEFAULT_SATURATION.get());
    }

    /**
     * Speed used exclusively by renderers. Work logic continues to read the real network speed;
     * animation stops scaling once this machine reaches the speed where its native processing
     * already saturates.
     */
    public static float animationSpeed(KineticBlockEntity be) {
        if (be.isOverStressed())
            return 0;

        float speed = RpmLimitManager.clamp(be, be.getTheoreticalSpeed());
        if (!RpmLimitConfig.OVERSPEED_ENABLED.get() || speed == 0)
            return speed;

        double cap = saturationOf(be);
        if (cap <= 0 || Math.abs(speed) <= cap)
            return speed;
        return (float) Math.copySign(cap, speed);
    }

    /**
     * Machines whose processing lives in their own {@code tick()}. Fans are excluded here because
     * their work happens inside {@code AirCurrent}, which the airflow mixin covers instead.
     */
    public static boolean isKineticBatchMachine(KineticBlockEntity be) {
        if (be instanceof EncasedFanBlockEntity)
            return false;
        return be instanceof MechanicalMixerBlockEntity
            || be instanceof MechanicalPressBlockEntity
            || be instanceof SawBlockEntity
            || be instanceof MillstoneBlockEntity;
    }

    public static boolean isWhitelisted(KineticBlockEntity be) {
        List<? extends String> whitelist = RpmLimitConfig.OVERSPEED_CLASS_WHITELIST.get();
        if (whitelist.isEmpty())
            return false;
        String className = be.getClass().getName();
        String simpleName = be.getClass().getSimpleName();
        for (String entry : whitelist) {
            if (entry == null || entry.isBlank())
                continue;
            String trimmed = entry.trim();
            if (trimmed.equals(className) || trimmed.equals(simpleName))
                return true;
        }
        return false;
    }

    private static boolean isCovered(KineticBlockEntity be) {
        return isKineticBatchMachine(be) || isWhitelisted(be);
    }

    /**
     * The batch multiplier for a machine, as a pure function of its effective player-limited speed.
     *
     * <p>Deterministic on both sides of the game so the goggles can print the same number the
     * server is acting on without any extra packet.
     */
    public static double multiplierOf(KineticBlockEntity be) {
        return OverspeedCurve.multiplier(
            RpmLimitManager.clamp(be, be.getTheoreticalSpeed()), saturationOf(be), cap());
    }

    /** Extra processing passes for this tick, without advancing animation state machines. */
    public static int extraBatches(KineticBlockEntity be) {
        if (!RpmLimitConfig.OVERSPEED_ENABLED.get())
            return 0;
        if (!isCovered(be))
            return 0;

        Level level = be.getLevel();
        if (level == null || level.isClientSide || be.isRemoved() || be.isOverStressed())
            return 0;

        double multiplier = multiplierOf(be);
        if (multiplier <= 1)
            return 0;
        return Math.max(0, OverspeedCurve.batches(multiplier, level.getGameTime()) - 1);
    }

    public static void repeatBasin(BasinOperatingBlockEntity be, Runnable operation) {
        repeat(be, operation, BASIN_REENTRY);
    }

    public static void repeatMillstone(MillstoneBlockEntity be, Runnable operation) {
        repeat(be, operation, MILLSTONE_REENTRY);
    }

    /**
     * Processes a world item stack several times during one press animation.
     *
     * <p>Create's non-bulk world press loop breaks after one call. A depot can hold a whole
     * stack in a single {@link ItemEntity}, so repeating the outer loop does not reliably bind
     * the extra batches to that stack. Redirecting the call lets each extra batch consume the
     * same stack while the animation remains a single cycle.
     */
    public static boolean tryProcessWorldBatched(PressingBehaviour behaviour,
                                                PressingBehaviourSpecifics specifics,
                                                ItemEntity itemEntity,
                                                boolean simulate) {
        if (simulate || specifics.canProcessInBulk())
            return specifics.tryProcessInWorld(itemEntity, simulate);
        if (!(behaviour.blockEntity instanceof KineticBlockEntity kinetic))
            return specifics.tryProcessInWorld(itemEntity, false);

        int batches = extraBatches(kinetic) + 1;
        boolean processed = false;
        for (int i = 0; i < batches && itemEntity.isAlive(); i++) {
            if (!specifics.tryProcessInWorld(itemEntity, false))
                break;
            processed = true;
        }
        return processed;
    }

    /**
     * Processes a depot or belt stack several times during one press animation.
     *
     * <p>The vanilla callback only calls {@code tryProcessOnBelt} once and then removes one
     * item from the stack. The extra calls are aggregated into the same result list, and the
     * remaining stack is reduced by the number of successful calls minus the one removal the
     * original callback performs afterwards.
     */
    public static boolean tryProcessOnBeltBatched(PressingBehaviourSpecifics specifics,
                                                  TransportedItemStack transported,
                                                  List<ItemStack> outputList,
                                                  boolean simulate) {
        if (simulate || specifics.canProcessInBulk() || !(specifics instanceof KineticBlockEntity kinetic))
            return specifics.tryProcessOnBelt(transported, outputList, simulate);

        int batches = Math.min(transported.stack.getCount(), extraBatches(kinetic) + 1);
        int processed = 0;
        for (int i = 0; i < batches; i++) {
            if (!specifics.tryProcessOnBelt(transported, outputList, false))
                break;
            processed++;
        }

        if (processed <= 1)
            return processed == 1;
        transported.stack.shrink(processed - 1);
        return true;
    }

    private static void repeat(KineticBlockEntity be, Runnable operation, ThreadLocal<Boolean> reentry) {
        if (reentry.get())
            return;
        int extra = extraBatches(be);
        if (extra <= 0)
            return;

        reentry.set(true);
        try {
            for (int i = 0; i < extra && !be.isRemoved(); i++)
                operation.run();
        } finally {
            reentry.set(false);
        }
    }

    /** Stress scales with the curve value so over-speeding still needs real generator capacity. */
    public static float stressMultiplier(KineticBlockEntity be) {
        if (!RpmLimitConfig.OVERSPEED_ENABLED.get())
            return 1;
        if (!RpmLimitConfig.OVERSPEED_STRESS_SCALING.get())
            return 1;
        if (!isCovered(be))
            return 1;
        return (float) multiplierOf(be);
    }

    /** Extra {@code AirCurrent} item-processing passes for a fan past its saturation speed. */
    public static int fanExtraPasses(IAirCurrentSource source) {
        if (!(source instanceof EncasedFanBlockEntity fan))
            return 0;
        return extraBatches(fan);
    }

    /**
     * Re-runs the airflow's item handlers for an over-spun fan.

     * <p>Only {@code tickAffectedHandlers} is repeated. {@code tickAffectedEntities} is deliberately
     * left alone, because that is the code that pushes players and mobs around - repeating it would
     * multiply the knockback instead of the processing speed.
     */
    public static void fanExtraHandlerPasses(AirCurrent current) {
        if (FAN_REENTRY.get())
            return;
        int extra = fanExtraPasses(current.source);
        if (extra <= 0)
            return;

        FAN_REENTRY.set(true);
        try {
            for (int i = 0; i < extra; i++)
                current.tickAffectedHandlers();
        } finally {
            FAN_REENTRY.set(false);
        }
    }

    public static String formatMultiplier(double multiplier) {
        if (Math.abs(multiplier - Math.rint(multiplier)) < 1.0E-4)
            return Long.toString(Math.round(multiplier));
        return String.format(Locale.ROOT, "%.1f", multiplier);
    }
}
