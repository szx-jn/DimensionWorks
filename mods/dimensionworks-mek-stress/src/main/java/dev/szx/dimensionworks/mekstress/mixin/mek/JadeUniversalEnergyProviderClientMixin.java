package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.integration.jade.JadeEnergyVisibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.addon.universal.EnergyStorageProvider;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

@Mixin(value = EnergyStorageProvider.class, remap = false)
public abstract class JadeUniversalEnergyProviderClientMixin {
    @Inject(
        method = "appendTooltip(Lsnownee/jade/api/ITooltip;Lsnownee/jade/api/Accessor;Lsnownee/jade/api/config/IPluginConfig;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void dimensionworks$skipClientTooltip(ITooltip tooltip, Accessor<?> accessor, IPluginConfig config,
                                                   CallbackInfo ci) {
        if (accessor instanceof BlockAccessor block && JadeEnergyVisibility.shouldHide(block.getBlockEntity())) {
            ci.cancel();
        }
    }
}
