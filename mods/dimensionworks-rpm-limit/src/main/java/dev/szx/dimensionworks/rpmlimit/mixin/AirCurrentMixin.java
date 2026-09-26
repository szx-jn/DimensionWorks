package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AirCurrent.class, remap = false)
public abstract class AirCurrentMixin {

    @Inject(method = "tickAffectedHandlers", at = @At("TAIL"), remap = false)
    private void dimensionworks$overspeedFanHandlers(CallbackInfo ci) {
        OverspeedBonus.fanExtraHandlerPasses((AirCurrent) (Object) this);
    }
}
