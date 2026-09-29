package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes a failed saw operation consume its input without producing a result. */
@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class SawBlockEntityMixin {

    @Inject(method = "applyRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$failCursedSawOutput(CallbackInfo ci) {
        SawBlockEntity saw = (SawBlockEntity) (Object) this;
        if (GearHeartEffects.shouldFailOutput(saw)) {
            GearHeartEffects.failSawOutput(saw);
            ci.cancel();
        }
    }
}
