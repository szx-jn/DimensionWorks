package dev.szx.dimensionworks.mekstress.memory;

/** Marks Create kinetic members backed by a Mekanism block instead of a real kinetic block entity. */
public interface IVirtualKineticMember extends dev.szx.dimensionworks.rpmlimit.api.IRuntimeKineticMember {
    boolean dimensionworks$isValid();
}
