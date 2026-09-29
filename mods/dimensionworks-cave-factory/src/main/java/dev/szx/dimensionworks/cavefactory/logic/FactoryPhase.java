package dev.szx.dimensionworks.cavefactory.logic;

public enum FactoryPhase {
    PHASE_A,
    PHASE_B;

    public FactoryPhase opposite() {
        return this == PHASE_A ? PHASE_B : PHASE_A;
    }
}
