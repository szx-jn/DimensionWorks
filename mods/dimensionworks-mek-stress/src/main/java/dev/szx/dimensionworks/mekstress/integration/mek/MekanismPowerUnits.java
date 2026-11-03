package dev.szx.dimensionworks.mekstress.integration.mek;

import mekanism.api.math.FloatingLong;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;

/** Read the forward conversion, avoiding Mekanism's four-decimal cached inverse. */
public final class MekanismPowerUnits {
    private MekanismPowerUnits() {}

    public static double joulesPerFe() {
        return EnergyUnit.FORGE_ENERGY.convertFrom(FloatingLong.ONE).doubleValue();
    }

    public static boolean valid() {
        double ratio = joulesPerFe();
        return Double.isFinite(ratio) && ratio > 0;
    }

    public static double fePerTick(FloatingLong joules) {
        double ratio = joulesPerFe();
        return Double.isFinite(ratio) && ratio > 0 ? joules.doubleValue() / ratio : 0;
    }
}
