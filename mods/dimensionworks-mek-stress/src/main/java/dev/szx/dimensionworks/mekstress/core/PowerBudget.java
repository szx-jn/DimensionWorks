package dev.szx.dimensionworks.mekstress.core;

/** Per-tick finite virtual energy; simulations cannot spend or create budget. */
public final class PowerBudget {
    private long tick = Long.MIN_VALUE;
    private double spent;

    public double available(long now, double limit) {
        if (now != tick) { tick = now; spent = 0; }
        return Double.isFinite(limit) && limit > 0 ? Math.max(0, limit - spent) : 0;
    }

    public double extract(long now, double limit, double requested, boolean execute) {
        if (!Double.isFinite(requested) || requested <= 0) return 0;
        double accepted = Math.min(requested, available(now, limit));
        if (execute) spent += accepted;
        return accepted;
    }
}
