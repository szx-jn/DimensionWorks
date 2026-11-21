package dev.szx.dimensionworks.transfer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Optional bridge to Sophisticated Storage's native packed-state API. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity", remap = false)
public interface SophisticatedStoragePackedAccessor {
    @Invoker("setPacked")
    void dimensionworks$setPacked(boolean packed);
}
