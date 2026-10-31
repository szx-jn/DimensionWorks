package dev.szx.dimensionworks.mekstress.mixin.draconic;

import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import dev.szx.dimensionworks.mekstress.reactor.ReactorPowerBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The original reactor state machine and catastrophic failure code always run first. */
@Mixin(value = TileReactorCore.class, remap = false)
public abstract class ReactorCoreMixin {
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void dimensionworks$runVirtualMechanicalEnergy(CallbackInfo ci) {
        ReactorPowerBridge.afterCoreTick((TileReactorCore) (Object) this);
    }
}
