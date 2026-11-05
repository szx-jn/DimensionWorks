package dev.szx.dimensionworks.mekstress.core;

/** Capacity is shared by a channel, rather than multiplied by its endpoint count. */
public record ChannelCapacity(int itemSlots, long fluidMillibuckets) {
    public static final int UPGRADE_SLOTS = 20;
    public static final int BASE_ITEM_SLOTS = 27;
    public static final int ITEM_SLOTS_PER_CARD = 27;
    public static final long BASE_FLUID_MB = 16_000;
    public static final long FLUID_MB_PER_CARD = 32_000;

    public static ChannelCapacity of(int itemCards, int fluidCards) {
        if (itemCards < 0 || fluidCards < 0 || itemCards > UPGRADE_SLOTS
            || fluidCards > UPGRADE_SLOTS - itemCards) throw new IllegalArgumentException();
        return new ChannelCapacity(BASE_ITEM_SLOTS + itemCards * ITEM_SLOTS_PER_CARD,
            BASE_FLUID_MB + fluidCards * FLUID_MB_PER_CARD);
    }
}
