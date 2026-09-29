package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import dev.szx.dimensionworks.mekstress.core.AppliedCreateStressLimit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MEGearboxBlockEntity.class, remap = false)
public abstract class MEGearboxBlockEntityMixin {

    @Redirect(
        method = "setConfiguredStress",
        at = @At(
            value = "INVOKE",
            target = "Lcom/loliball/appliedcreate/energy/MEGearboxBlockEntity$Companion;getMaxStress()F"
        ),
        remap = false
    )
    private float dimensionworks$allowStressForOutputSpeed(MEGearboxBlockEntity.Companion companion) {
        MEGearboxBlockEntity self = (MEGearboxBlockEntity) (Object) this;
        return AppliedCreateStressLimit.effectiveGearboxMaximum(
            companion.getMaxStress(),
            self.getConfiguredSpeed()
        );
    }
}
