package dev.szx.dimensionworks.transfer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/** Registry keys for {@code dw:transfer}. */
public final class WDKeys {
    public static final ResourceLocation TRANSFER_ID =
            ResourceLocation.fromNamespaceAndPath(WDTransfer.DIMENSION_NAMESPACE, "transfer");

    public static final ResourceKey<Level> TRANSFER_LEVEL =
            ResourceKey.create(Registries.DIMENSION, TRANSFER_ID);

    public static final ResourceKey<DimensionType> TRANSFER_DIMENSION_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, TRANSFER_ID);

    private WDKeys() {
    }
}
