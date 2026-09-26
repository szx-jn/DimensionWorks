package dev.szx.dimensionworks.rpmlimit.mixin.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps Flywheel rotation visually capped while the server keeps processing at the real RPM. */
@Mixin(value = RotatingInstance.class, remap = false)
public abstract class RotatingInstanceMixin {

    @Redirect(
        method = "setup(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;getSpeed()F"
        ),
        remap = false
    )
    private float dimensionworks$capVisualSpeed(KineticBlockEntity be) {
        return OverspeedBonus.animationSpeed(be);
    }

    @Redirect(
        method = "setup(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/core/Direction$Axis;)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;getSpeed()F"
        ),
        remap = false
    )
    private float dimensionworks$capAxisVisualSpeed(KineticBlockEntity be) {
        return OverspeedBonus.animationSpeed(be);
    }
}
