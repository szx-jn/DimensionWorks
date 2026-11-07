package dev.szx.dimensionworks.mekstress.mixin.mek;

import dev.szx.dimensionworks.mekstress.integration.mek.ZeroEnergyHandler;
import java.util.List;
import mekanism.common.capabilities.ItemCapabilityWrapper.ItemCapability;
import mekanism.common.capabilities.energy.item.ItemStackEnergyHandler;
import mekanism.common.item.ItemEnergized;
import mekanism.common.registries.MekanismItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemEnergized.class, remap = false)
public abstract class ItemEnergizedEnergyTabletMixin {

    @Inject(method = "gatherCapabilities", at = @At("TAIL"), remap = false)
    private void dimensionworks$useZeroEnergyTablet(List<ItemCapability> capabilities, ItemStack stack,
                                                   CompoundTag nbt, CallbackInfo ci) {
        if (stack.getItem() != MekanismItems.ENERGY_TABLET.get()) {
            return;
        }
        capabilities.removeIf(capability -> capability instanceof ItemStackEnergyHandler);
        ZeroEnergyHandler.scrub(stack);
        capabilities.add(new ZeroEnergyHandler());
    }

    @Inject(method = "addItems", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$addUnchargedTablet(CreativeModeTab.Output output, CallbackInfo ci) {
        if (!dimensionworks$isEnergyTablet()) {
            return;
        }
        output.accept(new ItemStack(MekanismItems.ENERGY_TABLET.get()));
        ci.cancel();
    }

    @Unique
    private boolean dimensionworks$isEnergyTablet() {
        return (Object) this == MekanismItems.ENERGY_TABLET.get();
    }
}
