package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.loliball.appliedcreate.storage.CreativeStressCell;
import com.loliball.appliedcreate.storage.StressStorageCell;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StorageCells.class, remap = false)
public abstract class StorageCellsMixin {
    @Inject(method = "isCellHandled", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dimensionworks$hideNativeStressCells(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (isStressCell(stack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getHandler", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dimensionworks$hideNativeStressCellHandler(ItemStack stack,
                                                                   CallbackInfoReturnable<appeng.api.storage.cells.ICellHandler> cir) {
        if (isStressCell(stack)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getCellInventory", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dimensionworks$retireNativeStressCells(ItemStack stack, ISaveProvider saveProvider,
                                                               CallbackInfoReturnable<StorageCell> cir) {
        if (isStressCell(stack)) {
            cir.setReturnValue(null);
        }
    }

    private static boolean isStressCell(ItemStack stack) {
        return stack.getItem() instanceof StressStorageCell || stack.getItem() instanceof CreativeStressCell;
    }
}
