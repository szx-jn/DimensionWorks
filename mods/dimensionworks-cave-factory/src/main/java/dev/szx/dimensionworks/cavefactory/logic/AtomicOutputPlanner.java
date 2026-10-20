package dev.szx.dimensionworks.cavefactory.logic;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

public final class AtomicOutputPlanner {
    private AtomicOutputPlanner() {}

    public static boolean canFit(int required, int present, int capacity) {
        long needed = Math.max(0, required);
        long existing = Math.max(0, present);
        long limit = Math.max(0, capacity);
        return existing + needed <= limit;
    }

    /** Simulates all output stacks in slot order without mutating the supplied inventory. */
    public static boolean canFitOutputs(IItemHandler inventory, List<ItemStack> outputs) {
        if (inventory == null || outputs == null) {
            return false;
        }

        ItemStack[] simulated = new ItemStack[inventory.getSlots()];
        for (int slot = 0; slot < simulated.length; slot++) {
            simulated[slot] = inventory.getStackInSlot(slot).copy();
        }

        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty()) {
                continue;
            }
            ItemStack remaining = output.copy();

            for (int slot = 0; slot < simulated.length && !remaining.isEmpty(); slot++) {
                ItemStack existing = simulated[slot];
                if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, remaining)) {
                    continue;
                }
                int capacity = Math.min(inventory.getSlotLimit(slot), existing.getMaxStackSize());
                int room = Math.max(0, capacity - existing.getCount());
                int moved = Math.min(room, remaining.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
            }

            for (int slot = 0; slot < simulated.length && !remaining.isEmpty(); slot++) {
                if (!simulated[slot].isEmpty()) {
                    continue;
                }
                int capacity = Math.min(inventory.getSlotLimit(slot), remaining.getMaxStackSize());
                if (capacity <= 0) {
                    continue;
                }
                int moved = Math.min(capacity, remaining.getCount());
                simulated[slot] = remaining.copyWithCount(moved);
                remaining.shrink(moved);
            }

            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Checks whether {@code required} is no greater than {@code available}. */
    public static boolean allFit(int required, int available) {
        return Math.max(0, required) <= Math.max(0, available);
    }
}
