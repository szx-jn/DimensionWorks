package dev.szx.dimensionworks.mekstress.core;

import appeng.api.config.Actionable;
import dev.szx.dimensionworks.mekstress.api.StressPoweredMachine;
import dev.szx.dimensionworks.mekstress.api.StressSupplySource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StressPowerStateTest {

    @Test
    void rpmBelowMachineMinimumProvidesNoStressOrOverspeed() {
        FakeSource source = source(0, 100, 128);
        StressPowerState state = new StressPowerState(new FakeMachine(512));
        source.touch(10L);
        state.register(source, 10L);

        assertEquals(0L, state.availableStress(100L, 10L));
        assertEquals(0L, state.consumeStress(100L, Actionable.MODULATE, 10L));
        assertEquals(0.0D, state.speedMultiplier(10L));
        assertEquals(1, state.batches(10L));
        assertEquals(100L, source.remaining);
    }

    @Test
    void machineTierRpmIsTheOverspeedSaturationPoint() {
        FakeSource source = source(0, 100, 4096);
        StressPowerState state = new StressPowerState(new FakeMachine(2048), 15);
        source.touch(10L);
        state.register(source, 10L);

        assertEquals(2.0D, state.speedMultiplier(10L));
    }

    @Test
    void mergesSourcesInStablePositionOrderWithoutExceedingRequest() {
        FakeSource first = source(0, 100);
        FakeSource second = source(1, 50);
        StressPowerState state = new StressPowerState(new FakeMachine());

        first.touch(10L);
        second.touch(10L);
        state.register(first, 10L);
        state.register(second, 10L);

        assertEquals(120L, state.availableStress(120L, 10L));
        assertEquals(120L, state.consumeStress(120L, Actionable.MODULATE, 10L));
        assertEquals(0L, first.remaining);
        assertEquals(30L, second.remaining);
    }

    @Test
    void simulationNeverConsumesBudget() {
        FakeSource source = source(0, 50);
        StressPowerState state = new StressPowerState(new FakeMachine());
        source.touch(1L);
        state.register(source, 1L);

        assertEquals(50L, state.consumeStress(50L, Actionable.SIMULATE, 1L));
        assertEquals(50L, state.availableStress(50L, 1L));
        assertEquals(50L, source.remaining);
    }

    @Test
    void repeatedConsumptionInOneTickOnlyDebitsActualExtraction() {
        FakeSource source = source(0, 70);
        StressPowerState state = new StressPowerState(new FakeMachine());
        source.touch(5L);
        state.register(source, 5L);

        assertEquals(40L, state.consumeStress(40L, Actionable.MODULATE, 5L));
        assertEquals(30L, state.consumeStress(40L, Actionable.MODULATE, 5L));
        assertEquals(0L, state.consumeStress(1L, Actionable.MODULATE, 5L));
        assertEquals(2, source.consumeCalls);
    }

    @Test
    void fillBufferWritesStressIntoPersistentMachineStorage() {
        FakeSource source = source(0, 100);
        FakeMachine machine = new FakeMachine(128, 1_000.0D);
        StressPowerState state = new StressPowerState(machine, 2.5D);
        source.touch(10L);
        state.register(source, 10L);

        assertEquals(250.0D, state.tryFillBuffer(10L), 0.000_001D);
        assertEquals(250.0D, machine.buffer.stored, 0.000_001D);
        assertEquals(0L, source.remaining);
        assertEquals(0.0D, state.tryFillBuffer(10L), 0.000_001D);
        assertEquals(250.0D, machine.buffer.stored, 0.000_001D);
    }

    @Test
    void fillBufferNeverExceedsMachineCapacity() {
        FakeSource source = source(0, 100);
        FakeMachine machine = new FakeMachine(128, 100.0D);
        StressPowerState state = new StressPowerState(machine, 2.5D);
        source.touch(10L);
        state.register(source, 10L);

        assertEquals(100.0D, state.tryFillBuffer(10L), 0.000_001D);
        assertEquals(100.0D, machine.buffer.stored, 0.000_001D);
        assertEquals(60L, source.remaining);
    }

    @Test
    void multipleSourcesShareMachineCapacity() {
        FakeSource first = source(0, 100);
        FakeSource second = source(1, 50);
        FakeMachine machine = new FakeMachine(128, 300.0D);
        StressPowerState state = new StressPowerState(machine, 2.5D);
        first.touch(10L);
        second.touch(10L);
        state.register(first, 10L);
        state.register(second, 10L);

        assertEquals(300.0D, state.tryFillBuffer(10L), 0.000_001D);
        assertEquals(0L, first.remaining);
        assertEquals(30L, second.remaining);
    }

    private static FakeSource source(int x, long budget) {
        return new FakeSource(new StressSupplyKey(new BlockPos(x, 0, 0), Direction.NORTH), budget);
    }

    private static FakeSource source(int x, long budget, int rpm) {
        return new FakeSource(new StressSupplyKey(new BlockPos(x, 0, 0), Direction.NORTH), budget, rpm);
    }

    private static final class FakeSource implements StressSupplySource {

        private final StressSupplyKey key;
        private final long initialBudget;
        private final int rpm;
        private long remaining;
        private long seenTick;
        private int consumeCalls;

        private FakeSource(StressSupplyKey key, long budget) {
            this(key, budget, 512);
        }

        private FakeSource(StressSupplyKey key, long budget, int rpm) {
            this.key = key;
            this.initialBudget = budget;
            this.rpm = rpm;
            this.remaining = budget;
        }

        private void touch(long tick) {
            seenTick = tick;
        }

        @Override
        public Object key() {
            return key;
        }

        @Override
        public int rpm() {
            return rpm;
        }

        @Override
        public long lastSeenTick() {
            return seenTick;
        }

        @Override
        public long availableStress(long requested, Actionable mode, long gameTick) {
            seenTick = gameTick;
            return Math.min(requested, remaining);
        }

        @Override
        public long consumeStress(long requested, Actionable mode, long gameTick) {
            seenTick = gameTick;
            long amount = Math.min(requested, remaining);
            if (mode == Actionable.MODULATE && amount > 0L) {
                consumeCalls++;
                remaining -= amount;
            }
            return amount;
        }

        @Override
        public String toString() {
            return key.toString() + ":" + initialBudget;
        }
    }

    private static final class FakeBuffer implements StressEnergyBuffer {

        private final double capacity;
        private double stored;

        private FakeBuffer(double capacity) {
            this.capacity = capacity;
        }

        @Override
        public double dimensionworks$storedJoules() {
            return stored;
        }

        @Override
        public double dimensionworks$capacityJoules() {
            return capacity;
        }

        @Override
        public double dimensionworks$insertJoulesDirect(double joules, Actionable mode) {
            double accepted = Math.min(Math.max(0.0D, joules), Math.max(0.0D, capacity - stored));
            if (mode == Actionable.MODULATE) {
                stored += accepted;
            }
            return accepted;
        }
    }

    private static final class FakeMachine implements StressPoweredMachine {

        private final int requiredRpm;
        private final FakeBuffer buffer;

        private FakeMachine() {
            this(128, 1_000.0D);
        }

        private FakeMachine(int requiredRpm) {
            this(requiredRpm, 1_000.0D);
        }

        private FakeMachine(int requiredRpm, double capacity) {
            this.requiredRpm = requiredRpm;
            this.buffer = new FakeBuffer(capacity);
        }

        @Override
        public StressEnergyBuffer dimensionworks$stressEnergyBuffer() {
            return buffer;
        }

        @Override
        public void dimensionworks$rechargeFromStress(long gameTick) {
        }

        @Override
        public int dimensionworks$requiredRpm() {
            return requiredRpm;
        }

        @Override
        public StressPowerState dimensionworks$stressPowerState() {
            return null;
        }

        @Override
        public void dimensionworks$registerStressSource(StressSupplySource source, long gameTick) {
        }

        @Override
        public double dimensionworks$speedMultiplier(long gameTick) {
            return 1.0D;
        }

        @Override
        public int dimensionworks$batches(long gameTick) {
            return 1;
        }

        @Override
        public boolean dimensionworks$disabledEnergyInfrastructure() {
            return false;
        }
    }
}
