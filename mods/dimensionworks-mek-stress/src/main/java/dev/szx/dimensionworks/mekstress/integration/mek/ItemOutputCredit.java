package dev.szx.dimensionworks.mekstress.integration.mek;

import dev.szx.dimensionworks.mekstress.core.FractionalOutputLedger;
import dev.szx.dimensionworks.mekstress.core.MachineWorkContext;
import mekanism.api.inventory.IInventorySlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Fractional item yield belongs to one output slot and one item including its NBT. */
public final class ItemOutputCredit {
    private static final FractionalOutputLedger LEDGER = new FractionalOutputLedger();
    private record Product(Item item, CompoundTag tag) {}
    private ItemOutputCredit() {}

    private static Product product(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return new Product(stack.getItem(), tag == null ? null : tag.copy());
    }

    public static long preview(IInventorySlot slot, ItemStack stack, long base, int operations) {
        return LEDGER.preview(slot, product(stack), base, operations, MachineWorkContext.multiplier());
    }

    public static long commit(IInventorySlot slot, ItemStack stack, long base, int operations) {
        return LEDGER.commit(slot, product(stack), base, operations, MachineWorkContext.multiplier());
    }

    public static int maxOperations(IInventorySlot slot, ItemStack stack, long room) {
        return LEDGER.maxOperations(slot, product(stack), stack.getCount(), room, MachineWorkContext.multiplier());
    }
}
