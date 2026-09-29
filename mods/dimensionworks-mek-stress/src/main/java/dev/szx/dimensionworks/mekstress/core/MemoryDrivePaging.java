package dev.szx.dimensionworks.mekstress.core;

/** Fixed 4x5 paging geometry for the AE2-derived Memory Drive screen. */
public final class MemoryDrivePaging {
    public static final int COLUMNS = 4;
    public static final int ROWS = 5;
    public static final int SLOTS_PER_PAGE = COLUMNS * ROWS;
    public static final int SLOT_SPACING = 18;

    private MemoryDrivePaging() {
    }

    public static int pageCount(int totalSlots) {
        return Math.max(1, (Math.max(0, totalSlots) + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
    }

    public static int pageOffset(int page) {
        return Math.max(0, page) * SLOTS_PER_PAGE;
    }

    public static int slotX(int index) {
        int normalized = Math.max(0, index);
        return normalized % COLUMNS * SLOT_SPACING;
    }

    public static int slotY(int index) {
        int normalized = Math.max(0, index);
        return normalized / COLUMNS * SLOT_SPACING;
    }
}
