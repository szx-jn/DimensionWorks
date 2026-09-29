package dev.szx.dimensionworks.mekstress.mixin.mek.client;

import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.StressFormula;
import dev.szx.dimensionworks.mekstress.core.StressContainerOwner;
import dev.szx.dimensionworks.mekstress.core.StressDisplayContext;
import dev.szx.dimensionworks.mekstress.core.StressEnergyDisplay;
import dev.szx.dimensionworks.mekstress.core.StressRules;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.FloatingLong;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnergyDisplay.class, remap = false)
public abstract class EnergyDisplayMixin implements StressEnergyDisplay {

    @Shadow
    private FloatingLong energy;

    @Shadow
    private FloatingLong max;

    @Unique
    private boolean dimensionworks$stressDisplay;

    @Override
    public void dimensionworks$setStressDisplay(boolean stressDisplay) {
        dimensionworks$stressDisplay = stressDisplay;
    }

    @Override
    public boolean dimensionworks$isStressDisplay() {
        return dimensionworks$stressDisplay;
    }

    @Inject(method = "of(Lmekanism/api/energy/IEnergyContainer;)Lmekanism/common/util/text/EnergyDisplay;",
        at = @At("RETURN"), remap = false)
    private static void dimensionworks$markStressDisplay(IEnergyContainer container,
                                                          CallbackInfoReturnable<EnergyDisplay> cir) {
        if (container instanceof StressContainerOwner owner
            && owner.dimensionworks$getOwner() != null
            && StressRules.canAcceptStress(owner.dimensionworks$getOwner())) {
            ((StressEnergyDisplay) cir.getReturnValue()).dimensionworks$setStressDisplay(true);
        }
    }

    @Inject(method = "getTextComponent", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$getStressTextComponent(CallbackInfoReturnable<Component> cir) {
        if (!dimensionworks$stressDisplay && !StressDisplayContext.isActive()) {
            return;
        }
        double joulesPerSu = MekStressConfig.joulesPerSu();
        long storedSu = StressFormula.joulesToStressFloor(energy.doubleValue(), joulesPerSu);
        long maxSu = StressFormula.joulesToStressFloor(max.doubleValue(), joulesPerSu);
        if (maxSu <= 0L) {
            cir.setReturnValue(Component.translatable("gui.dimensionworks_mek_stress.energy", storedSu));
        } else {
            cir.setReturnValue(Component.translatable(
                "gui.dimensionworks_mek_stress.energy_fraction",
                storedSu,
                maxSu
            ));
        }
    }
}
