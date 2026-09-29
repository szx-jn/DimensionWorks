package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.core.StressContainerOwner;
import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.ISidedStrictEnergyHandler;
import mekanism.common.capabilities.resolver.manager.EnergyHandlerManager;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnergyHandlerManager.class, remap = false)
public abstract class EnergyHandlerManagerMixin {

    @Shadow
    @Final
    private ISidedStrictEnergyHandler baseHandler;

    @Inject(method = "getContainers", at = @At("RETURN"), remap = false)
    private void dimensionworks$associateContainers(@Nullable Direction side,
                                                   CallbackInfoReturnable<List<IEnergyContainer>> cir) {
        if (!(baseHandler instanceof TileEntityMekanism tile)) {
            return;
        }
        for (IEnergyContainer container : cir.getReturnValue()) {
            if (container instanceof StressContainerOwner owner) {
                owner.dimensionworks$setOwner(tile);
            }
        }
    }
}
