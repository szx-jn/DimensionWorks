package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps stress accounting in the same effective RPM space that machines actually use and applies
 * the Gear Heart owner aggregate after Create has calculated natural network capacity.
 *
 * <p>Propagation still keeps Create's raw source graph untouched. The aggregate hook only scales
 * the returned number and never invokes calculateCapacity again.</p>
 */
@Mixin(value = KineticNetwork.class, remap = false)
public abstract class KineticNetworkMixin {

    @Inject(method = "getActualStressOf", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$limitEffectiveStress(KineticBlockEntity be,
                                                     CallbackInfoReturnable<Float> cir) {
        float raw = be.getTheoreticalSpeed();
        float scale = RpmLimitManager.effectiveScale(be, raw);
        if (scale < 1) {
            cir.setReturnValue(cir.getReturnValueF() * scale);
        }
    }

    @Inject(method = "getActualCapacityOf", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$limitEffectiveCapacity(KineticBlockEntity be,
                                                       CallbackInfoReturnable<Float> cir) {
        float raw = be.getGeneratedSpeed();
        float scale = RpmLimitManager.effectiveScale(be, raw);
        if (scale < 1) {
            cir.setReturnValue(cir.getReturnValueF() * scale);
        }
    }

    @Inject(method = "calculateCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$limitOwnerAggregate(CallbackInfoReturnable<Float> cir) {
        KineticNetwork self = (KineticNetwork) (Object) this;
        float naturalCapacity = cir.getReturnValueF();
        cir.setReturnValue(CreateCapacityRegistry.limitNaturalCapacity(self, naturalCapacity));
    }
}
