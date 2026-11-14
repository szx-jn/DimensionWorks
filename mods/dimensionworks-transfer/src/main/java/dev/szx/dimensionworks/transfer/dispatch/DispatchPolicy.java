package dev.szx.dimensionworks.transfer.dispatch;

/** Server-side decision rules for a terminal request, independent of client rendering. */
public final class DispatchPolicy {
    private DispatchPolicy() {}

    public enum Decision {
        ALLOWED, WRONG_DIMENSION, NOT_OWNER, TOO_FAR, NOT_PACKED, LOCKED, UNAVAILABLE
    }

    public static Decision decide(boolean inTransfer, boolean owner, boolean inRange,
                                  boolean packed, boolean locked, boolean available) {
        if (!inTransfer) return Decision.WRONG_DIMENSION;
        if (!owner) return Decision.NOT_OWNER;
        if (!inRange) return Decision.TOO_FAR;
        if (!packed) return Decision.NOT_PACKED;
        if (locked) return Decision.LOCKED;
        if (!available) return Decision.UNAVAILABLE;
        return Decision.ALLOWED;
    }

    public static boolean routeAllowed(String route, boolean alexCavesUnlocked) {
        return switch (route) {
            case "overworld", "nether", "end", "twilight" -> true;
            default -> alexCavesUnlocked && AlexCavesDestination.fromRoute(route) != null;
        };
    }
}
