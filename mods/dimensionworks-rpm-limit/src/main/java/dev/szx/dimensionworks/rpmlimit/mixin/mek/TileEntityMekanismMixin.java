package dev.szx.dimensionworks.rpmlimit.mixin.mek;

import dev.szx.dimensionworks.rpmlimit.quota.MekProcessingContext;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Establishes quota context around the static server tick entry. Mekanism subclasses override
 * onUpdateServer, so injecting only the empty base implementation would miss real machines.
 */
@Mixin(value = TileEntityMekanism.class, remap = false)
public abstract class TileEntityMekanismMixin {

    @Inject(method = "tickServer", at = @At("HEAD"), remap = false)
    private static void dimensionworks$pushQuotaContext(Level level, BlockPos pos,
                                                        BlockState state, TileEntityMekanism tile,
                                                        CallbackInfo ci) {
        MekProcessingContext.push(tile);
    }

    @Inject(method = "tickServer", at = @At("RETURN"), remap = false)
    private static void dimensionworks$popQuotaContext(Level level, BlockPos pos,
                                                       BlockState state, TileEntityMekanism tile,
                                                       CallbackInfo ci) {
        MekProcessingContext.pop(null);
    }
}
