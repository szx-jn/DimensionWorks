package dev.szx.dimensionworks.mekstress.mixin.ae2;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.IOBusMenu;
import dev.szx.dimensionworks.mekstress.core.StressOutputMenuAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AEBaseMenu.class, remap = false)
public abstract class AEBaseMenuStressSyncMixin {

    @Inject(method = "broadcastChanges", at = @At("TAIL"), remap = true)
    private void dimensionworks$syncStressSettings(CallbackInfo ci) {
        if ((Object) this instanceof IOBusMenu) {
            ((StressOutputMenuAccess) this).dimensionworks$refreshStressSettings();
        }
    }
}
