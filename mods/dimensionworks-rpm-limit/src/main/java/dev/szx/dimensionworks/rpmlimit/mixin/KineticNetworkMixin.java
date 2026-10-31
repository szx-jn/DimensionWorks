package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds cached network accounting without changing Create's rotation-propagation graph.
 * Natural capacity is cached before the existing per-owner quota is applied.
 */
@Mixin(value = KineticNetwork.class, remap = false)
public abstract class KineticNetworkMixin {

    @Inject(method = "calculateCapacity", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$useCachedCapacity(CallbackInfoReturnable<Float> cir) {
        KineticNetwork self = (KineticNetwork) (Object) this;
        Float cached = KineticNetworkOptimizer.cachedCapacity(self);
        if (cached != null) {
            cir.setReturnValue(CreateCapacityRegistry.limitNaturalCapacity(self, cached));
        }
    }

    @Inject(method = "calculateCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$limitOwnerAggregate(CallbackInfoReturnable<Float> cir) {
        KineticNetwork self = (KineticNetwork) (Object) this;
        float naturalCapacity = cir.getReturnValueF();
        KineticNetworkOptimizer.finishCapacityRebuild(self, naturalCapacity);
        cir.setReturnValue(CreateCapacityRegistry.limitNaturalCapacity(self, naturalCapacity));
    }

    @Inject(method = "getActualStressOf", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$limitEffectiveStress(KineticBlockEntity be,
                                                     CallbackInfoReturnable<Float> cir) {
        float raw = be.getTheoreticalSpeed();
        float scale = RpmLimitManager.effectiveScale(be, raw);
        float multiplier = OverspeedBonus.stressMultiplier(be);
        cir.setReturnValue(cir.getReturnValueF() * scale * multiplier);
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

    @Inject(
        method = "updateCapacityFor",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/KineticNetwork;updateCapacity()V",
            shift = At.Shift.BEFORE
        ),
        remap = false
    )
    private void dimensionworks$updateCapacityIncrementally(KineticBlockEntity be, float capacity,
                                                            CallbackInfo ci) {
        // Create has already written the new base capacity into sources at this point.
        KineticNetworkOptimizer.updateCapacityContribution((KineticNetwork) (Object) this, be);
    }

    @Inject(
        method = "updateStressFor",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/KineticNetwork;updateStress()V",
            shift = At.Shift.BEFORE
        ),
        remap = false
    )
    private void dimensionworks$updateStressIncrementally(KineticBlockEntity be, float stress,
                                                          CallbackInfo ci) {
        // Create has already written the new base stress into members at this point.
        KineticNetworkOptimizer.updateStressContribution((KineticNetwork) (Object) this, be);
    }

    @Inject(method = "updateCapacity", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$batchCapacityRefresh(CallbackInfo ci) {
        if (KineticNetworkOptimizer.isBatching()) {
            KineticNetworkOptimizer.queueNetwork((KineticNetwork) (Object) this);
            ci.cancel();
        }
    }

    @Inject(method = "updateStress", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$batchStressRefresh(CallbackInfo ci) {
        if (KineticNetworkOptimizer.isBatching()) {
            KineticNetworkOptimizer.queueNetwork((KineticNetwork) (Object) this);
            ci.cancel();
        }
    }

    @Inject(method = "updateNetwork", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$batchFullRefresh(CallbackInfo ci) {
        if (KineticNetworkOptimizer.isBatching()) {
            KineticNetworkOptimizer.queueNetwork((KineticNetwork) (Object) this);
            ci.cancel();
        }
    }

    @Redirect(
        method = "updateNetwork",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/KineticNetwork;sync()V"),
        remap = false
    )
    private void dimensionworks$trackLoadRefreshSync(KineticNetwork network) {
        KineticNetworkOptimizer.syncDuringNetworkRefresh(network);
    }

    @Inject(method = "updateNetwork", at = @At("TAIL"), remap = false)
    private void dimensionworks$finishLoadRefresh(CallbackInfo ci) {
        KineticNetworkOptimizer.finishLoadedNetworkRefresh((KineticNetwork) (Object) this);
    }

    @Inject(method = "add", at = @At("RETURN"), remap = false)
    private void dimensionworks$invalidateAfterAdd(KineticBlockEntity be, CallbackInfo ci) {
        KineticNetworkOptimizer.onTopologyChanged((KineticNetwork) (Object) this);
    }

    @Inject(method = "addSilently", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$invalidateAfterSilentAdd(KineticBlockEntity be, float lastCapacity,
                                                         float lastStress, CallbackInfo ci) {
        dev.szx.dimensionworks.rpmlimit.optimizer.KineticPersistence.restore((KineticNetwork) (Object) this, be);
        ci.cancel();
    }

    @Inject(method = "remove", at = @At("RETURN"), remap = false)
    private void dimensionworks$invalidateAfterRemove(KineticBlockEntity be, CallbackInfo ci) {
        KineticNetworkOptimizer.onTopologyChanged((KineticNetwork) (Object) this);
    }

    @Inject(method = "initFromTE", at = @At("HEAD"), remap = false)
    private void dimensionworks$invalidateBeforeLoad(float maxStress, float currentStress, int members,
                                                     CallbackInfo ci) {
        KineticNetworkOptimizer.invalidate((KineticNetwork) (Object) this);
    }
}
