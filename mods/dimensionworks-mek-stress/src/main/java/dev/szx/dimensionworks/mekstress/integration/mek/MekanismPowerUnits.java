package dev.szx.dimensionworks.mekstress.integration.mek;

import mekanism.common.util.UnitDisplayUtils.EnergyUnit;

/** Read the forward conversion, avoiding Mekanism's four-decimal cached inverse. */
public final class MekanismPowerUnits {
    private MekanismPowerUnits() {}

    public static double joulesPerFe() {
        return EnergyUnit.FORGE_ENERGY.getConversion();
    }

    public static boolean valid() {
        double ratio = joulesPerFe();
        return Double.isFinite(ratio) && ratio > 0;
    }

    public static double fePerTick(long joules) {
        double ratio = joulesPerFe();
        return Double.isFinite(ratio) && ratio > 0 ? (double) joules / ratio : 0;
    }
}
