package dev.szx.dimensionworks.cavefactory.logic;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Rejects malformed saved inventories rather than silently replacing their contents with empty stacks. */
public final class FactoryStorage {
    private FactoryStorage() {}

    public static void readItems(HolderLookup.Provider registries, CompoundTag tag, ItemStackHandler inventory) {
        if (tag.getInt("Size") != inventory.getSlots()) throw new IllegalArgumentException("Factory inventory size changed");
        Tag raw = tag.get("Items");
        if (!(raw instanceof ListTag entries) || !entries.isEmpty() && entries.getElementType() != Tag.TAG_COMPOUND) {
            throw new IllegalArgumentException("Factory inventory Items must be compound records");
        }
        ItemStack[] stacks = new ItemStack[inventory.getSlots()];
        java.util.Arrays.fill(stacks, ItemStack.EMPTY);
        boolean[] seen = new boolean[stacks.length];
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= stacks.length || seen[slot]) throw new IllegalArgumentException("Invalid factory slot " + slot);
            seen[slot] = true;
            stacks[slot] = ItemStack.parse(registries, entry).orElseThrow(() -> new IllegalArgumentException("Invalid saved factory item"));
        }
        for (int slot = 0; slot < stacks.length; slot++) inventory.setStackInSlot(slot, stacks[slot]);
    }

    public static void readFluid(HolderLookup.Provider registries, CompoundTag tag, FluidTank tank) {
        FluidStack fluid = tag.isEmpty() ? FluidStack.EMPTY : FluidStack.parse(registries, tag.get("Fluid"))
            .orElseThrow(() -> new IllegalArgumentException("Invalid saved factory fluid"));
        if (fluid.getAmount() > tank.getCapacity()) throw new IllegalArgumentException("Saved factory fluid exceeds tank capacity");
        tank.setFluid(fluid);
    }
}
