package dev.szx.dimensionworks.transfer.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class DispatchPolicyTest {
    @Test
    void onlyAnEligiblePlayerCanEnterAnAvailableUnlockedDestination() {
        assertEquals(DispatchPolicy.Decision.ALLOWED, DispatchPolicy.decide(true, true, true, true, false, true));
        assertEquals(DispatchPolicy.Decision.WRONG_DIMENSION, DispatchPolicy.decide(false, true, true, true, false, true));
        assertEquals(DispatchPolicy.Decision.NOT_OWNER, DispatchPolicy.decide(true, false, true, true, false, true));
        assertEquals(DispatchPolicy.Decision.TOO_FAR, DispatchPolicy.decide(true, true, false, true, false, true));
        assertEquals(DispatchPolicy.Decision.NOT_PACKED, DispatchPolicy.decide(true, true, true, false, false, true));
        assertEquals(DispatchPolicy.Decision.LOCKED, DispatchPolicy.decide(true, true, true, true, true, true));
        assertEquals(DispatchPolicy.Decision.UNAVAILABLE, DispatchPolicy.decide(true, true, true, true, false, false));
    }
}
