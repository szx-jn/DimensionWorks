package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.quota.FinalProductionGate;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Gates tagged mill output after simulating all result slots without consuming input. */
@Mixin(value = MillstoneBlockEntity.class, remap = false)
public abstract class MillstoneBlockEntityMixin {

    @Shadow
    private MillingRecipe lastRecipe;

    @Invoker("process")
    protected abstract void dimensionworks$process();

    @Inject(method = "process", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$gateFinalOutput(CallbackInfo ci) {
        MillstoneBlockEntity self = (MillstoneBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }

        RecipeWrapper inventoryIn = new RecipeWrapper(self.inputInv);
        MillingRecipe recipe = lastRecipe;
        if (recipe == null || !recipe.matches(inventoryIn, self.getLevel())) {
            Optional<MillingRecipe> found = AllRecipeTypes.MILLING.find(inventoryIn, self.getLevel());
            if (found.isEmpty()) {
                return;
            }
            recipe = found.get();
        }

        List<ItemStack> outputs = new ArrayList<>(recipe.getRollableResultsAsItemStacks());
        ItemStack remaining = self.inputInv.getStackInSlot(0).getCraftingRemainingItem();
        if (!remaining.isEmpty()) {
            outputs.add(remaining);
        }

        ItemStackHandler simulated = new ItemStackHandler(self.outputInv.getSlots());
        for (int slot = 0; slot < self.outputInv.getSlots(); slot++) {
            simulated.setStackInSlot(slot, self.outputInv.getStackInSlot(slot).copy());
        }
        for (ItemStack output : outputs) {
            if (!ItemHandlerHelper.insertItemStacked(simulated, output.copy(), false).isEmpty()) {
                return;
            }
        }

        if (!FinalProductionGate.tryConsumeOutputs(
            RpmLimitManager.owner(self), outputs, self.getLevel().getGameTime())) {
            ci.cancel();
        }
    }

    @Inject(method = "process", at = @At("RETURN"), remap = false)
    private void dimensionworks$overspeedProcess(CallbackInfo ci) {
        OverspeedBonus.repeatMillstone(
            (MillstoneBlockEntity) (Object) this,
            this::dimensionworks$process
        );
    }
}
