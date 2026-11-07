package dev.szx.dimensionworks.cavefactory.compat.jei;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.twilight.TwilightIndustry;
import dev.szx.dimensionworks.cavefactory.twilight.TwilightWorkRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import java.util.Arrays;
import java.util.List;

@JeiPlugin
public final class TwilightJeiPlugin implements IModPlugin {
    private static final RecipeType<TwilightWorkRecipe> TYPE = RecipeType.create(
        DimensionWorksCaveFactory.MOD_ID, "twilight_work", TwilightWorkRecipe.class);
    @Override public ResourceLocation getPluginUid() { return DimensionWorksCaveFactory.id("twilight_jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(TwilightIndustry.LEACHER.get()), TYPE);
        registration.addRecipeCatalyst(new ItemStack(TwilightIndustry.CONDITIONER.get()), TYPE);
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) registration.addRecipes(TYPE, level.getRecipeManager().getAllRecipesFor(TwilightIndustry.RECIPE_TYPE.get()));
        registration.addItemStackInfo(List.of(new ItemStack(TwilightIndustry.LEACHER.get())),
            Component.translatable("jei.dimensionworks_cave_factory.twilight.leacher"));
        registration.addItemStackInfo(List.of(new ItemStack(TwilightIndustry.CONDITIONER.get())),
            Component.translatable("jei.dimensionworks_cave_factory.twilight.conditioner"));
        registration.addItemStackInfo(List.of(new ItemStack(TwilightIndustry.FILTER.get())),
            Component.translatable("jei.dimensionworks_cave_factory.twilight.filter"));
    }
    private static final class Category implements IRecipeCategory<TwilightWorkRecipe> {
        private final IDrawable background, icon;
        Category(IGuiHelper helper) {
            background = helper.createBlankDrawable(166, 88);
            icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(TwilightIndustry.LEACHER.get()));
        }
        @Override public RecipeType<TwilightWorkRecipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("jei.dimensionworks_cave_factory.twilight.title"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, TwilightWorkRecipe r, IFocusGroup focus) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5, 5).addItemStacks(Arrays.stream(r.first().getItems())
                .map(stack -> stack.copyWithCount(r.firstCount())).toList());
            builder.addSlot(RecipeIngredientRole.INPUT, 35, 5).addItemStacks(Arrays.stream(r.second().getItems())
                .map(stack -> stack.copyWithCount(r.secondCount())).toList());
            builder.addSlot(RecipeIngredientRole.INPUT, 65, 5).addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(Fluids.WATER, r.water()));
            var out = builder.addSlot(RecipeIngredientRole.OUTPUT, 137, 5);
            if (r.biological()) out.addItemStack(r.result()); else out.addIngredient(ForgeTypes.FLUID_STACK, r.fluidResult());
        }
        @Override public void draw(TwilightWorkRecipe r, IRecipeSlotsView slots, GuiGraphics g, double x, double y) {
            var font = Minecraft.getInstance().font;
            g.drawString(font, "→", 104, 10, 0x55724A, false);
            g.drawWordWrap(font, Component.translatable(r.biological()
                ? "jei.dimensionworks_cave_factory.twilight.boar_requirement"
                : "jei.dimensionworks_cave_factory.twilight.bed_requirement"), 5, 30, 156, 0x444444);
            g.drawString(font, Component.translatable("jei.dimensionworks_cave_factory.twilight.time", r.duration() / 20.0), 5, 68, 0x444444, false);
        }
    }
}
