package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Repeats only the completed basin recipe operation. The mixer/press animation state machine still
 * advances once through its normal tick.
 */
@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin {

    @Shadow
    protected Recipe<?> currentRecipe;

    @Shadow
    protected abstract Optional<BasinBlockEntity> getBasin();

    @Invoker("applyBasinRecipe")
    protected abstract void dimensionworks$applyBasinRecipe();

    @Inject(method = "applyBasinRecipe", at = @At("RETURN"), remap = false)
    private void dimensionworks$overspeedBasinRecipe(CallbackInfo ci) {
        OverspeedBonus.repeatBasin(
            (BasinOperatingBlockEntity) (Object) this,
            this::dimensionworks$applyBasinRecipe
        );
    }

    @Inject(method = "applyBasinRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$failCursedBasinOutput(CallbackInfo ci) {
        Optional<BasinBlockEntity> basin = getBasin();
        if (basin.isPresent()
            && GearHeartEffects.failBasinOutput(
                (BasinOperatingBlockEntity) (Object) this, basin.get(), currentRecipe)) {
            ci.cancel();
        }
    }
}
