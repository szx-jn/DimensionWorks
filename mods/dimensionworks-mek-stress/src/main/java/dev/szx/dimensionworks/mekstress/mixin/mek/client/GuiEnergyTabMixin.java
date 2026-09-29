package dev.szx.dimensionworks.mekstress.mixin.mek.client;

import dev.szx.dimensionworks.mekstress.core.StressContainerOwner;
import dev.szx.dimensionworks.mekstress.core.StressDisplayContext;
import dev.szx.dimensionworks.mekstress.core.StressRules;
import java.util.function.BooleanSupplier;
import mekanism.api.math.FloatingLongSupplier;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiEnergyTab.class, remap = false)
public abstract class GuiEnergyTabMixin {

    @Unique
    private boolean dimensionworks$stressTab;

    @Inject(
        method = "<init>(Lmekanism/client/gui/IGuiWrapper;Lmekanism/common/capabilities/energy/MachineEnergyContainer;Lmekanism/api/math/FloatingLongSupplier;)V",
        at = @At("RETURN"),
        remap = false
    )
    private void dimensionworks$markStressTab(IGuiWrapper gui, MachineEnergyContainer<?> container,
                                               FloatingLongSupplier rate, CallbackInfo ci) {
        dimensionworks$stressTab = dimensionworks$isStressContainer(container);
    }

    @Inject(
        method = "<init>(Lmekanism/client/gui/IGuiWrapper;Lmekanism/common/capabilities/energy/MachineEnergyContainer;Ljava/util/function/BooleanSupplier;)V",
        at = @At("RETURN"),
        remap = false
    )
    private void dimensionworks$markStressTab(IGuiWrapper gui, MachineEnergyContainer<?> container,
                                               BooleanSupplier active, CallbackInfo ci) {
        dimensionworks$stressTab = dimensionworks$isStressContainer(container);
    }

    @Inject(method = "renderToolTip", at = @At("HEAD"), remap = false)
    private void dimensionworks$enterStressContext(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (dimensionworks$stressTab) {
            StressDisplayContext.push();
        }
    }

    @Inject(method = "renderToolTip", at = @At("RETURN"), remap = false)
    private void dimensionworks$exitStressContext(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (dimensionworks$stressTab) {
            StressDisplayContext.pop();
        }
    }

    private static boolean dimensionworks$isStressContainer(MachineEnergyContainer<?> container) {
        return container instanceof StressContainerOwner owner
            && owner.dimensionworks$getOwner() != null
            && StressRules.canAcceptStress(owner.dimensionworks$getOwner());
    }
}
