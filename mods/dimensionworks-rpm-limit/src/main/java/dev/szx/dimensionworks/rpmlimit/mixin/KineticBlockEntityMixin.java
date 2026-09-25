package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityMixin {

    @Shadow
    protected KineticEffectHandler effects;

    @Shadow
    protected boolean overStressed;

    @Shadow
    protected float capacity;

    @Shadow
    protected float stress;

    @Shadow
    private int networkSize;

    /**
     * Caps the rotation produced by the block that drives the network.
     *
     * <p>Everything in {@code RotationPropagator.propagateNewSource} writes speeds through this
     * method too. Clamping those writes leaves a block permanently slower than the neighbour that
     * feeds it, and the propagator answers that state by recursing into itself forever, so only a
     * generator with no upstream source is allowed to be capped.
     */
    @ModifyVariable(method = "setSpeed", at = @At("HEAD"), argsOnly = true, remap = false)
    private float dimensionworks$limitSourceSpeed(float speed) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        if (!self.isSource() || self.hasSource())
            return speed;
        return RpmLimitManager.clamp(self, speed);
    }

    /**
     * Replaces Create's network update with the same calculation plus the mismatch state.
     *
     * <p>Changing Create's value after it runs would make overstress flicker between false and
     * true on every sync. Create counts those speed changes and may eventually destroy a block
     * as a safety measure, so the mismatch has to be part of the single calculation instead.
     */
    @Inject(method = "updateFromNetwork", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$updateNetworkState(float maxStress, float currentStress, int size,
                                                   CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        self.networkDirty = false;
        capacity = maxStress;
        stress = currentStress;
        networkSize = size;

        boolean shouldStall = RpmLimitManager.isSpeedMismatch(self)
            || (maxStress < currentStress && StressImpact.isEnabled());
        self.setChanged();

        if (shouldStall != overStressed) {
            float prevSpeed = self.getSpeed();
            overStressed = shouldStall;
            self.onSpeedChanged(prevSpeed);
            self.sendData();
        }
        ci.cancel();
    }

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void dimensionworks$writeMismatch(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        compound.putBoolean("DimensionWorksSpeedMismatch",
            RpmLimitManager.isSpeedMismatch((KineticBlockEntity) (Object) this));
    }

    /**
     * The client derives {@code overStressed} from the synced stress values, so the mismatch flag
     * has to ride along and force the same state client side, otherwise the goggles would show
     * stress overload instead of the real cause.
     */
    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void dimensionworks$readMismatch(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        boolean mismatch = compound.getBoolean("DimensionWorksSpeedMismatch");
        RpmLimitManager.setSpeedMismatch(self, mismatch);

        if (self instanceof SpeedControllerBlockEntity controller && controller.targetSpeed != null) {
            int current = controller.targetSpeed.getValue();
            int clamped = RpmLimitManager.clampInt(controller, current);
            if (clamped != current)
                controller.targetSpeed.value = clamped;
        }

        if (!clientPacket || !mismatch || overStressed)
            return;
        overStressed = true;
        effects.triggerOverStressedEffect();
    }

    /**
     * Same spot and same styling as Create's own overstress line in {@code addToTooltip}, with the
     * wording swapped. Returning early keeps any other message off the tooltip.
     */
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$mismatchTooltip(List<Component> tooltip, boolean isPlayerSneaking,
                                                CallbackInfoReturnable<Boolean> cir) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        if (!RpmLimitManager.isSpeedMismatch(self))
            return;
        CreateLang.translate("gui.stressometer.speed_mismatch")
            .style(ChatFormatting.GOLD)
            .forGoggles(tooltip);
        cir.setReturnValue(true);
    }
}
