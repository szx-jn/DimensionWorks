package dev.szx.dimensionworks.mekstress.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Stable ordering key for multiple output buses feeding one machine. */
public record StressSupplyKey(BlockPos pos, Direction side) implements Comparable<StressSupplyKey> {

    @Override
    public int compareTo(StressSupplyKey other) {
        int result = Integer.compare(pos.getX(), other.pos.getX());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(pos.getY(), other.pos.getY());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(pos.getZ(), other.pos.getZ());
        if (result != 0) {
            return result;
        }
        return Integer.compare(side.ordinal(), other.side.ordinal());
    }
}
