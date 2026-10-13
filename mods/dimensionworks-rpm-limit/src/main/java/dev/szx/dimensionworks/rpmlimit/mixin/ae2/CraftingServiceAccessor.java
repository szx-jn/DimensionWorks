package dev.szx.dimensionworks.rpmlimit.mixin.ae2;

import appeng.api.networking.IGrid;
import appeng.me.service.CraftingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CraftingService.class, remap = false)
public interface CraftingServiceAccessor {

    @Accessor("grid")
    IGrid dimensionworks$getGrid();
}
