package dev.szx.dimensionworks.cavefactory.logic;

public final class MachineTuning {
    public static final int BASE_FLUID_CAPACITY = 4_000;
    public static final int BASE_KINETIC_RPM = 512;
    public static final float STRESS_IMPACT = 64.0F;
    public static final int PHASE_BATCHES = 8;
    public static final int STABILITY_SECONDS = 60;
    public static final int STABILIZATION_FLUID = 1_000;

    private MachineTuning() {}

    public static int scaleFluidCapacity(int capacity, boolean pressureBuffer) {
        int base = Math.max(0, capacity);
        if (!pressureBuffer) {
            return base;
        }
        int percent = dev.szx.dimensionworks.cavefactory.CaveFactoryConfig.numericModulePercent();
        return base + base * percent / 100;
    }
}
