package dev.szx.dimensionworks.mekstress.core;

/** Instantaneous power only: one FE/t is two SU, never stored energy. */
public final class EnergyPowerMath {
    public static final double SU_PER_FE = 2.0D;
    private EnergyPowerMath() {}

    public static double su(double fePerTick) {
        return Double.isFinite(fePerTick) && fePerTick > 0 ? fePerTick * SU_PER_FE : 0;
    }

    public static float coefficient(double su, float rpm, int ports) {
        if (!Double.isFinite(su) || su <= 0 || !Float.isFinite(rpm) || rpm == 0 || ports <= 0) return 0;
        double exact = Math.min(Float.MAX_VALUE / 65536.0D, su / (Math.abs((double) rpm) * ports));
        float rounded = (float) exact;
        return rounded > exact ? Math.nextDown(rounded) : rounded;
    }

    public static float demandCoefficient(double su, float rpm) {
        if (!Double.isFinite(su) || su <= 0 || !Float.isFinite(rpm) || rpm == 0) return 0;
        double exact = Math.min(Float.MAX_VALUE / 65536.0D, su / Math.abs((double) rpm));
        float rounded = (float) exact;
        return rounded < exact ? Math.nextUp(rounded) : rounded;
    }

    public static boolean isLive(long producedTick, long currentTick) {
        return producedTick >= 0 && currentTick >= producedTick && currentTick - producedTick <= 1;
    }
}
