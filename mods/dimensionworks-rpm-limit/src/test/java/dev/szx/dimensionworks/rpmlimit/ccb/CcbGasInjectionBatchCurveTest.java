package dev.szx.dimensionworks.rpmlimit.ccb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CcbGasInjectionBatchCurveTest {

    @Test
    void mapsPumpSpeedPressureTiersToBatchCeilings() {
        assertEquals(40_000, CcbGasInjectionBatchCurve.MINIMUM_PUMP_BOOST_PA);
        assertFalse(CcbGasInjectionBatchCurve.hasMinimumPumpBoost(39_999));
        assertTrue(CcbGasInjectionBatchCurve.hasMinimumPumpBoost(40_000));
        assertEquals(0, CcbGasInjectionBatchCurve.limitBatch(39_999, 1));
        assertEquals(0, CcbGasInjectionBatchCurve.limitBatch(39_999, 20));
        assertEquals(1, CcbGasInjectionBatchCurve.limitBatch(40_000, 1));
        assertEquals(16, CcbGasInjectionBatchCurve.limitBatch(40_000, 20));
        assertEquals(0, CcbGasInjectionBatchCurve.limitBatch(320_000, 0));
        assertEquals(1, CcbGasInjectionBatchCurve.batchLimit(9999));
        assertEquals(8, CcbGasInjectionBatchCurve.batchLimit(10000));
        assertEquals(12, CcbGasInjectionBatchCurve.batchLimit(20000));
        assertEquals(16, CcbGasInjectionBatchCurve.batchLimit(40000));
        assertEquals(24, CcbGasInjectionBatchCurve.batchLimit(80000));
        assertEquals(32, CcbGasInjectionBatchCurve.batchLimit(160000));
        assertEquals(48, CcbGasInjectionBatchCurve.batchLimit(240000));
        assertEquals(64, CcbGasInjectionBatchCurve.batchLimit(320000));
        assertEquals(64, CcbGasInjectionBatchCurve.batchLimit(400000));
    }
}
