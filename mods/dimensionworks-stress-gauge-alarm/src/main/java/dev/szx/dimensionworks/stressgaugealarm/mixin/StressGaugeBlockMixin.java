package dev.szx.dimensionworks.stressgaugealarm.mixin;

import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class StressGaugeBlockMixin {
    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$emitOverstressSignal(BlockState state, BlockGetter level, BlockPos pos,
                                                      Direction direction, CallbackInfoReturnable<Integer> cir) {
        if (level.getBlockEntity(pos) instanceof StressGaugeBlockEntity gauge && gauge.isOverStressed()) {
            cir.setReturnValue(15);
        }
    }
}
