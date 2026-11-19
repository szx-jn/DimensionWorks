package dev.szx.dimensionworks.transfer.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mark Sophisticated Storage contents packed before Mekanism snapshots the block entity. */
@Pseudo
@Mixin(targets = "mekanism.common.item.block.ItemBlockCardboardBox", remap = false)
public abstract class SophisticatedStorageCardboardPackingMixin {
    @Inject(method = "onItemUseFirst", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;saveWithFullMetadata()Lnet/minecraft/nbt/CompoundTag;"))
    private void dimensionworks$markStoragePacked(ItemStack stack, UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof SophisticatedStoragePackedAccessor accessor) {
            accessor.dimensionworks$setPacked(true);
        }
    }
}
