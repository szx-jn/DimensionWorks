package dev.szx.dimensionworks.stressgaugealarm.mixin;

import dev.szx.dimensionworks.stressgaugealarm.redstone.StressGaugeSignal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class StressGaugeSignalTest {
    @Test
    void matchesCreateAnalogOutputRoundingAndClamp() {
        assertEquals(0, StressGaugeSignal.analogOutput(0.0F));
        assertEquals(1, StressGaugeSignal.analogOutput(0.01F));
        assertEquals(7, StressGaugeSignal.analogOutput(0.5F));
        assertEquals(14, StressGaugeSignal.analogOutput(1.0F));
        assertEquals(15, StressGaugeSignal.analogOutput(1.125F));
        assertEquals(0, StressGaugeSignal.analogOutput(-1.0F));
    }
}
