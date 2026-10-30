package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import dev.szx.dimensionworks.rpmlimit.controller.AdvancedControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {
    @Inject(method = "isLargeCogToSpeedController", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dimensionworks$advancedController(BlockState from, BlockState to, BlockPos diff,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (!(to.getBlock() instanceof AdvancedControllerBlock)) return;
        cir.setReturnValue(ICogWheel.isLargeCog(from) && diff.equals(BlockPos.ZERO.below())
            && from.getValue(CogWheelBlock.AXIS).isHorizontal()
            && to.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) != from.getValue(CogWheelBlock.AXIS));
    }
}
