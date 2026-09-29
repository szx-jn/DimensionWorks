package dev.szx.dimensionworks.cavefactory.logic;

public final class StabilizationMath {
    public record Result(int outputs, int remainingFluid) {
        public boolean hasOutput() {
            return outputs > 0;
        }
    }

    private StabilizationMath() {}

    public static Result evaluate(int availableFluid, int requiredFluid) {
        int available = Math.max(0, availableFluid);
        int required = requiredFluid <= 0 ? 1_000 : requiredFluid;
        return new Result(available / required, available % required);
    }
}
