package dev.szx.dimensionworks.cavefactory.logic;

public final class InternalInsertionMath {
    private InternalInsertionMath() {}

    public static int intoEmptySlot(int incoming, int limit) {
        return Math.min(Math.max(0, incoming), Math.max(0, limit));
    }

    public static int intoExistingSlot(int existing, int incoming, int limit) {
        int room = Math.max(0, Math.max(0, limit) - Math.max(0, existing));
        return Math.min(Math.max(0, incoming), room);
    }
}
