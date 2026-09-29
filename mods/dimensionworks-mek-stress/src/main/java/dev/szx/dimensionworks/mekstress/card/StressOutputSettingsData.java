package dev.szx.dimensionworks.mekstress.card;

import dev.szx.dimensionworks.mekstress.StressFormula;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** Pure NBT codec for the stress output card settings. */
public final class StressOutputSettingsData {

    private static final String RPM_KEY = "Rpm";
    private static final String STRESS_LIMIT_KEY = "StressLimit";

    private StressOutputSettingsData() {
    }

    public static StressOutputSettings read(@Nullable CompoundTag tag, StressOutputSettings defaults) {
        if (tag == null || !tag.contains(RPM_KEY) || !tag.contains(STRESS_LIMIT_KEY)) {
            return defaults;
        }
        return new StressOutputSettings(tag.getInt(RPM_KEY), tag.getLong(STRESS_LIMIT_KEY));
    }

    public static void write(CompoundTag tag, StressOutputSettings settings, int maxRpm, long maxStress) {
        int rpm = StressFormula.clampRpm(settings.rpm(), settings.rpm(), maxRpm);
        long stress = StressFormula.clampStress(settings.stressPerTick(), maxStress);
        tag.putInt(RPM_KEY, rpm);
        tag.putLong(STRESS_LIMIT_KEY, stress);
    }
}
