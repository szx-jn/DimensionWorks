package dev.szx.dimensionworks.itempickupguard.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupGuardMixin {
    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$hideStackAfterRemoval(CallbackInfoReturnable<ItemStack> cir) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (itemEntity.isRemoved()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
