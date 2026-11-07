package dev.szx.dimensionworks.cavefactory.twilight;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import static dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory.CFRegistry.*;

/** Forest-only source machines; downstream materials deliberately have no overworld substitutions. */
public final class TwilightIndustry {
    public static final ResourceLocation DIMENSION = new ResourceLocation("twilightforest", "twilight_forest");
    public static final ResourceLocation EXTRACT = new ResourceLocation("dimensionworks", "forest_extract");
    public static final ResourceLocation BOAR = new ResourceLocation("twilightforest", "boar");
    public static final TagKey<Item> LEAVES = TagKey.create(Registries.ITEM,
        DimensionWorksCaveFactory.id("twilight_leaves"));
    public static final TagKey<Item> FEED = TagKey.create(Registries.ITEM,
        DimensionWorksCaveFactory.id("boar_feed"));
    public static final TagKey<net.minecraft.world.level.block.Block> ROOT_BED = TagKey.create(Registries.BLOCK,
        DimensionWorksCaveFactory.id("root_bed"));
    public static RegistryObject<TwilightWorkstationBlock> LEACHER, CONDITIONER;
    public static RegistryObject<BlockEntityType<TwilightWorkstationBlockEntity>> BLOCK_ENTITY;
    public static RegistryObject<MenuType<TwilightWorkstationMenu>> MENU;
    public static RegistryObject<RecipeType<TwilightWorkRecipe>> RECIPE_TYPE;
    public static RegistryObject<RecipeSerializer<TwilightWorkRecipe>> SERIALIZER;
    public static RegistryObject<Item> RESIN, FIBER, HUMUS, FRAME, FILTER;

    private TwilightIndustry() {}

    public static void register() {
        LEACHER = BLOCKS.register("forest_leacher", () -> new TwilightWorkstationBlock(false,
            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.5F).requiresCorrectToolForDrops()));
        CONDITIONER = BLOCKS.register("boar_conditioner", () -> new TwilightWorkstationBlock(true,
            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.5F).requiresCorrectToolForDrops()));
        ITEMS.register("forest_leacher", () -> new BlockItem(LEACHER.get(), new Item.Properties()));
        ITEMS.register("boar_conditioner", () -> new BlockItem(CONDITIONER.get(), new Item.Properties()));
        RESIN = simpleItem("forest_resin");
        FIBER = simpleItem("root_fiber");
        HUMUS = simpleItem("mature_humus");
        FRAME = simpleItem("ironwood_frame");
        FILTER = simpleItem("forest_filter");
        BLOCK_ENTITY = BLOCK_ENTITIES.register("twilight_workstation", () -> BlockEntityType.Builder.of(
            TwilightWorkstationBlockEntity::new, LEACHER.get(), CONDITIONER.get()).build(null));
        MENU = MENUS.register("twilight_workstation", () -> IForgeMenuType.create(TwilightWorkstationMenu::new));
        RECIPE_TYPE = RECIPE_TYPES.register("twilight_work", () -> RecipeType.simple(
            DimensionWorksCaveFactory.id("twilight_work")));
        SERIALIZER = RECIPE_SERIALIZERS.register("twilight_work", TwilightWorkRecipe.Serializer::new);
    }

    private static RegistryObject<Item> simpleItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }
}
