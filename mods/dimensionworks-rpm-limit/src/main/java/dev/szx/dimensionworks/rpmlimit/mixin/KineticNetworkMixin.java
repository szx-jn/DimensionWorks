package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps stress accounting in the same effective RPM space that machines actually use.
 *
 * <p>Propagation still keeps Create's raw source graph untouched. Only the stress/capacity totals
 * are scaled when a player limit makes a member or generator run slower than its theoretical
 * network speed.
 */
@Mixin(value = KineticNetwork.class, remap = false)
public abstract class KineticNetworkMixin {

    @Inject(method = "getActualStressOf", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$limitEffectiveStress(KineticBlockEntity be,
                                                     CallbackInfoReturnable<Float> cir) {
        float raw = be.getTheoreticalSpeed();
        float scale = RpmLimitManager.effectiveScale(be, raw);
        if (scale < 1)
            cir.setReturnValue(cir.getReturnValueF() * scale);
    }

    @Inject(method = "getActualCapacityOf", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$limitEffectiveCapacity(KineticBlockEntity be,
                                                       CallbackInfoReturnable<Float> cir) {
        float raw = be.getGeneratedSpeed();
        float scale = RpmLimitManager.effectiveScale(be, raw);
        if (scale < 1)
            cir.setReturnValue(cir.getReturnValueF() * scale);
    }
}
