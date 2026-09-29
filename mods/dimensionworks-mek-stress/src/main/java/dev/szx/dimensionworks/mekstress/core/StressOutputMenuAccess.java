package dev.szx.dimensionworks.mekstress.core;

import dev.szx.dimensionworks.mekstress.card.StressOutputSettings;

/** Accessors exposed by the AE2 output bus menu mixin. */
public interface StressOutputMenuAccess {

    String ACTION_SET_STRESS = "dimensionworks_mek_stress.set_stress";

    boolean dimensionworks$hasStressCard();

    int dimensionworks$getStressRpm();

    long dimensionworks$getStressLimit();

    void dimensionworks$requestStressSettings(StressOutputSettings settings);

    void dimensionworks$applyStressSettings(StressOutputSettings settings);

    void dimensionworks$refreshStressSettings();
}
