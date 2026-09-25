package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Re-evaluates the whole network before Create pushes network state to its members.
 *
 * <p>{@code KineticNetwork.sync()} is the same place where Create recalculates and clears
 * overstress for every member, so hooking here gives the mismatch state the same automatic
 * recovery behaviour without destroying any blocks.
 */
@Mixin(value = KineticNetwork.class, remap = false)
public abstract class KineticNetworkMixin {

    @Inject(method = "sync", at = @At("HEAD"), remap = false)
    private void dimensionworks$evaluateSpeedMismatch(CallbackInfo ci) {
        RpmLimitManager.evaluateNetwork((KineticNetwork) (Object) this);
    }

    /**
     * Clears the flag on a member that leaves the network. Otherwise a disconnected block could
     * keep its old red mismatch state until it joins another network.
     */
    @Inject(method = "remove", at = @At("HEAD"), remap = false)
    private void dimensionworks$clearRemovedMismatch(KineticBlockEntity be, CallbackInfo ci) {
        RpmLimitManager.setSpeedMismatch(be, false);
    }
}
