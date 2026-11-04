package dev.szx.dimensionworks.mekstress.core;

import java.util.Map;
import java.util.WeakHashMap;

/** Per-output-slot fractional credit. Preview never changes credit; only delivery commits it. */
public final class FractionalOutputLedger {
    private final Map<Object, Entry> entries = new WeakHashMap<>();

    private record Entry(Object product, double remainder) {}
    private record Calculation(long amount, double remainder) {}

    private Calculation calculate(Object slot, Object product, long base, int operations, double multiplier) {
        if (slot == null || product == null || base <= 0 || operations <= 0
            || !Double.isFinite(multiplier) || multiplier < 1) return new Calculation(0, 0);
        Entry previous = entries.get(slot);
        double remainder = previous != null && product.equals(previous.product()) ? previous.remainder() : 0;
        double total = (double) base * operations * multiplier + remainder;
        if (!Double.isFinite(total) || total >= Long.MAX_VALUE) return new Calculation(Long.MAX_VALUE, 0);
        long whole = (long) Math.floor(total);
        return new Calculation(whole, total - whole);
    }

    public long preview(Object slot, Object product, long base, int operations, double multiplier) {
        return calculate(slot, product, base, operations, multiplier).amount();
    }

    public long commit(Object slot, Object product, long base, int operations, double multiplier) {
        Calculation result = calculate(slot, product, base, operations, multiplier);
        if (slot != null && product != null && base > 0 && operations > 0 && multiplier >= 1
            && Double.isFinite(multiplier)) entries.put(slot, new Entry(product, result.remainder()));
        return result.amount();
    }

    public int maxOperations(Object slot, Object product, long base, long room, double multiplier) {
        if (slot == null || product == null || base <= 0 || room <= 0
            || !Double.isFinite(multiplier) || multiplier < 1) return 0;
        int low = 0, high = (int) Math.min((long) Integer.MAX_VALUE, room / base);
        while (low < high) {
            int mid = low + (int) (((long) high - low + 1) / 2);
            if (preview(slot, product, base, mid, multiplier) <= room) low = mid;
            else high = mid - 1;
        }
        return low;
    }
}
