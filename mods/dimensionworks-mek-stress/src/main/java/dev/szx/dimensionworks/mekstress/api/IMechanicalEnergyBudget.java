package dev.szx.dimensionworks.mekstress.api;

/** Freeze the power reserved by the network before the original machine tick. */
public interface IMechanicalEnergyBudget {
    void dimensionworks$prepareBudget(long tick, double joulesPerTick);
}
