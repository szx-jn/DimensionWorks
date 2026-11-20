package dev.szx.dimensionworks.transfer;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import de.theidler.create_mobile_packages.index.CMPItems;
import dev.szx.dimensionworks.transfer.elastic.ElasticItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.BuiltInRegistries;

/** Runs with -PelasticRecipeCheck and the actual production KubeJS scripts. */
@GameTestHolder("dimensionworks_elastic_recipe")
@PrefixGameTestTemplate(false)
public final class ElasticRecipeGameTests {
    private static ItemStack item(String id) {
        ItemStack stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        if(stack.isEmpty())throw new AssertionError("Missing item "+id);
        return stack;
    }

    @GameTest(template="empty")
    public static void realKubeJsUpgradePreservesDataAndRejectsIncompleteTape(GameTestHelper h) {
        var recipe=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("dimensionworks:elastic/upgrade_packager")).orElseThrow().value();
        var menu=new AbstractContainerMenu(null,-1) {
            @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        };
        var grid=new TransientCraftingContainer(menu,2,2);
        ItemStack original=CMPItems.MOBILE_PACKAGER.asStack();
        original.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Payload-preserving upgrade"));
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, original, tag -> tag.putString("SavedAddress","my-factory"));
        var before=original.save(h.getLevel().registryAccess());
        grid.setItem(0,original);grid.setItem(1,item("kubejs:bounce_tape"));
        h.assertTrue(recipe.matches(grid.asCraftInput(),h.getLevel()),"Finished tape and tool must match the actual KubeJS recipe");
        ItemStack upgraded=recipe.assemble(grid.asCraftInput(),h.getLevel().registryAccess());
        h.assertTrue(upgraded.is(ElasticItems.PACKAGER.get()) && upgraded.getComponentsPatch().equals(original.getComponentsPatch()),"Actual crafting callback must preserve all tool NBT");
        h.assertTrue(original.save(h.getLevel().registryAccess()).equals(before),"Crafting preview must leave input intact");
        grid.setItem(1,item("kubejs:incomplete_bounce_tape"));
        h.assertTrue(!recipe.matches(grid.asCraftInput(),h.getLevel()),"Incomplete tape must not shortcut final assembly");
        h.succeed();
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    @GameTest(template="empty")
    public static void allTenAssemblyStepsAdvanceAndOnlyLastProducesTape(GameTestHelper h) {
        var recipe=(SequencedAssemblyRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("dimensionworks:elastic/bounce_tape")).orElseThrow().value();
        h.assertTrue(recipe.getSequence().size()==10 && recipe.getLoops()==1,"Actual assembly must load all ten steps once");
        ItemStack work=item("kubejs:elastic_compound");
        for(int index=0;index<10;index++) {
            ProcessingRecipe<?,?> expected=recipe.getSequence().get(index).getRecipe();
            for(var ingredient:expected.getIngredients())h.assertTrue(ingredient.getItems().length>0,"All step ingredients must resolve");
            var holder=(net.minecraft.world.item.crafting.RecipeHolder<?>) SequencedAssemblyRecipe.getRecipe(h.getLevel(),work,(RecipeType)expected.getType(),expected.getClass()).orElseThrow();
            ProcessingRecipe selected=(ProcessingRecipe) holder.value();
            work=(ItemStack)selected.rollResults(h.getLevel().random).get(0);
            if(index<9) {
                h.assertTrue(work.is(item("kubejs:incomplete_bounce_tape").getItem()),"Intermediate steps must stay unfinished");
                h.assertTrue(work.get(com.simibubi.create.AllDataComponents.SEQUENCED_ASSEMBLY).step()==index+1,"Create must retain real progress");
            }
        }
        h.assertTrue(work.is(item("kubejs:bounce_tape").getItem()) && !work.has(com.simibubi.create.AllDataComponents.SEQUENCED_ASSEMBLY),"Only the tenth step must produce finished tape");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void upstreamIngredientsAndNativeIronwoodSpringRecipesResolve(GameTestHelper h) {
        for(String id:new String[]{"dimensionworks:elastic/compound","dimensionworks:elastic/seal_strip","vintageimprovements:coiling/ironwood_rod","vintageimprovements:coiling/ironwood_wire"}) {
            var recipe=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow();
            for(var ingredient:recipe.value().getIngredients())h.assertTrue(ingredient.getItems().length>0,"Upstream ingredient must resolve: "+id);
            h.assertTrue(!recipe.value().getResultItem(h.getLevel().registryAccess()).isEmpty(),"Upstream output must exist: "+id);
        }
        h.succeed();
    }
}
