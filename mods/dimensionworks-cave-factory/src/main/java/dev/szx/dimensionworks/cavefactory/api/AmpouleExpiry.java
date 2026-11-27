package dev.szx.dimensionworks.cavefactory.api;

import net.minecraft.world.item.ItemStack;

public final class AmpouleExpiry {
    public record Settlement(ItemStack stack, ItemStack residue, boolean expired) {}

    private AmpouleExpiry() {}

    public static boolean isAmpoule(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IFluidAmpoule;
    }

    public static boolean isFilled(ItemStack stack) {
        return stack.getItem() instanceof IFluidAmpoule ampoule && ampoule.hasStoredFluid(stack);
    }

    public static Settlement observe(ItemStack stack, String observedDimension, long nowMillis, long windowMillis) {
        if (!(stack.getItem() instanceof IFluidAmpoule ampoule)) {
            return new Settlement(stack, ItemStack.EMPTY, false);
        }
        return ampoule.observe(stack, observedDimension, nowMillis, windowMillis);
    }

    public static Settlement settle(ItemStack stack, long nowMillis) {
        if (!(stack.getItem() instanceof IFluidAmpoule ampoule)) {
            return new Settlement(stack, ItemStack.EMPTY, false);
        }
        return ampoule.settle(stack, nowMillis);
    }

    public static boolean mergeable(ItemStack first, ItemStack second) {
        return first.getItem() instanceof IFluidAmpoule ampoule && ampoule.mergeable(first, second);
    }

    /**
     * Returns an empty stack when an expired ampoule cannot enter channel storage.
     * Non-ampoule stacks are copied unchanged.
     */
    public static ItemStack prepareForChannelStorage(ItemStack stack, long nowMillis) {
        if (!(stack.getItem() instanceof IFluidAmpoule ampoule)) {
            return stack.copy();
        }
        if (!ampoule.hasStoredFluid(stack)) {
            return stack.copy();
        }
        return ampoule.prepareForChannelStorage(stack, nowMillis);
    }
}
