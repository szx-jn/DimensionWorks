package dev.szx.dimensionworks.mekstress.core;

import appeng.api.config.Actionable;

/** Internal access to a Mekanism energy buffer used by the stress bridge. */
public interface StressEnergyBuffer {

    double dimensionworks$storedJoules();

    double dimensionworks$capacityJoules();

    double dimensionworks$insertJoulesDirect(double joules, Actionable mode);
}
