package dev.szx.dimensionworks.mekstress.api;

/** Mixin bridge exposing the validated export state of an ME Gearbox. */
public interface IMEGearboxExportState {
    boolean dimensionworks$isExportRunning();

    int dimensionworks$getExportOutputRpm();

    long dimensionworks$getExportChargedSu();
}
