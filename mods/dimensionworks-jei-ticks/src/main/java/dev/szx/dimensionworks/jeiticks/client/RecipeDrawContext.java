package dev.szx.dimensionworks.jeiticks.client;

/** Scoped to JEI calls, including widgets and extras; never active in machine GUIs. */
public final class RecipeDrawContext {
    private static final ThreadLocal<Long> TICKS = new ThreadLocal<>();

    private RecipeDrawContext() {}

    public static long ticks() {
        Long ticks = TICKS.get();
        return ticks == null ? 0 : ticks;
    }

    public static void draw(long ticks, Runnable draw) {
        Long previous = TICKS.get();
        TICKS.set(ticks);
        try {
            draw.run();
        } finally {
            if (previous == null) {
                TICKS.remove();
            } else {
                TICKS.set(previous);
            }
        }
    }
}
