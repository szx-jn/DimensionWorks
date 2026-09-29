package dev.szx.dimensionworks.cavefactory.logic;

public final class DeterministicBonus {
    private DeterministicBonus() {}

    public static boolean triggers(long successfulOperations, int interval) {
        if (successfulOperations <= 0 || interval <= 0) {
            return false;
        }
        return successfulOperations % interval == 0;
    }

    public static long occurrences(long successfulOperations, int ratePerHundred) {
        if (successfulOperations <= 0 || ratePerHundred <= 0) {
            return 0;
        }
        int clampedRate = Math.min(100, ratePerHundred);
        return successfulOperations * clampedRate / 100;
    }
}
