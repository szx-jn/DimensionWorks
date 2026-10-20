package dev.szx.dimensionworks.cavefactory.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AtomicOutputPlanner {
    private AtomicOutputPlanner() {}

    public static boolean canFit(int required, int present, int capacity) {
        long needed = Math.max(0, required);
        long existing = Math.max(0, present);
        long limit = Math.max(0, capacity);
        return existing + needed <= limit;
    }

    /** Inventory-neutral planning records, kept independent of Minecraft bootstrap for unit tests. */
    public record SlotState(Object key, int count, int slotLimit, int maxStackSize) {}
    public record OutputState(Object key, int count, int maxStackSize) {}

    private record ItemIdentity(Item item, CompoundTag tag) {}

    /** Simulates all output stacks in slot order without mutating the supplied inventory. */
    public static boolean canFitOutputs(IItemHandler inventory, List<ItemStack> outputs) {
        if (inventory == null || outputs == null) {
            return false;
        }

        List<SlotState> slots = new ArrayList<>(inventory.getSlots());
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            Object key = keyOf(stack);
            slots.add(new SlotState(
                key,
                stack.getCount(),
                inventory.getSlotLimit(slot),
                stack.getMaxStackSize()
            ));
        }

        List<OutputState> requested = new ArrayList<>(outputs.size());
        for (ItemStack output : outputs) {
            if (output == null || output.isEmpty()) {
                continue;
            }
            requested.add(new OutputState(keyOf(output), output.getCount(), output.getMaxStackSize()));
        }
        return canFitStacks(slots, requested);
    }

    private static Object keyOf(ItemStack stack) {
        return stack.isEmpty() ? null : new ItemIdentity(
            stack.getItem(),
            stack.getTag() == null ? null : stack.getTag().copy()
        );
    }

    /** Plans output placement while respecting item/tag identity and both slot and item stack limits. */
    public static boolean canFitStacks(List<SlotState> originalSlots, List<OutputState> outputs) {
        if (originalSlots == null || outputs == null) {
            return false;
        }
        List<SlotState> slots = new ArrayList<>(originalSlots);

        for (OutputState output : outputs) {
            if (output == null || output.key() == null || output.count() <= 0) {
                continue;
            }
            int remaining = output.count();

            for (int slot = 0; slot < slots.size() && remaining > 0; slot++) {
                SlotState existing = slots.get(slot);
                if (existing.key() == null || !Objects.equals(existing.key(), output.key())) {
                    continue;
                }
                int capacity = Math.min(Math.max(0, existing.slotLimit()), Math.max(0, existing.maxStackSize()));
                int room = Math.max(0, capacity - Math.max(0, existing.count()));
                int moved = Math.min(room, remaining);
                if (moved > 0) {
                    slots.set(slot, new SlotState(
                        existing.key(), existing.count() + moved, existing.slotLimit(), existing.maxStackSize()));
                    remaining -= moved;
                }
            }

            for (int slot = 0; slot < slots.size() && remaining > 0; slot++) {
                SlotState existing = slots.get(slot);
                if (existing.key() != null && existing.count() > 0) {
                    continue;
                }
                int capacity = Math.min(Math.max(0, existing.slotLimit()), Math.max(0, output.maxStackSize()));
                int moved = Math.min(capacity, remaining);
                if (moved <= 0) {
                    continue;
                }
                slots.set(slot, new SlotState(output.key(), moved, existing.slotLimit(), output.maxStackSize()));
                remaining -= moved;
            }

            if (remaining > 0) {
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
