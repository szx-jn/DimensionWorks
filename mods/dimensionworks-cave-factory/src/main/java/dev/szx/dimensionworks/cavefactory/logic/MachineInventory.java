package dev.szx.dimensionworks.cavefactory.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface MachineInventory {
    int ITEM_INPUT_START = 0;
    int ITEM_OUTPUT_START = 4;
    int RESIDUE_SLOT = 8;
    int EMPTY_AMPOULE_SLOT = 9;
    int FULL_AMPOULE_SLOT = 10;
    int NUMERIC_MODULE_SLOT = 11;
    int MECHANISM_MODULE_SLOT = 12;
    int MENU_SLOTS = 13;

    ItemStack getStack(int slot);

    void setStack(int slot, ItemStack stack);

    default boolean isItemInput(int slot) {
        return slot >= ITEM_INPUT_START && slot < ITEM_OUTPUT_START;
    }

    default boolean isItemOutput(int slot) {
        return slot >= ITEM_OUTPUT_START && slot < RESIDUE_SLOT;
    }

    default boolean isModuleSlot(int slot) {
        return slot == NUMERIC_MODULE_SLOT || slot == MECHANISM_MODULE_SLOT;
    }

    default boolean isProcessSlot(int slot) {
        return isItemInput(slot) || isItemOutput(slot) || slot == RESIDUE_SLOT;
    }

    int fluidInputCapacity();

    int fluidOutputCapacity();

    FluidStack fluidInput();

    FluidStack fluidOutput();

    BlockPos machinePos();

    boolean isMachineValid();
}
