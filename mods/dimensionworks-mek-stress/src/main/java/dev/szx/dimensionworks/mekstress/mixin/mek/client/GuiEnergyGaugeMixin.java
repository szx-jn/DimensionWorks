package dev.szx.dimensionworks.mekstress.mixin.mek.client;

import dev.szx.dimensionworks.mekstress.core.StressContainerOwner;
import dev.szx.dimensionworks.mekstress.core.StressDisplayContext;
import dev.szx.dimensionworks.mekstress.core.StressRules;
import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiEnergyGauge.class, remap = false)
public abstract class GuiEnergyGaugeMixin {

    @Unique
    private boolean dimensionworks$stressGauge;

    @Inject(
        method = "<init>(Lmekanism/api/energy/IEnergyContainer;Lmekanism/client/gui/element/gauge/GaugeType;Lmekanism/client/gui/IGuiWrapper;II)V",
        at = @At("RETURN"),
        remap = false
    )
    private void dimensionworks$markStressGauge(IEnergyContainer container, GaugeType type, IGuiWrapper gui,
                                                 int x, int y, CallbackInfo ci) {
        dimensionworks$stressGauge = container instanceof StressContainerOwner owner
            && owner.dimensionworks$getOwner() != null
            && StressRules.canAcceptStress(owner.dimensionworks$getOwner());
    }

    @Inject(method = "getTooltipText", at = @At("HEAD"), remap = false)
    private void dimensionworks$enterStressContext(CallbackInfoReturnable<List<Component>> cir) {
        if (dimensionworks$stressGauge) {
            StressDisplayContext.push();
        }
    }

    @Inject(method = "getTooltipText", at = @At("RETURN"), remap = false)
    private void dimensionworks$exitStressContext(CallbackInfoReturnable<List<Component>> cir) {
        if (dimensionworks$stressGauge) {
            StressDisplayContext.pop();
        }
    }
}
