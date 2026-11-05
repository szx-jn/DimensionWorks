package dev.szx.dimensionworks.mekstress.integration;

import java.util.Map;
import mekanism.api.Coord4D;
import mekanism.common.tile.TileEntityQuantumEntangloporter;

/** Access to a frequency's active endpoints without exposing the mixin package at runtime. */
public interface QuantumEntangloporterFrequencyAccess {
    Map<Coord4D, TileEntityQuantumEntangloporter> dimensionworks$activeEntangloporters();
}
