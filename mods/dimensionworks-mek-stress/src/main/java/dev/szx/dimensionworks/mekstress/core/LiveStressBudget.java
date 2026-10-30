package dev.szx.dimensionworks.mekstress.core;

import java.util.HashMap;
import java.util.Map;

/** A live capacity ledger. Entries replace reservations; elapsed time never consumes SU. */
public final class LiveStressBudget<K> {
    private final Map<K, Long> inputs = new HashMap<>();
    private final Map<K, Long> outputs = new HashMap<>();

    public void input(K key, long capacity) { replace(inputs, key, capacity); }
    public void output(K key, long reserved) { replace(outputs, key, reserved); }
    public void remove(K key) { inputs.remove(key); outputs.remove(key); }
    public void clear() { inputs.clear(); outputs.clear(); }

    public Snapshot snapshot(int lossBasisPoints) {
        long raw = sum(inputs);
        long effective = InputStressMath.convert(raw, lossBasisPoints, 0).effectiveSu();
        long reserved = sum(outputs);
        long remaining = effective;
        boolean overAllocated = false;
        for (long value : outputs.values()) {
            if (value > remaining) { overAllocated = true; break; }
            remaining -= value;
        }
        return new Snapshot(raw, effective, reserved, overAllocated ? 0 : remaining,
            effective <= 0 || overAllocated);
    }

    public static long stress(float coefficient, float rpm) {
        if (!Float.isFinite(coefficient) || coefficient <= 0 || !Float.isFinite(rpm)) return 0;
        double stress = (double) coefficient * Math.abs(rpm);
        return stress >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor(stress);
    }

    private static <K> void replace(Map<K, Long> entries, K key, long value) {
        if (value > 0) entries.put(key, value); else entries.remove(key);
    }

    private static long sum(Map<?, Long> entries) {
        long sum = 0;
        for (long value : entries.values()) {
            if (sum > Long.MAX_VALUE - value) return Long.MAX_VALUE;
            sum += value;
        }
        return sum;
    }

    public record Snapshot(long rawSu, long effectiveSu, long reservedSu, long availableSu, boolean overloaded) {}
}
