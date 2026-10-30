package dev.szx.dimensionworks.mekstress.core;

/** Converts an output's reserved mechanical capacity into Create's SU/RPM coefficient. */
public final class StressTransfer {
    private StressTransfer() {}
    public static float capacityPerRpm(long reservedSu, int outputRpm) {
        if (reservedSu <= 0 || outputRpm <= 0) return 0;
        float coefficient = (float) ((double) reservedSu / outputRpm);
        return (double) coefficient * outputRpm > reservedSu ? Math.nextDown(coefficient) : coefficient;
    }
}
