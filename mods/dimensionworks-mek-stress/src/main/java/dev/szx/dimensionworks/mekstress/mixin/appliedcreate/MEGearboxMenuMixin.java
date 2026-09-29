package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import com.loliball.appliedcreate.energy.MEGearboxMenu;
import dev.szx.dimensionworks.mekstress.core.AppliedCreateStressLimit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MEGearboxMenu.class, remap = false)
public abstract class MEGearboxMenuMixin {

    @Shadow
    @Final
    private MEGearboxBlockEntity gearbox;

    @Redirect(
        method = "handleSetStress",
        at = @At(
            value = "INVOKE",
            target = "Lcom/loliball/appliedcreate/energy/MEGearboxBlockEntity$Companion;getMaxStress()F"
        ),
        remap = false
    )
    private float dimensionworks$allowStressForOutputSpeed(MEGearboxBlockEntity.Companion companion) {
        return AppliedCreateStressLimit.effectiveGearboxMaximum(
            companion.getMaxStress(),
            gearbox.getConfiguredSpeed()
        );
    }
}
