package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DimensionTransportPolicyTest {
    @Test void legacyOnlyCrossesBetweenTheThreeVanillaDimensions() {
        String[] vanilla = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};
        for (String from : vanilla) for (String to : vanilla)
            assertTrue(DimensionTransportPolicy.legacyTransportAllowed(from, to));
        for (String dimension : new String[] {"aether:the_aether", "twilightforest:twilight_forest",
                "alex_caves_dimensions:magnetic_caves", "dw:transfer"}) {
            assertTrue(DimensionTransportPolicy.legacyTransportAllowed(dimension, dimension));
            assertFalse(DimensionTransportPolicy.legacyTransportAllowed(vanilla[0], dimension));
            assertFalse(DimensionTransportPolicy.legacyTransportAllowed(dimension, vanilla[1]));
            assertFalse(DimensionTransportPolicy.channelExtractionAllowed(dimension, dimension));
            assertTrue(DimensionTransportPolicy.channelExtractionAllowed(dimension, vanilla[2]));
        }
    }

    @Test void missingOriginCannotBeUsedToBypassPolicy() {
        assertThrows(NullPointerException.class, () -> DimensionTransportPolicy.channelExtractionAllowed(null, "aether:the_aether"));
        assertThrows(IllegalArgumentException.class, () -> DimensionTransportPolicy.legacyTransportAllowed("", "minecraft:overworld"));
    }
}
