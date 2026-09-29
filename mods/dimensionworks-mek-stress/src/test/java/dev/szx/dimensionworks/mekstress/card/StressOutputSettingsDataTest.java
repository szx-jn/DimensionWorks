package dev.szx.dimensionworks.mekstress.card;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StressOutputSettingsDataTest {

    @Test
    void nbtRoundTripsAndClampsToCardLimits() {
        CompoundTag tag = new CompoundTag();
        StressOutputSettingsData.write(tag, new StressOutputSettings(99_999, 9_999_999L), 10_240, 1_048_576L);

        assertEquals(new StressOutputSettings(10_240, 1_048_576L),
            StressOutputSettingsData.read(tag, new StressOutputSettings(32, 1_024L)));
    }

    @Test
    void missingNbtUsesDefaults() {
        StressOutputSettings defaults = new StressOutputSettings(32, 1_024L);
        assertEquals(defaults, StressOutputSettingsData.read(new CompoundTag(), defaults));
        assertEquals(defaults, StressOutputSettingsData.read(null, defaults));
    }
}
