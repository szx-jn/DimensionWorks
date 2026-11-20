package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.integration.mek.ZeroEnergyHandler;
import java.util.function.Consumer;
import mekanism.common.item.ItemEnergized;
import mekanism.common.registries.MekanismItems;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ItemEnergized.class, remap=false)
public abstract class ItemEnergizedEnergyTabletMixin {
    @Inject(method="addItems", at=@At("HEAD"), cancellable=true, remap=false)
    private void dimensionworks$addUnchargedTablet(Holder<Item> item, Consumer<ItemStack> output, CallbackInfo ci) {
        if ((Object)this != MekanismItems.ENERGY_TABLET.get()) return;
        var stack = new ItemStack(item); ZeroEnergyHandler.scrub(stack); output.accept(stack); ci.cancel();
    }
}
