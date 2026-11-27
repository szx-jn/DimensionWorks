package dev.szx.dimensionworks.cavefactory.blockentity;

import dev.szx.dimensionworks.cavefactory.logic.FactoryPhase;
import dev.szx.dimensionworks.cavefactory.logic.FactoryStorage;
import dev.szx.dimensionworks.cavefactory.logic.PhaseCycle;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Encodes the established machine NBT contract; key names and tag types are save-format API. */
final class MachinePersistenceCodec {
    private static final String OWNER_KEY = "Owner";
    private static final String OWNER_NAME_KEY = "OwnerName";
    private static final String PHASE_KEY = "FactoryPhase";
    private static final String PHASE_PROGRESS_KEY = "PhaseProgress";
    private static final String INPUT_ITEMS_KEY = "InputItems";
    private static final String OUTPUT_ITEMS_KEY = "OutputItems";
    private static final String RESIDUE_KEY = "Residue";
    private static final String AMPOULE_IN_KEY = "AmpouleInput";
    private static final String AMPOULE_OUT_KEY = "AmpouleOutput";
    private static final String MODULES_KEY = "Modules";
    private static final String INPUT_FLUID_KEY = "InputFluid";
    private static final String OUTPUT_FLUID_KEY = "OutputFluid";
    private static final String PORT_MODES_KEY = "PortModes";

    record MachineState(@Nullable UUID owner, String ownerName, PhaseCycle phaseCycle) {}

    private MachinePersistenceCodec() {}

    static void write(
        CompoundTag tag,
        HolderLookup.Provider registries,
        @Nullable UUID owner,
        String ownerName,
        PhaseCycle phaseCycle,
        ItemStackHandler inputItems,
        ItemStackHandler outputItems,
        ItemStackHandler residue,
        ItemStackHandler ampouleInput,
        ItemStackHandler ampouleOutput,
        ItemStackHandler modules,
        FluidTank inputTank,
        FluidTank outputTank,
        MachinePortController portController
    ) {
        tag.putString(OWNER_KEY, owner == null ? "" : owner.toString());
        tag.putString(OWNER_NAME_KEY, ownerName);
        tag.putString(PHASE_KEY, phaseCycle.phase().name());
        tag.putInt(PHASE_PROGRESS_KEY, phaseCycle.progress());
        tag.put(INPUT_ITEMS_KEY, inputItems.serializeNBT(registries));
        tag.put(OUTPUT_ITEMS_KEY, outputItems.serializeNBT(registries));
        tag.put(RESIDUE_KEY, residue.serializeNBT(registries));
        tag.put(AMPOULE_IN_KEY, ampouleInput.serializeNBT(registries));
        tag.put(AMPOULE_OUT_KEY, ampouleOutput.serializeNBT(registries));
        tag.put(MODULES_KEY, modules.serializeNBT(registries));
        tag.put(INPUT_FLUID_KEY, inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put(OUTPUT_FLUID_KEY, outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put(PORT_MODES_KEY, portController.writeNbt());
    }

    static MachineState readState(CompoundTag tag, int batchesPerPhase) {
        UUID owner = tag.getString(OWNER_KEY).isBlank() ? null : UUID.fromString(tag.getString(OWNER_KEY));
        String ownerName = tag.getString(OWNER_NAME_KEY);
        FactoryPhase phase = tag.contains(PHASE_KEY)
            ? FactoryPhase.valueOf(tag.getString(PHASE_KEY))
            : FactoryPhase.PHASE_A;
        PhaseCycle phaseCycle = PhaseCycle.fromState(
            phase,
            tag.getInt(PHASE_PROGRESS_KEY),
            batchesPerPhase
        );
        return new MachineState(owner, ownerName, phaseCycle);
    }

    static void readStorage(
        CompoundTag tag,
        HolderLookup.Provider registries,
        ItemStackHandler inputItems,
        ItemStackHandler outputItems,
        ItemStackHandler residue,
        ItemStackHandler ampouleInput,
        ItemStackHandler ampouleOutput,
        ItemStackHandler modules,
        FluidTank inputTank,
        FluidTank outputTank,
        Runnable refreshFluidCapacity,
        MachinePortController portController
    ) {
        if (tag.contains(INPUT_ITEMS_KEY)) FactoryStorage.readItems(registries, tag.getCompound(INPUT_ITEMS_KEY), inputItems);
        if (tag.contains(OUTPUT_ITEMS_KEY)) FactoryStorage.readItems(registries, tag.getCompound(OUTPUT_ITEMS_KEY), outputItems);
        if (tag.contains(RESIDUE_KEY)) FactoryStorage.readItems(registries, tag.getCompound(RESIDUE_KEY), residue);
        if (tag.contains(AMPOULE_IN_KEY)) FactoryStorage.readItems(registries, tag.getCompound(AMPOULE_IN_KEY), ampouleInput);
        if (tag.contains(AMPOULE_OUT_KEY)) FactoryStorage.readItems(registries, tag.getCompound(AMPOULE_OUT_KEY), ampouleOutput);
        if (tag.contains(MODULES_KEY)) FactoryStorage.readItems(registries, tag.getCompound(MODULES_KEY), modules);
        refreshFluidCapacity.run();
        if (tag.contains(INPUT_FLUID_KEY)) FactoryStorage.readFluid(registries, tag.getCompound(INPUT_FLUID_KEY), inputTank);
        if (tag.contains(OUTPUT_FLUID_KEY)) FactoryStorage.readFluid(registries, tag.getCompound(OUTPUT_FLUID_KEY), outputTank);
        portController.readNbt(tag.getCompound(PORT_MODES_KEY));
    }
}
