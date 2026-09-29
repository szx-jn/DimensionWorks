package dev.szx.dimensionworks.mekstress.mixin.mek;

import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TileEntityMekanism.class, remap = false)
public interface TileEntityMekanismInvoker {

    @Invoker("onUpdateServer")
    void dimensionworks$onUpdateServer();
}
