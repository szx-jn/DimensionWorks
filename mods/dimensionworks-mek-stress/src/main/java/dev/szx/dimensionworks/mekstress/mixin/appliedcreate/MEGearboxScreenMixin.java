package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import com.loliball.appliedcreate.energy.MEGearboxBlockEntity;
import com.loliball.appliedcreate.energy.MEGearboxMenu;
import com.loliball.appliedcreate.energy.MEGearboxScreen;
import dev.szx.dimensionworks.mekstress.core.AppliedCreateStressLimit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MEGearboxScreen.class, remap = false)
public abstract class MEGearboxScreenMixin {

    @Redirect(
        method = "onStressChanged",
        at = @At(
            value = "INVOKE",
            target = "Lcom/loliball/appliedcreate/energy/MEGearboxBlockEntity$Companion;getMaxStress()F"
        ),
        remap = false
    )
    private float dimensionworks$allowStressForOutputSpeed(MEGearboxBlockEntity.Companion companion) {
        MEGearboxScreen self = (MEGearboxScreen) (Object) this;
        MEGearboxMenu menu = self.getMenu();
        return AppliedCreateStressLimit.effectiveGearboxMaximum(
            companion.getMaxStress(),
            menu.currentConfiguredSpeed
        );
    }
}
