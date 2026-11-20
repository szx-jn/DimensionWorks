package dev.szx.dimensionworks.jeiticks.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.szx.dimensionworks.jeiticks.client.RecipeDrawContext;
import dev.szx.dimensionworks.jeiticks.client.RecipeTimeResolver;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** One exception-safe scope also covers JEI's widget lambdas and positioned drawables. */
@Mixin(targets = "mezz.jei.library.gui.recipes.RecipeLayout", remap = false)
public abstract class RecipeLayoutMixin<R> {
    @Shadow @Final private IRecipeCategory<R> recipeCategory;
    @Shadow @Final private R recipe;

    @WrapMethod(method = "drawRecipe")
    private void dimensionworks$timedDraw(GuiGraphics graphics, int mouseX, int mouseY, Operation<Void> original) {
        RecipeDrawContext.draw(RecipeTimeResolver.resolve(recipeCategory, recipe),
            () -> original.call(graphics, mouseX, mouseY));
    }
}
