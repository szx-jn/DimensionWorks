package dev.szx.dimensionworks.cavefactory.logic;

public final class MachineTuning {
    public static final int BASE_FLUID_CAPACITY = 4_000;
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
