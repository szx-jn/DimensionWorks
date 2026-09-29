package dev.szx.dimensionworks.mekstress.api;

import appeng.api.config.Actionable;

/** One per-tick stress provider registered by an AE2 output bus. */
public interface StressSupplySource {

    Object key();

    int rpm();

    long lastSeenTick();

    long availableStress(long requested, Actionable mode, long gameTick);

    long consumeStress(long requested, Actionable mode, long gameTick);
}
