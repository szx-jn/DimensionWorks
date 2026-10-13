package dev.szx.dimensionworks.rpmlimit.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.szx.dimensionworks.rpmlimit.OverspeedBonus;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.quota.FinalProductionGate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/** Repeats basin recipes and gates tagged final output before ingredients are consumed. */
@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin {

    @Shadow
    protected Recipe<?> currentRecipe;

    @Shadow
    protected abstract Optional<BasinBlockEntity> getBasin();

    @Invoker("applyBasinRecipe")
    protected abstract void dimensionworks$applyBasinRecipe();

    @Inject(method = "applyBasinRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void dimensionworks$gateFinalOutput(CallbackInfo ci) {
        if (currentRecipe == null) {
            return;
        }
        Optional<BasinBlockEntity> maybeBasin = getBasin();
        if (maybeBasin.isEmpty() || !BasinRecipe.match(maybeBasin.get(), currentRecipe)) {
            return;
        }

        BasinOperatingBlockEntity self = (BasinOperatingBlockEntity) (Object) this;
        List<ItemStack> outputs = currentRecipe instanceof ProcessingRecipe<?> processingRecipe
            ? processingRecipe.getRollableResultsAsItemStacks()
            : List.of(currentRecipe.getResultItem(self.getLevel().registryAccess()));
        long tick = self.getLevel() == null ? 0L : self.getLevel().getGameTime();
        if (!FinalProductionGate.tryConsumeOutputs(
            RpmLimitManager.owner(self), outputs, tick)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyBasinRecipe", at = @At("RETURN"), remap = false)
    private void dimensionworks$overspeedBasinRecipe(CallbackInfo ci) {
        OverspeedBonus.repeatBasin(
            (BasinOperatingBlockEntity) (Object) this,
            this::dimensionworks$applyBasinRecipe
        );
    }
}
