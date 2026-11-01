package dev.szx.dimensionworks.stressgaugealarm.mixin;

import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StressGaugeBlockEntity.class)
public abstract class StressGaugeBlockEntityMixin {
    @Shadow public Level level;
    @Shadow public BlockPos worldPosition;

    @Inject(method = "updateFromNetwork", at = @At("RETURN"))
    private void dimensionworks$notifySignalChange(float maxStress, float currentStress, int networkSize,
                                                    CallbackInfo ci) {
        dimensionworks$notifyNeighbors();
    }

    @org.spongepowered.asm.mixin.Unique
    private void dimensionworks$notifyNeighbors() {
        if (level == null || level.isClientSide) return;
        level.updateNeighborsAt(worldPosition, level.getBlockState(worldPosition).getBlock());
        level.updateNeighbourForOutputSignal(worldPosition, level.getBlockState(worldPosition).getBlock());
    }
}
