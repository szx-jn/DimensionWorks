package dev.szx.dimensionworks.mekstress.core;

import java.util.Objects;
import java.util.Set;

/** Pure routing rules; callers must enforce these at each actual transfer boundary. */
public final class DimensionTransportPolicy {
    private static final Set<String> VANILLA_DIMENSIONS = Set.of(
        "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

    private DimensionTransportPolicy() {}

    public static boolean standardTransportAllowed(String from, String to) {
        requireDimension(from);
        requireDimension(to);
        return from.equals(to) || (VANILLA_DIMENSIONS.contains(from) && VANILLA_DIMENSIONS.contains(to));
    }

    public static boolean channelExtractionAllowed(String origin, String destination) {
        requireDimension(origin);
        requireDimension(destination);
        return !origin.equals(destination);
    }

    private static void requireDimension(String dimension) {
        Objects.requireNonNull(dimension);
        if (dimension.isBlank()) throw new IllegalArgumentException("Dimension ID cannot be blank");
    }
}
