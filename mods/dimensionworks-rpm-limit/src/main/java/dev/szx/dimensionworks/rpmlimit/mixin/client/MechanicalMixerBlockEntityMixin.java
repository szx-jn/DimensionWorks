package dev.szx.dimensionworks.rpmlimit.mixin.client;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Caps the mixer head's rendered rotation at the mixer's native 512 RPM saturation point. */
@Mixin(value = MechanicalMixerBlockEntity.class, remap = false)
public abstract class MechanicalMixerBlockEntityMixin {

    @Redirect(
        method = "getRenderedHeadRotationSpeed",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/mixer/MechanicalMixerBlockEntity;getSpeed()F"
        ),
        remap = false
    )
    private float dimensionworks$capRenderedMixerHead(MechanicalMixerBlockEntity be) {
        return OverspeedBonus.animationSpeed(be);
    }
}
