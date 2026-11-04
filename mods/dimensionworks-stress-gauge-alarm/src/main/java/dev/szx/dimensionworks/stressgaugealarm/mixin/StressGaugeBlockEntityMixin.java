package dev.szx.dimensionworks.stressgaugealarm.mixin;

import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import dev.szx.dimensionworks.stressgaugealarm.redstone.StressGaugeSignal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StressGaugeBlockEntity.class, remap = false)
public abstract class StressGaugeBlockEntityMixin {
    @org.spongepowered.asm.mixin.Unique private int dimensionworks$updateDepth;
    @org.spongepowered.asm.mixin.Unique private int dimensionworks$oldSignal;

    @Inject(method = "updateFromNetwork", at = @At("HEAD"), remap = false)
    private void dimensionworks$captureSignal(float maxStress, float currentStress, int networkSize,
                                               CallbackInfo ci) {
        StressGaugeBlockEntity self = (StressGaugeBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide) return;
        if (dimensionworks$updateDepth++ == 0) {
            dimensionworks$oldSignal = StressGaugeSignal.analogOutput(self.dialTarget);
        }
    }

    @Inject(method = "updateFromNetwork", at = @At("RETURN"), remap = false)
    private void dimensionworks$notifySignalChange(float maxStress, float currentStress, int networkSize,
                                                    CallbackInfo ci) {
        StressGaugeBlockEntity self = (StressGaugeBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide || dimensionworks$updateDepth == 0) return;
        if (--dimensionworks$updateDepth == 0
            && dimensionworks$oldSignal != StressGaugeSignal.analogOutput(self.dialTarget)) {
            dimensionworks$notifyNeighbors();
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private void dimensionworks$notifyNeighbors() {
        StressGaugeBlockEntity self = (StressGaugeBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide) return;
        BlockPos pos = self.getBlockPos();
        level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        level.updateNeighbourForOutputSignal(pos, level.getBlockState(pos).getBlock());
    }
}
