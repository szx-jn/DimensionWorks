package dev.szx.dimensionworks.rpmlimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OverspeedCurveTest {

    private static final double CAP = 15d;
    private static final double MIXER = 512d;
    private static final double PRESS = 512d;
    private static final double FAN = 256d;
    private static final double SAW = 3072d;
    private static final double MILLSTONE = 8192d;

    private final List<String> warnings = new ArrayList<>();

    @BeforeEach
    void resetCurve() {
        warnings.clear();
        OverspeedCurve.reload(OverspeedCurve.DEFAULT_TABLE, warnings::add);
    }

    private double multiplierAt(double rpm, double saturation) {
        return OverspeedCurve.multiplier(rpm, saturation, CAP);
    }

    @Test
    void stepsUseTheLastReachedBreakpoint() {
        assertEquals(1d, multiplierAt(512d, MIXER));
        assertEquals(1d, multiplierAt(1023d, MIXER));
        assertEquals(2d, multiplierAt(1024d, MIXER));
        assertEquals(2d, multiplierAt(2047d, MIXER));
        assertEquals(2.5d, multiplierAt(2048d, MIXER));
        assertEquals(3.5d, multiplierAt(4096d, MIXER));
        assertEquals(5d, multiplierAt(5120d, MIXER));
        assertEquals(7d, multiplierAt(6144d, MIXER));
    }

    @Test
    void reverseRotationFollowsTheSameCurve() {
        assertEquals(2d, multiplierAt(-1024d, MIXER));
    }

    @Test
    void curveIsCappedAtFifteenTimes() {
        assertEquals(15d, multiplierAt(10240d, MIXER));
        assertEquals(15d, multiplierAt(20000d, MIXER));
        assertEquals(15d, multiplierAt(5120d, FAN));
    }

    @Test
    void everyMachineConvertsItsOwnSaturationPoint() {
        assertEquals(1d, multiplierAt(256d, FAN));
        assertEquals(2d, multiplierAt(512d, FAN));
        assertEquals(1d, multiplierAt(3072d, SAW));
        assertEquals(2d, multiplierAt(10240d, SAW));
        assertEquals(1d, multiplierAt(8192d, MILLSTONE));
        assertEquals(2d, multiplierAt(1024d, PRESS));
    }

    @Test
    void malformedTableKeepsThePreviousOne() {
        OverspeedCurve.reload("1:1,two:2", warnings::add);
        assertFalse(warnings.isEmpty());
        assertEquals(2d, multiplierAt(1024d, MIXER));
    }

    @Test
    void emptyTableKeepsThePreviousOne() {
        OverspeedCurve.reload("", warnings::add);
        assertFalse(warnings.isEmpty());
        assertEquals(OverspeedCurve.defaults(), OverspeedCurve.active());
    }

    @Test
    void fractionalMultipliersAverageOutOverTime() {
        long batches = 0;
        for (long tick = 0; tick < 1000; tick++)
            batches += OverspeedCurve.batches(2.5d, tick);
        assertEquals(2500L, batches);
    }

    @Test
    void integralMultipliersBatchEveryTick() {
        assertEquals(2, OverspeedCurve.batches(2d, 7L));
        assertEquals(15, OverspeedCurve.batches(15d, 7L));
    }

    @Test
    void aSingleBatchIsTheFloor() {
        assertEquals(1, OverspeedCurve.batches(1d, 3L));
        assertEquals(1, OverspeedCurve.batches(0.5d, 3L));
    }
}
