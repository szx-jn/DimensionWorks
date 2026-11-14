package dev.szx.dimensionworks.transfer.elastic;

/** A charge belongs to one continuous grounded key hold; airborne ticks never accumulate. */
public final class ElasticCharge {
    public static final int FULL_TICKS = 40;
    private boolean pressed;
    private int ticks;

    public int input(boolean down) {
        if (pressed == down) return 0;
        pressed = down;
        if (down) return 0;
        int released = ticks;
        ticks = 0;
        return released;
    }

    public void tick(boolean grounded) {
        ticks = pressed && grounded ? Math.min(FULL_TICKS, ticks + 1) : 0;
    }

    public int ticks() { return ticks; }

    public static double speed(int ticks) {
        if (ticks <= 0) return 0;
        if (ticks >= FULL_TICKS) return 3.7125 * 5;
        // Preserve the original per-tick strength until the charge is full.
        return 0.35 + 3.3625 * ticks / 60;
    }
}
