package dev.szx.dimensionworks.cavefactory.logic;

public final class AtomicOutputPlanner {
    private AtomicOutputPlanner() {}

    public static boolean canFit(int required, int present, int capacity) {
        int needed = Math.max(0, required);
        int existing = Math.max(0, present);
        int limit = Math.max(0, capacity);
        return existing + needed <= limit;
    }

    /** Checks whether {@code required} is no greater than {@code available}. */
    public static boolean allFit(int required, int available) {
        return Math.max(0, required) <= Math.max(0, available);
    }
}
