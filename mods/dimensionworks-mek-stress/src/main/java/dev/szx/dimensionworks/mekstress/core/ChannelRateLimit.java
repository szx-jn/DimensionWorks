package dev.szx.dimensionworks.mekstress.core;

/** Shared insertion budget in twentieths of a unit, capped at one game second. */
public final class ChannelRateLimit {
    private long rate;
    private long credits;
    private long lastTick = Long.MIN_VALUE;

    public ChannelRateLimit(long unitsPerSecond) {
        setRate(unitsPerSecond);
    }

    /** Tier changes preserve earned credit but never let the bucket exceed the new one-second cap. */
    public void setRate(long unitsPerSecond) {
        if (unitsPerSecond <= 0 || unitsPerSecond > Long.MAX_VALUE / 20) throw new IllegalArgumentException();
        rate = unitsPerSecond;
        credits = Math.min(credits, rate * 20);
    }

    /** A paused/unloaded channel earns no credit, including across tick discontinuities. */
    public void tick(long tick, boolean loaded) {
        if (lastTick != Long.MIN_VALUE && tick <= lastTick) return;
        long cap = rate * 20;
        if (loaded) credits = credits >= cap - rate ? cap : credits + rate;
        lastTick = tick;
    }

    public long available() { return credits / 20; }

    /** Call only for the amount actually inserted, never for simulated capability calls. */
    public void commit(long amount) {
        if (amount < 0 || amount > available()) throw new IllegalArgumentException();
        credits -= amount * 20;
    }
}
