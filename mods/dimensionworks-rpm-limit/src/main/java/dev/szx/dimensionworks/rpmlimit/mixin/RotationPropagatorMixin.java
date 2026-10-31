package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import dev.szx.dimensionworks.rpmlimit.controller.AdvancedControllerBlock;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {
    /**
     * Create compares this prediction to the stored speed before propagating a faster source.
     * setSpeed() already applies the recipient's RPM limit; use that same limit here so a
     * controller target above the limit cannot repeatedly re-enter the faster-source branch.
     * This also covers ordinary controllers and gear ratios that exceed the recipient's limit.
     */
    @Inject(method = "getConveyedSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dimensionworks$capConveyedSpeed(KineticBlockEntity from, KineticBlockEntity to,
                                                        CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(RpmLimitManager.clampIndividual(to, cir.getReturnValueF()));
    }

    @Inject(method = "isLargeCogToSpeedController", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dimensionworks$advancedController(BlockState from, BlockState to, BlockPos diff,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (!(to.getBlock() instanceof AdvancedControllerBlock)) return;
        cir.setReturnValue(ICogWheel.isLargeCog(from) && diff.equals(BlockPos.ZERO.below())
            && from.getValue(CogWheelBlock.AXIS).isHorizontal()
            && to.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) != from.getValue(CogWheelBlock.AXIS));
    }
}
