package dev.szx.dimensionworks.mekstress.core;

/** Thread-local production context applied at Mekanism's shared output boundary. */
public final class MachineWorkContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private MachineWorkContext() {
    }

    public static Scope push(double productionMultiplier) {
        State previous = CURRENT.get();
        double multiplier = Double.isFinite(productionMultiplier) && productionMultiplier > 1.0D
            ? productionMultiplier
            : 1.0D;
        CURRENT.set(new State(multiplier, previous));
        return new Scope(previous);
    }

    public static boolean scaling() {
        State state = CURRENT.get();
        return state != null && state.multiplier() > 1.0D;
    }

    public static boolean active() {
        return CURRENT.get() != null;
    }

    public static double multiplier() {
        State state = CURRENT.get();
        return state == null ? 1.0D : state.multiplier();
    }

    public static long scaleOutput(long baseAmount, int operations) {
        State state = CURRENT.get();
        double multiplier = state == null ? 1.0D : state.multiplier();
        return MachinePowerMath.scaledOutput(baseAmount, operations, multiplier);
    }

    public static int maxOutputOperations(long baseAmount, long availableRoom) {
        State state = CURRENT.get();
        double multiplier = state == null ? 1.0D : state.multiplier();
        return MachinePowerMath.maxOutputOperations(baseAmount, availableRoom, multiplier);
    }

    public static void pop() {
        State state = CURRENT.get();
        if (state == null) {
            return;
        }
        if (state.previous() == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(state.previous());
        }
    }

    private record State(double multiplier, State previous) {
    }

    public static final class Scope implements AutoCloseable {
        private final State previous;
        private boolean closed;

        private Scope(State previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
