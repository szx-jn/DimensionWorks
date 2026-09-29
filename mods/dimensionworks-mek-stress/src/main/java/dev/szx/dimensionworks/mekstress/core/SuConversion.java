package dev.szx.dimensionworks.mekstress.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Conversion boundary for Mekanism FE-based machine work. */
public final class SuConversion {
    private SuConversion() {
    }

    public static long ceilFromFe(long fe, double fePerSu) {
        if (fe <= 0L) {
            return 0L;
        }
        if (!Double.isFinite(fePerSu) || fePerSu <= 0.0D) {
            throw new IllegalArgumentException("FE per SU must be finite and positive");
        }
        BigDecimal su = BigDecimal.valueOf(fe)
            .divide(BigDecimal.valueOf(fePerSu), 0, RoundingMode.CEILING);
        return su.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0 ? Long.MAX_VALUE : su.longValue();
    }
}
