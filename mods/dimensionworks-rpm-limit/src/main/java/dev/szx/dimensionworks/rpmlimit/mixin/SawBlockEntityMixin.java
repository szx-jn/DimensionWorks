package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.quota.FinalProductionGate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Gates tagged saw output before the native recipe clears or rewrites its inventory. */
@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class SawBlockEntityMixin {

    @Shadow
    private int recipeIndex;

    @Invoker("getRecipes")
    protected abstract List<? extends Recipe<?>> dimensionworks$getRecipes();

    @Inject(method = "applyRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$gateFinalOutput(CallbackInfo ci) {
        SawBlockEntity self = (SawBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }

        ItemStack input = self.inventory.getStackInSlot(0);
        if (input.isEmpty()) {
            return;
        }

        List<ItemStack> outputs = new ArrayList<>();
        if (PackageItem.isPackage(input)) {
            ItemStackHandler contents = PackageItem.getContents(input);
            for (int slot = 0; slot < contents.getSlots(); slot++) {
                ItemStack stack = contents.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    outputs.add(stack.copy());
                }
            }
        } else {
            List<? extends Recipe<?>> recipes = dimensionworks$getRecipes();
            if (recipes.isEmpty()) {
                return;
            }
            int index = recipeIndex >= recipes.size() ? 0 : recipeIndex;
            Recipe<?> recipe = recipes.get(index);
            if (recipe instanceof ProcessingRecipe<?> processingRecipe) {
                for (ItemStack possible : processingRecipe.getRollableResultsAsItemStacks()) {
                    outputs.add(possible.copyWithCount(
                        saturatedMultiply(possible.getCount(), input.getCount())));
                }
            } else {
                ItemStack result = recipe.getResultItem(self.getLevel().registryAccess());
                if (!result.isEmpty()) {
                    outputs.add(result.copyWithCount(
                        saturatedMultiply(result.getCount(), input.getCount())));
                }
            }
            ItemStack remaining = input.getCraftingRemainingItem();
            if (!remaining.isEmpty()) {
                outputs.add(remaining.copyWithCount(
                    saturatedMultiply(remaining.getCount(), input.getCount())));
            }
        }

        if (!hasOutputSpace(self.inventory, outputs)
            || !FinalProductionGate.tryConsumeOutputs(
                RpmLimitManager.owner(self), outputs, self.getLevel().getGameTime())) {
            ci.cancel();
        }
    }

    private static boolean hasOutputSpace(ProcessingInventory inventory, List<ItemStack> outputs) {
        ItemStackHandler simulated = new ItemStackHandler(inventory.getSlots());
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            simulated.setStackInSlot(slot, inventory.getStackInSlot(slot).copy());
        }
        for (ItemStack output : outputs) {
            int target = -1;
            for (int slot = 1; slot < simulated.getSlots(); slot++) {
                if (simulated.insertItem(slot, output.copy(), true).isEmpty()) {
                    target = slot;
                    break;
                }
            }
            if (target < 0 || !simulated.insertItem(target, output.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int saturatedMultiply(int value, int count) {
        if (value <= 0 || count <= 0) {
            return 0;
        }
        long result = (long) value * (long) count;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }
}
