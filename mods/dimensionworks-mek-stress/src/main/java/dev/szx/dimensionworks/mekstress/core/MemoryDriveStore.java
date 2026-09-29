package dev.szx.dimensionworks.mekstress.core;

/**
 * Per-drive SU inventory. The drive owns its stock so the total remains
 * recoverable when a logical grid is unloaded or split.
 */
public final class MemoryDriveStore {
    private final MemoryTier tier;
    private int cardCount;
    private long cardCapacitySu;
    private long storedSu;

    public MemoryDriveStore(MemoryTier tier, int cardCount) {
        if (tier == null) {
            throw new NullPointerException("tier");
        }
        this.tier = tier;
        this.cardCapacitySu = tier.cardCapacitySu();
        setCardCount(cardCount);
    }

    public MemoryTier tier() {
        return tier;
    }

    public int cardCount() {
        return cardCount;
    }

    public void setCardCount(int cardCount) {
        this.cardCount = Math.max(0, Math.min(cardCount, tier.slotsPerDrive()));
        truncateToCapacity();
    }

    public long capacitySu() {
        return Math.multiplyExact((long) cardCount, cardCapacitySu);
    }

    public void setCardCapacitySu(long cardCapacitySu) {
        this.cardCapacitySu = Math.max(0L, cardCapacitySu);
        truncateToCapacity();
    }

    public long storedSu() {
        return storedSu;
    }

    public long insert(long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        long accepted = Math.min(amount, Math.max(0L, capacitySu() - storedSu));
        storedSu += accepted;
        return accepted;
    }

    public long extract(long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        long extracted = Math.min(amount, storedSu);
        storedSu -= extracted;
        return extracted;
    }

    public long truncateToCapacity() {
        long capacity = capacitySu();
        if (storedSu <= capacity) {
            return 0L;
        }
        long removed = storedSu - capacity;
        storedSu = capacity;
        return removed;
    }

    public void restore(long amount) {
        storedSu = Math.max(0L, amount);
        truncateToCapacity();
    }
}
