package dev.szx.dimensionworks.cavefactory.mixin;

import dev.szx.dimensionworks.cavefactory.MoltenCrystallizationEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockPlacementMixin {
    @Inject(method = "onPlace", at = @At("TAIL"))
    private void dimensionworks$trackMoltenExposure(BlockState state, Level level, BlockPos pos,
                                                    BlockState previous, boolean moving, CallbackInfo ci) {
        if (!state.is(previous.getBlock())) MoltenCrystallizationEvents.onPlaced(level, pos, state);
    }
}
