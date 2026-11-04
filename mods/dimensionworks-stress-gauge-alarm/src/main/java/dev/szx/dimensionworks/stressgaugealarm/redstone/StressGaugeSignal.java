package dev.szx.dimensionworks.stressgaugealarm.redstone;

/** Matches Create's analog output mapping for its current dial target. */
public final class StressGaugeSignal {
    private StressGaugeSignal() {
    }

    public static int analogOutput(float dialTarget) {
        float clamped = Math.max(0.0F, Math.min(15.0F, dialTarget * 14.0F));
        return (int) Math.ceil(clamped);
    }
}
