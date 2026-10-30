package dev.szx.dimensionworks.rpmlimit.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.Test;

class FinalProductionGateTest {

    @Test
    void emptyOutputAlwaysPasses() {
        assertTrue(FinalProductionGate.tryConsume(
            java.util.UUID.randomUUID(), (ItemStack) null, 64L, 10L));
        assertTrue(FinalProductionGate.tryConsume(
            java.util.UUID.randomUUID(), (ItemStack) null, 0L, 10L));
    }

    @Test
    void configuredRateUsesPolicyAndConfiguredLimits() {
        assertEquals(16.0D, FinalProductionGate.configuredRate(0, 16, 128));
        assertEquals(128.0D, FinalProductionGate.configuredRate(
            GearHeartStateBits.FINAL_PRODUCTION, 16, 128));
        assertEquals(Double.POSITIVE_INFINITY, FinalProductionGate.configuredRate(
            GearHeartStateBits.ALL, 16, 128));
    }

    @Test
    void unownedProductionUsesTheSharedOwnerInsteadOfDereferencingNull() {
        assertEquals(GearHeartOwnerResolver.SHARED_OWNER, GearHeartOwnerResolver.normalize(null));
    }

    private static final class GearHeartStateBits {
        private static final int FINAL_PRODUCTION = 1 << 6;
        private static final int ALL = 0b1111111;
    }
}
