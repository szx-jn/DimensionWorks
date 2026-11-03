package dev.szx.dimensionworks.mekstress.mixin.mekgenerators;

import dev.szx.dimensionworks.mekstress.integration.mekgenerators.GeneratorPowerBridge;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.generators.common.content.fusion.FusionReactorMultiblockData;
import mekanism.generators.common.content.turbine.TurbineMultiblockData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {TurbineMultiblockData.class, FusionReactorMultiblockData.class}, remap = false)
public abstract class MultiblockGenerationMixin {
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void dimensionworks$begin(Level world, CallbackInfoReturnable<Boolean> cir) {
        if (!world.isClientSide) GeneratorPowerBridge.beforeMultiblock((MultiblockData) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void dimensionworks$end(Level world, CallbackInfoReturnable<Boolean> cir) {
        if (!world.isClientSide) GeneratorPowerBridge.afterMultiblock((MultiblockData) (Object) this, world);
    }
}
