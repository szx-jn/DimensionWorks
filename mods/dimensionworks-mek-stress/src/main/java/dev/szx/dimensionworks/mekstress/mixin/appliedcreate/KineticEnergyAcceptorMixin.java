package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import appeng.api.config.PowerUnits;
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorBlockEntity;
import dev.szx.dimensionworks.mekstress.core.EnergyPowerMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Charge the actual SU load, using AE2's configured FE/AE ratio, including reverse rotation. */
@Mixin(value = KineticEnergyAcceptorBlockEntity.class, remap = false)
public abstract class KineticEnergyAcceptorMixin {
    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0, remap = false)
    private double dimensionworks$mechanicalPower(double original) {
        KineticEnergyAcceptorBlockEntity acceptor = (KineticEnergyAcceptorBlockEntity) (Object) this;
        if (acceptor.getLevel() == null || acceptor.getLevel().isClientSide || acceptor.isOverStressed()) return 0;
        double su = acceptor.calculateStressApplied() * (double) Math.abs(acceptor.getSpeed());
        return PowerUnits.FE.convertTo(PowerUnits.AE, su / EnergyPowerMath.SU_PER_FE);
    }
}
