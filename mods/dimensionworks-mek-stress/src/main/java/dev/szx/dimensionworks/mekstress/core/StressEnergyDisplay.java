package dev.szx.dimensionworks.mekstress.core;

/** Marks an existing Mekanism energy display as belonging to a stress-powered machine. */
public interface StressEnergyDisplay {

    void dimensionworks$setStressDisplay(boolean stressDisplay);

    boolean dimensionworks$isStressDisplay();
}
