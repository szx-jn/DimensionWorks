package dev.szx.dimensionworks.cavefactory.logic;

import java.util.Objects;

public final class AmpouleState {
    private final String originDimension;
    private final long expiresAtMillis;

    public AmpouleState(String originDimension, long expiresAtMillis) {
        this.originDimension = Objects.requireNonNull(originDimension, "originDimension");
        this.expiresAtMillis = Math.max(0L, expiresAtMillis);
    }

    public static AmpouleState fresh(String originDimension) {
        return new AmpouleState(originDimension, 0L);
    }

    public static AmpouleState observeDimension(
        AmpouleState state,
        String observedDimension,
        long nowMillis,
        long stabilityWindowMillis
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(observedDimension, "observedDimension");
        if (state.originDimension.equals(observedDimension) || state.expiresAtMillis > 0) {
            return state;
        }
        long window = Math.max(1L, stabilityWindowMillis);
        return new AmpouleState(state.originDimension, Math.max(0L, nowMillis) + window);
    }

    public String originDimension() {
        return originDimension;
    }

    public long expiresAtMillis() {
        return expiresAtMillis;
    }

    public boolean hasTimer() {
        return expiresAtMillis > 0;
    }

    public boolean isExpired(long nowMillis) {
        return hasTimer() && nowMillis >= expiresAtMillis;
    }

    public boolean canMerge(AmpouleState other) {
        return other != null
            && originDimension.equals(other.originDimension)
            && expiresAtMillis == other.expiresAtMillis;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AmpouleState other)) {
            return false;
        }
        return expiresAtMillis == other.expiresAtMillis && originDimension.equals(other.originDimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originDimension, expiresAtMillis);
    }

    @Override
    public String toString() {
        return "AmpouleState[" + originDimension + ", expiresAt=" + expiresAtMillis + "]";
    }
}
