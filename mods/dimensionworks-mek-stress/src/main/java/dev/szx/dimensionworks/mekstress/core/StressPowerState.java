package dev.szx.dimensionworks.mekstress.core;

import appeng.api.config.Actionable;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.StressFormula;
import dev.szx.dimensionworks.mekstress.api.StressPoweredMachine;
import dev.szx.dimensionworks.mekstress.api.StressSupplySource;
import dev.szx.dimensionworks.rpmlimit.OverspeedCurve;
import dev.szx.dimensionworks.rpmlimit.RpmLimitConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

public final class StressPowerState {

    private static final long SOURCE_TIMEOUT_TICKS = 2L;

    private final Map<Object, StressSupplySource> sources = new HashMap<>();
    private final StressPoweredMachine machine;
    private final int requiredRpm;
    private final IntSupplier overspeedCap;
    private final DoubleSupplier joulesPerSu;

    public StressPowerState(StressPoweredMachine machine) {
        this(machine, () -> RpmLimitConfig.OVERSPEED_CAP.get(), MekStressConfig::joulesPerSu);
    }

    StressPowerState(StressPoweredMachine machine, int overspeedCap) {
        this(machine, () -> overspeedCap, MekStressConfig::joulesPerSu);
    }

    StressPowerState(StressPoweredMachine machine, double joulesPerSu) {
        this(machine, () -> RpmLimitConfig.OVERSPEED_CAP.get(), () -> joulesPerSu);
    }

    private StressPowerState(StressPoweredMachine machine, IntSupplier overspeedCap, DoubleSupplier joulesPerSu) {
        this.machine = machine;
        this.requiredRpm = Math.max(0, machine.dimensionworks$requiredRpm());
        this.overspeedCap = overspeedCap;
        this.joulesPerSu = joulesPerSu;
    }

    public void register(StressSupplySource source, long gameTick) {
        sources.put(source.key(), source);
        prune(gameTick);
    }

    public void prune(long gameTick) {
        sources.values().removeIf(source -> gameTick - source.lastSeenTick() > SOURCE_TIMEOUT_TICKS);
    }

    public long availableStress(long requested, long gameTick) {
        prune(gameTick);
        long total = 0L;
        for (StressSupplySource source : orderedSources()) {
            if (!qualifies(source)) {
                continue;
            }
            if (total >= requested) {
                break;
            }
            long remaining = requested - total;
            total += Math.min(remaining, source.availableStress(remaining, Actionable.SIMULATE, gameTick));
        }
        return total;
    }

    public long consumeStress(long requested, Actionable mode, long gameTick) {
        prune(gameTick);
        long total = 0L;
        for (StressSupplySource source : orderedSources()) {
            if (!qualifies(source)) {
                continue;
            }
            if (total >= requested) {
                break;
            }
            long remaining = requested - total;
            total += Math.min(remaining, source.consumeStress(remaining, mode, gameTick));
        }
        return total;
    }

    public double speedMultiplier(long gameTick) {
        prune(gameTick);
        int rpm = 0;
        for (StressSupplySource source : sources.values()) {
            if (!qualifies(source)) {
                continue;
            }
            int sourceRpm = source.rpm();
            if (sourceRpm > 0 && (rpm == 0 || sourceRpm < rpm)) {
                rpm = sourceRpm;
            }
        }
        if (rpm <= 0) {
            return 0.0D;
        }
        return StressFormula.multiplier(rpm, requiredRpm, overspeedCap.getAsInt());
    }

    public int batches(long gameTick) {
        return OverspeedCurve.batches(speedMultiplier(gameTick), gameTick);
    }

    public boolean hasQualifyingSource(long gameTick) {
        prune(gameTick);
        for (StressSupplySource source : sources.values()) {
            if (qualifies(source)) {
                return true;
            }
        }
        return false;
    }

    /** Fills the machine's real energy buffer from qualifying stress sources. */
    public double tryFillBuffer(long gameTick) {
        StressEnergyBuffer buffer = machine.dimensionworks$stressEnergyBuffer();
        if (buffer == null) {
            return 0.0D;
        }
        double joulesPerSu = this.joulesPerSu.getAsDouble();
        double neededJoules = buffer.dimensionworks$capacityJoules() - buffer.dimensionworks$storedJoules();
        long neededStress = StressFormula.joulesToStressFloor(neededJoules, joulesPerSu);
        if (neededStress <= 0L) {
            return 0.0D;
        }

        long availableStress = availableStress(neededStress, gameTick);
        if (availableStress <= 0L) {
            return 0.0D;
        }
        double requestedJoules = StressFormula.stressToJoules(availableStress, joulesPerSu);
        double acceptedJoules = buffer.dimensionworks$insertJoulesDirect(requestedJoules, Actionable.SIMULATE);
        long acceptedStress = StressFormula.joulesToStressFloor(acceptedJoules, joulesPerSu);
        if (acceptedStress <= 0L) {
            return 0.0D;
        }

        long extractedStress = consumeStress(acceptedStress, Actionable.MODULATE, gameTick);
        if (extractedStress <= 0L) {
            return 0.0D;
        }
        return buffer.dimensionworks$insertJoulesDirect(
            StressFormula.stressToJoules(extractedStress, joulesPerSu),
            Actionable.MODULATE
        );
    }

    public boolean hasSources(long gameTick) {
        prune(gameTick);
        return !sources.isEmpty();
    }

    private boolean qualifies(StressSupplySource source) {
        return source.rpm() >= requiredRpm;
    }

    private List<StressSupplySource> orderedSources() {
        List<StressSupplySource> result = new ArrayList<>(sources.values());
        result.sort(Comparator.comparing(StressSupplySource::key, StressPowerState::compareKeys));
        return result;
    }

    private static int compareKeys(Object a, Object b) {
        if (a instanceof StressSupplyKey first && b instanceof StressSupplyKey second) {
            return first.compareTo(second);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }
}
