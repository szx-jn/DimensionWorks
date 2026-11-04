package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Combines the multiple stress/capacity mutations made by one generator update into one network
 * calculation and one synchronization.
 */
@Mixin(value = GeneratingKineticBlockEntity.class, remap = false)
public abstract class GeneratingKineticBlockEntityMixin {

    @Inject(method = "updateGeneratedRotation", at = @At("HEAD"), remap = false)
    private void dimensionworks$beginKineticBatch(CallbackInfo ci) {
        KineticNetworkOptimizer.beginGeneratorBatch(
            (com.simibubi.create.content.kinetics.base.KineticBlockEntity) (Object) this);
    }

    @Inject(method = "updateGeneratedRotation", at = @At("RETURN"), remap = false)
    private void dimensionworks$finishKineticBatch(CallbackInfo ci) {
        KineticNetworkOptimizer.endBatch();
    }
}
