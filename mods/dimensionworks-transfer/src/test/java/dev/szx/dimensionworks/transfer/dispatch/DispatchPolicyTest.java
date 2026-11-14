package dev.szx.dimensionworks.transfer.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void upgradeOpensOnlyTheSixCaveRoutes() {
        for (AlexCavesDestination cave : AlexCavesDestination.values()) {
            assertFalse(DispatchPolicy.routeAllowed(cave.route(), false));
            assertTrue(DispatchPolicy.routeAllowed(cave.route(), true));
        }
        for (String route : new String[]{"aether", "alexscaves", "alex_unknown", "alex_caves_dimensions:magnetic_caves"}) {
            assertFalse(DispatchPolicy.routeAllowed(route, true));
        }
        assertTrue(DispatchPolicy.routeAllowed("twilight", false));
    }
}
