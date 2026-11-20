package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.integration.mek.MechanicalEnergy;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.TileEntityTeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntityTeleporter.class, remap = false)
public abstract class TeleporterPowerMixin {
    @Redirect(method = "canTeleport", at = @At(value = "INVOKE",
        target = "Lmekanism/common/capabilities/energy/MachineEnergyContainer;extract(JLmekanism/api/Action;Lmekanism/api/AutomationType;)J"), remap = false)
    private long dimensionworks$reserve(MachineEnergyContainer<?> energy, long amount,
        Action action, AutomationType automation) {
        MechanicalEnergy.reserveBurst((TileEntityTeleporter) (Object) this, amount);
        return energy.extract(amount, action, automation);
    }
}
