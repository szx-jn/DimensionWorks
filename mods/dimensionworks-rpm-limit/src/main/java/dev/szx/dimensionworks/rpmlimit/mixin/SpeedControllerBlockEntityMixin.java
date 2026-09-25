package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create's speed controller is not a regular kinetic source: it writes its configured target
 * speed through custom rotation propagation instead of {@code setSpeed} on an
 * {@code isSource()} generator. Its target must therefore be clamped separately.
 */
@Mixin(value = SpeedControllerBlockEntity.class, remap = false)
public abstract class SpeedControllerBlockEntityMixin {

    @Inject(method = "updateTargetRotation", at = @At("HEAD"), remap = false)
    private void dimensionworks$clampTargetSpeed(CallbackInfo ci) {
        SpeedControllerBlockEntity self = (SpeedControllerBlockEntity) (Object) this;
        if (self.targetSpeed == null)
            return;

        int current = self.targetSpeed.getValue();
        int clamped = RpmLimitManager.clampInt(self, current);
        if (clamped == current)
            return;

        self.targetSpeed.value = clamped;
        self.setChanged();
    }
}
