package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Repeats only the completed basin recipe operation. The mixer/press animation state machine still
 * advances once through its normal tick.
 */
@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin {

    @Invoker("applyBasinRecipe")
    protected abstract void dimensionworks$applyBasinRecipe();

    @Inject(method = "applyBasinRecipe", at = @At("RETURN"), remap = false)
    private void dimensionworks$overspeedBasinRecipe(CallbackInfo ci) {
        OverspeedBonus.repeatBasin(
            (BasinOperatingBlockEntity) (Object) this,
            this::dimensionworks$applyBasinRecipe
        );
    }
}
