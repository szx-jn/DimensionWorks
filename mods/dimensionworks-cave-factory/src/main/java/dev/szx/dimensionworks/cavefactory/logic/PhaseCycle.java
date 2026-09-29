package dev.szx.dimensionworks.cavefactory.logic;

public final class PhaseCycle {
    private FactoryPhase phase;
    private int progress;
    private final int batchesPerPhase;

    public PhaseCycle(FactoryPhase phase, int progress, int batchesPerPhase) {
        this.phase = phase == null ? FactoryPhase.PHASE_A : phase;
        this.batchesPerPhase = Math.max(1, batchesPerPhase);
        this.progress = clampProgress(progress);
    }

    public static PhaseCycle fromState(FactoryPhase phase, int progress, int batchesPerPhase) {
        return new PhaseCycle(phase, progress, batchesPerPhase);
    }

    public FactoryPhase phase() {
        return phase;
    }

    public int progress() {
        return progress;
    }

    public int batchesPerPhase() {
        return batchesPerPhase;
    }

    public boolean recordSuccess(boolean redstoneLocked) {
        if (redstoneLocked) {
            return false;
        }

        progress++;
        if (progress < batchesPerPhase) {
            return false;
        }

        phase = phase.opposite();
        progress = 0;
        return true;
    }

    private int clampProgress(int value) {
        return Math.max(0, Math.min(batchesPerPhase - 1, value));
    }
}
