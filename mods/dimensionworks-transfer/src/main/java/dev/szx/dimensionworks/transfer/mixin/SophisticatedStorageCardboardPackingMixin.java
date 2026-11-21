package dev.szx.dimensionworks.transfer.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Mark Sophisticated Storage contents packed before Mekanism snapshots the block entity. */
@Pseudo
@Mixin(targets = "mekanism.common.item.block.ItemBlockCardboardBox", remap = false)
public abstract class SophisticatedStorageCardboardPackingMixin {
    @ModifyArg(method = "onItemUseFirst", at = @At(value = "INVOKE",
            target = "Lmekanism/common/attachments/BlockData;<init>(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            remap = false), index = 2, remap = false)
    private BlockEntity dimensionworks$markStoragePacked(BlockEntity blockEntity) {
        if (ModList.get().isLoaded("sophisticatedstorage")
                && blockEntity instanceof SophisticatedStoragePackedAccessor accessor) {
            accessor.dimensionworks$setPacked(true);
        }
        return blockEntity;
    }
}
