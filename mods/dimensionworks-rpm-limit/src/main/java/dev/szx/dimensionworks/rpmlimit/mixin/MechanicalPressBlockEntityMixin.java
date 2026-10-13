package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.quota.FinalProductionGate;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Gates pressing output before world or belt input is consumed. */
@Mixin(value = MechanicalPressBlockEntity.class, remap = false)
public abstract class MechanicalPressBlockEntityMixin {

    @Inject(method = "tryProcessInWorld", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$gateWorldFinalOutput(ItemEntity itemEntity, boolean simulate,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (simulate) {
            return;
        }
        MechanicalPressBlockEntity self = (MechanicalPressBlockEntity) (Object) this;
        Optional<PressingRecipe> recipe = self.getRecipe(itemEntity.getItem());
        if (recipe.isEmpty()) {
            return;
        }
        int rolls = self.canProcessInBulk() ? itemEntity.getItem().getCount() : 1;
        List<ItemStack> outputs = possibleOutputs(recipe.get(), rolls);
        if (!FinalProductionGate.tryConsumeOutputs(
            RpmLimitManager.owner(self), outputs, self.getLevel().getGameTime())) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "tryProcessOnBelt", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$gateBeltFinalOutput(TransportedItemStack input,
                                                    List<ItemStack> outputList,
                                                    boolean simulate,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (simulate) {
            return;
        }
        MechanicalPressBlockEntity self = (MechanicalPressBlockEntity) (Object) this;
        Optional<PressingRecipe> recipe = self.getRecipe(input.stack);
        if (recipe.isEmpty()) {
            return;
        }
        int rolls = self.canProcessInBulk() ? input.stack.getCount() : 1;
        List<ItemStack> outputs = possibleOutputs(recipe.get(), rolls);
        if (!FinalProductionGate.tryConsumeOutputs(
            RpmLimitManager.owner(self), outputs, self.getLevel().getGameTime())) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    private static List<ItemStack> possibleOutputs(PressingRecipe recipe, int rolls) {
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack possible : recipe.getRollableResultsAsItemStacks()) {
            long count = (long) possible.getCount() * Math.max(1, rolls);
            outputs.add(possible.copyWithCount(
                count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count));
        }
        return outputs;
    }
}
