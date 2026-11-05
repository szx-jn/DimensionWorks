package dev.szx.dimensionworks.mekstress.blockentity;

import dev.szx.dimensionworks.mekstress.memory.ChannelStockSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Shared channel inventory view. A remote endpoint can extract; local-origin stock stays local. */
final class CrossDimensionItemHandler implements IItemHandler {
    private final CrossDimensionTransportBlockEntity endpoint;
    CrossDimensionItemHandler(CrossDimensionTransportBlockEntity endpoint) { this.endpoint = endpoint; }

    private ChannelStockSavedData data() {
        return endpoint.getLevel() instanceof ServerLevel server && endpoint.channel() > 0
            ? ChannelStockSavedData.get(server) : null;
    }

    @Override public int getSlots() {
        ChannelStockSavedData data = data();
        return data == null ? 0 : data.itemSlots(endpoint.channel());
    }

    @Override public ItemStack getStackInSlot(int slot) {
        ChannelStockSavedData data = data();
        if (data == null || !endpoint.canExportItems()) return ItemStack.EMPTY;
        return data.itemAt(endpoint.channel(), endpoint.dimensionId(), slot);
    }

    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= getSlots() || !endpoint.canImportItems()) return stack;
        ChannelStockSavedData data = data();
        if (data == null) return stack;
        int accepted = data.insertItems(endpoint.channel(), endpoint.dimensionId(), stack, simulate);
        if (accepted <= 0) return stack;
        ItemStack remainder = stack.copy();
        remainder.shrink(accepted);
        return remainder;
    }

    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= getSlots() || !endpoint.canExportItems()) return ItemStack.EMPTY;
        ChannelStockSavedData data = data();
        return data == null ? ItemStack.EMPTY
            : data.extractItems(endpoint.channel(), endpoint.dimensionId(), slot, amount, simulate);
    }

    @Override public int getSlotLimit(int slot) { return 64; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < getSlots() && endpoint.canImportItems();
    }
}
