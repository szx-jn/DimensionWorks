package dev.szx.dimensionworks.mekstress.core;

import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import mekanism.common.tile.base.TileEntityMekanism;

/** Server-tick-local route reuse for one Mekanism block entity. */
public final class MachinePowerRouteScope {
    private static final ThreadLocal<Entry> CURRENT = new ThreadLocal<>();

    private MachinePowerRouteScope() {
    }

    public static Scope push(TileEntityMekanism tile, MachinePowerManager.DirectSource route) {
        Entry previous = CURRENT.get();
        Entry entry = new Entry(tile, route);
        CURRENT.set(entry);
        return new Scope(entry, previous);
    }

    public static MachinePowerManager.DirectSource current(TileEntityMekanism tile) {
        Entry entry = CURRENT.get();
        return entry != null && entry.tile() == tile ? entry.route() : null;
    }

    /** Distinguishes a scoped lookup that found no route from a lookup outside the scope. */
    public static boolean contains(TileEntityMekanism tile) {
        Entry entry = CURRENT.get();
        return entry != null && entry.tile() == tile;
    }

    public static final class Scope implements AutoCloseable {
        private final Entry entry;
        private final Entry previous;
        private boolean closed;

        private Scope(Entry entry, Entry previous) {
            this.entry = entry;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private record Entry(TileEntityMekanism tile, MachinePowerManager.DirectSource route) {
    }
}
