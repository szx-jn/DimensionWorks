package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class FractionalOutputLedgerTest {
    @Test
    void fractionalYieldAccumulatesOnlyAfterDelivery() {
        FractionalOutputLedger ledger = new FractionalOutputLedger();
        Object slot = new Object();
        for (int i = 0; i < 3; i++) {
            assertEquals(1, ledger.preview(slot, "iron", 1, 1, 1.25));
            assertEquals(1, ledger.preview(slot, "iron", 1, 1, 1.25));
            assertEquals(1, ledger.commit(slot, "iron", 1, 1, 1.25));
        }
        assertEquals(2, ledger.preview(slot, "iron", 1, 1, 1.25));
        assertEquals(0, ledger.maxOperations(slot, "iron", 1, 1, 1.25));
        assertEquals(1, ledger.maxOperations(slot, "iron", 1, 2, 1.25));
        assertEquals(2, ledger.commit(slot, "iron", 1, 1, 1.25));
        assertEquals(1, ledger.preview(slot, "iron", 1, 1, 1.25));
    }

    @Test
    void separatesParallelSlotsAndProductChanges() {
        FractionalOutputLedger ledger = new FractionalOutputLedger();
        Object a = new Object(), b = new Object();
        assertEquals(1, ledger.commit(a, "iron", 1, 1, 1.5));
        assertEquals(1, ledger.preview(b, "iron", 1, 1, 1.5));
        assertEquals(1, ledger.preview(a, "gold", 1, 1, 1.5));
        assertEquals(1, ledger.commit(a, "gold", 1, 1, 1.5));
        assertEquals(1, ledger.preview(a, "iron", 1, 1, 1.5));
        assertEquals(2, ledger.commit(a, "gold", 1, 1, 1.5));
    }

    @Test
    void producesExpectedTotalsAcrossRatesAndParallelOperations() {
        for (double rate : new double[]{1.25, 1.5, 1.75, 2.0}) {
            FractionalOutputLedger ledger = new FractionalOutputLedger();
            Object slot = new Object();
            long total = 0;
            for (int i = 0; i < 8; i++) total += ledger.commit(slot, "item", 1, 1, rate);
            assertEquals((long) (8 * rate), total);
        }
        FractionalOutputLedger ledger = new FractionalOutputLedger();
        Object slot = new Object();
        assertEquals(3, ledger.commit(slot, "item", 1, 2, 1.5));
        assertEquals(1, ledger.commit(slot, "item", 1, 1, 1.5));
        assertEquals(2, ledger.commit(slot, "item", 1, 1, 1.5));
    }

    @Test
    void speedChangesDoNotCreateOrEraseEarnedFraction() {
        FractionalOutputLedger ledger = new FractionalOutputLedger();
        Object slot = new Object();
        assertEquals(1, ledger.commit(slot, "item", 1, 1, 1.25));
        assertEquals(2, ledger.commit(slot, "item", 1, 1, 1.75));
        assertEquals(0, ledger.maxOperations(slot, "item", 1, 0, 1.5));
    }
}
