package dev.szx.dimensionworks.mekstress.memory;

/** Coalesces reads until the game tick or a known input changes. */
final class MemorySnapshotGate {
    private long publishedTick = Long.MIN_VALUE;
    private boolean dirty = true;

    boolean needsRefresh(long tick) {
        return dirty || publishedTick != tick;
    }

    void invalidate() {
        dirty = true;
    }

    void published(long tick) {
        publishedTick = tick;
        dirty = false;
    }
}
