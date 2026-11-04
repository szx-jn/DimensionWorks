package dev.szx.dimensionworks.stressgaugealarm.mixin;

/** Matches Create's analog output mapping for its current dial target. */
final class StressGaugeSignal {
    private StressGaugeSignal() {
    }

    static int analogOutput(float dialTarget) {
        float clamped = Math.max(0.0F, Math.min(15.0F, dialTarget * 14.0F));
        return (int) Math.ceil(clamped);
    }
}
