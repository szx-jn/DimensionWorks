package dev.szx.dimensionworks.mekstress.core;

/** Native integer J per tick; simulations cannot spend and execution cannot overdraw. */
public final class PowerBudget {
    private long tick = Long.MIN_VALUE;
    private long spent;

    public long available(long now, long limit) {
        if (now != tick) { tick = now; spent = 0; }
        return limit > spent ? limit - spent : 0;
    }

    public long extract(long now, long limit, long requested, boolean execute) {
        if (requested <= 0) return 0;
        long accepted = Math.min(requested, available(now, limit));
        if (execute) spent += accepted;
        return accepted;
    }
}
