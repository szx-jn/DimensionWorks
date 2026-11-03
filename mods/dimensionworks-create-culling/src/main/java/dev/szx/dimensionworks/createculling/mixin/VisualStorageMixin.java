package dev.szx.dimensionworks.createculling.mixin;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;
import dev.engine_room.flywheel.impl.visualization.storage.Storage;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import dev.szx.dimensionworks.createculling.ClientOcclusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Storage.class, remap = false)
public abstract class VisualStorageMixin {
    @Inject(method = "framePlan", at = @At("RETURN"), cancellable = true)
    private void dimensionworks$appendOcclusion(CallbackInfoReturnable<Plan<DynamicVisual.Context>> cir) {
        if ((Object) this instanceof BlockEntityStorage storage) {
            cir.setReturnValue(cir.getReturnValue().then(SimplePlan.<DynamicVisual.Context>of(
                context -> ClientOcclusion.updateInstances(storage))));
        }
    }
}
