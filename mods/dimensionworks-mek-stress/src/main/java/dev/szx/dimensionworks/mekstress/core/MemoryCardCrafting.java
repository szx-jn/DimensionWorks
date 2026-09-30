package dev.szx.dimensionworks.mekstress.core;

import java.util.function.DoubleSupplier;

/** Pure per-card defect roll used by the crafting integration and contract tests. */
public final class MemoryCardCrafting {
    private MemoryCardCrafting() {
    }

    public static int defectiveCount(int outputCount, double defectiveChance, DoubleSupplier random) {
        if (outputCount <= 0) {
            return 0;
        }
        if (random == null) {
            throw new NullPointerException("random");
        }
        double chance = Math.max(0.0D, Math.min(1.0D, defectiveChance));
        int defective = 0;
        for (int i = 0; i < outputCount; i++) {
            if (random.getAsDouble() < chance) {
                defective++;
            }
        }
        return defective;
    }
}
