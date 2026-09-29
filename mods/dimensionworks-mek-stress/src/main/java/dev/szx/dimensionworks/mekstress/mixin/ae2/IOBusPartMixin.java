package dev.szx.dimensionworks.mekstress.mixin.ae2;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickingRequest;
import appeng.parts.automation.IOBusPart;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IOBusPart.class, remap = false)
public abstract class IOBusPartMixin {
    @Inject(method = "getTickingRequest", at = @At("RETURN"), cancellable = true, remap = false)
    private void dimensionworks$fastStressTick(IGridNode node, CallbackInfoReturnable<TickingRequest> cir) {
        IOBusPart self = (IOBusPart) (Object) this;
        if (!StressRules.hasStressOutputCard(self)) {
            return;
        }
        TickingRequest original = cir.getReturnValue();
        cir.setReturnValue(new TickingRequest(1, original.maxTickRate(), false, original.canBeAlerted()));
    }
}
