package dev.szx.dimensionworks.rpmlimit.mixin.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies the same visual speed cap to the non-Flywheel kinetic renderer. */
@Mixin(value = KineticBlockEntityRenderer.class, remap = false)
public abstract class KineticBlockEntityRendererMixin {

    @Redirect(
        method = "getAngleForBe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;getSpeed()F"
        ),
        remap = false
    )
    private static float dimensionworks$capRenderedAngleSpeed(KineticBlockEntity be) {
        return OverspeedBonus.animationSpeed(be);
    }
}
