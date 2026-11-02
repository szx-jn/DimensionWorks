package dev.szx.dimensionworks.jeiticks.mixin.client;

import dev.szx.dimensionworks.jeiticks.client.RecipeDrawContext;
import dev.szx.dimensionworks.jeiticks.client.RecipeTimeResolver;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mezz.jei.library.gui.recipes.RecipeLayout", remap = false)
public abstract class RecipeLayoutMixin<R> {
    @Shadow @Final private IRecipeCategory<R> recipeCategory;
    @Shadow @Final private R recipe;
    @Unique private long dimensionworks$ticks;

    @Inject(method = "drawRecipe", at = @At("HEAD"))
    private void dimensionworks$readTime(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        dimensionworks$ticks = RecipeTimeResolver.resolve(recipeCategory, recipe);
    }

    @Redirect(method = "drawRecipe", at = @At(value = "INVOKE", target =
        "Lmezz/jei/api/recipe/category/IRecipeCategory;draw(Ljava/lang/Object;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V"))
    private void dimensionworks$category(IRecipeCategory<R> category, R value, IRecipeSlotsView slots,
                                        GuiGraphics graphics, double mouseX, double mouseY) {
        RecipeDrawContext.draw(dimensionworks$ticks, () -> category.draw(value, slots, graphics, mouseX, mouseY));
    }

    @Redirect(method = "drawRecipe", at = @At(value = "INVOKE", target =
        "Lmezz/jei/api/gui/widgets/IRecipeWidget;drawWidget(Lnet/minecraft/client/gui/GuiGraphics;DD)V"))
    private void dimensionworks$widget(IRecipeWidget widget, GuiGraphics graphics, double mouseX, double mouseY) {
        RecipeDrawContext.draw(dimensionworks$ticks, () -> widget.drawWidget(graphics, mouseX, mouseY));
    }

    @Redirect(method = "drawRecipe", at = @At(value = "INVOKE", target =
        "Lmezz/jei/api/gui/drawable/IDrawable;draw(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void dimensionworks$drawable(IDrawable drawable, GuiGraphics graphics) {
        RecipeDrawContext.draw(dimensionworks$ticks, () -> drawable.draw(graphics));
    }
}
