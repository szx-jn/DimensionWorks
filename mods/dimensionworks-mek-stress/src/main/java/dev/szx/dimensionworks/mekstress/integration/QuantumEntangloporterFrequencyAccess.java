package dev.szx.dimensionworks.mekstress.integration;

import java.util.Collection;
import mekanism.common.tile.TileEntityQuantumEntangloporter;

/** Access to a frequency's active endpoints without exposing the mixin package at runtime. */
public interface QuantumEntangloporterFrequencyAccess {
    Collection<TileEntityQuantumEntangloporter> dimensionworks$activeEntangloporters();
}
