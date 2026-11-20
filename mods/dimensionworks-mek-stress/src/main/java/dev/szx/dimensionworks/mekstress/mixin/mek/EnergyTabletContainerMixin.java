package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.integration.mek.ZeroEnergyHandler;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.ComponentBackedHandler;
import mekanism.common.registries.MekanismItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ContainerType.class, remap=false)
public abstract class EnergyTabletContainerMixin {
    @Inject(method={"createHandler", "createHandlerIfData"}, at=@At("HEAD"), cancellable=true, remap=false)
    private void dimensionworks$disableTabletEnergy(ItemStack stack,
            CallbackInfoReturnable<ComponentBackedHandler<?, ?, ?>> cir) {
        if ((Object)this == ContainerType.ENERGY && stack.is(MekanismItems.ENERGY_TABLET.get()))
            cir.setReturnValue(new ZeroEnergyHandler(stack));
    }
}
