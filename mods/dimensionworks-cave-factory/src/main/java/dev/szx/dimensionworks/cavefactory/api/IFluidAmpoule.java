package dev.szx.dimensionworks.cavefactory.api;

import net.minecraft.world.item.ItemStack;

/** Operations supported by Cave Factory fluid ampoules without exposing their implementation. */
public interface IFluidAmpoule {
    boolean hasStoredFluid(ItemStack stack);

    AmpouleExpiry.Settlement observe(
        ItemStack stack,
        String observedDimension,
        long nowMillis,
        long windowMillis
    );

    AmpouleExpiry.Settlement settle(ItemStack stack, long nowMillis);

    boolean mergeable(ItemStack first, ItemStack second);

    ItemStack prepareForChannelStorage(ItemStack stack, long nowMillis);
}
