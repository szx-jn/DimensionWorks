package dev.szx.dimensionworks.mekstress.mixin.minecraft;

import dev.szx.dimensionworks.mekstress.card.MemoryCardCraftingHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMemoryCardMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void dw$prepareMemoryCardQuickMove(
        Player player, int index, CallbackInfoReturnable<ItemStack> callback) {
        if (index == 0) {
            MemoryCardCraftingHandler.prepareQuickMove(player,
                ((AbstractContainerMenu) (Object) this).slots.get(index));
        }
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void dw$finishMemoryCardQuickMove(
        Player player, int index, CallbackInfoReturnable<ItemStack> callback) {
        MemoryCardCraftingHandler.finishQuickMove(player);
    }
}
