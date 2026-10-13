package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Repeats the millstone's actual item-processing step without re-running its tick or effects. */
@Mixin(value = MillstoneBlockEntity.class, remap = false)
public abstract class MillstoneBlockEntityMixin {

    @Invoker("process")
    protected abstract void dimensionworks$process();

    @Inject(method = "process", at = @At("RETURN"), remap = false)
    private void dimensionworks$overspeedProcess(CallbackInfo ci) {
        OverspeedBonus.repeatMillstone(
            (MillstoneBlockEntity) (Object) this,
            this::dimensionworks$process
        );
    }
}
