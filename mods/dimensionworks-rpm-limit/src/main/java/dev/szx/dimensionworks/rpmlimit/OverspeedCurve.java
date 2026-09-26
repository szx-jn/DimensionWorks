package dev.szx.dimensionworks.rpmlimit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Step lookup table behind the overspeed batch multiplier.
 *
 * <p>The table is expressed in ratios rather than absolute RPM so that every machine can use the
 * same curve: {@code r = |current RPM| / thisMachineSaturationRpm}. Values are taken from the last
 * breakpoint that has been reached, which is what "阶跃" means here: 1536 RPM on a 512-saturation
 * machine (r = 3) still yields the r = 2 multiplier.
 *
 * <p>This class deliberately avoids Minecraft types so the curve can be unit tested.
 */
public final class OverspeedCurve {

    public static final String DEFAULT_TABLE =
        "1:1,2:2,4:2.5,8:3.5,10:5,12:7,14:8.5,16:10,18:11.5,20:15";

    public record Point(double ratio, double multiplier) {}

    private static final List<Point> DEFAULT = List.copyOf(parse(DEFAULT_TABLE));

    private static volatile List<Point> active = DEFAULT;

    private OverspeedCurve() {}

    /**
     * Parses a {@code ratio:multiplier,ratio:multiplier,...} specification.
     *
     * @throws IllegalArgumentException when the specification is empty or contains a malformed or
     *         out-of-range entry, so callers can fall back to the previous table.
     */
    public static List<Point> parse(String spec) {
        if (spec == null || spec.isBlank())
            throw new IllegalArgumentException("empty overspeed table");

        List<Point> parsed = new ArrayList<>();
        for (String rawEntry : spec.split(",")) {
            String entry = rawEntry.trim();
            if (entry.isEmpty())
                continue;
            int separator = entry.indexOf(':');
            if (separator <= 0 || separator == entry.length() - 1)
                throw new IllegalArgumentException("malformed entry: " + entry);

            double ratio;
            double multiplier;
            try {
                ratio = Double.parseDouble(entry.substring(0, separator).trim());
                multiplier = Double.parseDouble(entry.substring(separator + 1).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("malformed entry: " + entry, e);
            }

            if (!(ratio > 0) || !(multiplier >= 1) || !Double.isFinite(ratio) || !Double.isFinite(multiplier))
                throw new IllegalArgumentException("out of range entry: " + entry);

            parsed.add(new Point(ratio, multiplier));
        }

        if (parsed.isEmpty())
            throw new IllegalArgumentException("no usable entries in overspeed table");

        parsed.sort((a, b) -> Double.compare(a.ratio(), b.ratio()));
        return List.copyOf(parsed);
    }

    /** Step lookup: the multiplier of the highest breakpoint that {@code ratio} has reached. */
    public static double step(List<Point> points, double ratio, double cap) {
        double clampedCap = Math.max(1, cap);
        double result = 1;
        for (Point point : points) {
            if (ratio < point.ratio())
                break;
            result = point.multiplier();
        }
        return Math.max(1, Math.min(clampedCap, result));
    }

    /** Convenience lookup using the currently active table. */
    public static double multiplier(double rpm, double saturationRpm, double cap) {
        if (!(saturationRpm > 0))
            return 1;
        return step(active, Math.abs(rpm) / saturationRpm, cap);
    }

    /**
     * Number of batches this machine runs on the given game tick for the given multiplier.

     * <p>Fractional multipliers are spread deterministically with a running difference of floors
     * instead of a stored residue: the total number of batches executed over ticks
     * {@code 0..tick} is exactly {@code floor(multiplier * (tick + 1))}, so a 2.5x machine alternates
     * two and three batches and averages exactly 2.5. Nothing has to be persisted or synced.
     */
    public static int batches(double multiplier, long tick) {
        if (!(multiplier > 1) || tick < 0)
            return 1;
        long reached = (long) Math.floor(multiplier * (tick + 1));
        long previous = (long) Math.floor(multiplier * tick);
        return (int) Math.max(1, reached - previous);
    }


    /**
     * Replaces the active table. A malformed specification is reported through {@code warn} and the
     * previous table stays in place, matching the documented fallback behaviour.
     */
    public static void reload(String spec, Consumer<String> warn) {
        try {
            active = List.copyOf(parse(spec));
        } catch (IllegalArgumentException e) {
            warn.accept("Invalid overspeed step table, keeping the previous one: " + e.getMessage());
        }
    }

    /** Restores the built-in table; used by reload handling and tests. */
    public static List<Point> defaults() {
        return DEFAULT;
    }

    public static List<Point> active() {
        return active;
    }
}
