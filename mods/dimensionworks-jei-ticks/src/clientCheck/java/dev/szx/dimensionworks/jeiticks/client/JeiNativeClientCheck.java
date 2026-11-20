package dev.szx.dimensionworks.jeiticks.client;

import com.mojang.logging.LogUtils;
import dev.szx.dimensionworks.jeiticks.DimensionWorksJeiTicks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@JeiPlugin
@EventBusSubscriber(modid = DimensionWorksJeiTicks.MOD_ID, value = Dist.CLIENT)
public final class JeiNativeClientCheck implements IModPlugin {
    public record Diagnostic(int duration) { public int getDuration() { return duration; } }
    private static final RecipeType<Diagnostic> TYPE = RecipeType.create("dimensionworks_jei_ticks", "native_check", Diagnostic.class);
    private static IJeiRuntime runtime;
    private static boolean started;
    private static long deadline;
    @Override public ResourceLocation getPluginUid() { return TYPE.getUid(); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, java.util.List.of(new Diagnostic(43), new Diagnostic(271)));
    }
    @Override public void onRuntimeAvailable(IJeiRuntime available) { runtime = available; }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("dimensionworks.jeiClientCheck")) return;
        var client = Minecraft.getInstance();
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Native JEI client check timed out");
        if (client.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            client.options.onboardingAccessibilityFinished();
            client.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
        }
        if (!started && client.screen instanceof net.minecraft.client.gui.screens.TitleScreen && client.getOverlay() == null) {
            started = true;
            client.options.renderDistance().set(2);
            client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(30);
            client.options.guiScale().set(1);
            client.resizeDisplay();
            client.options.pauseOnLostFocus = false;
            var settings = new net.minecraft.world.level.LevelSettings("DW JEI Check", net.minecraft.world.level.GameType.CREATIVE,
                false, net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(),
                net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
            client.createWorldOpenFlows().createFreshLevel("dw-jei-check-" + System.currentTimeMillis(), settings,
                new net.minecraft.world.level.levelgen.WorldOptions(7, false, false),
                access -> access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                    .getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).createWorldDimensions(), client.screen);
        }
        if (client.player != null && runtime != null && client.getOverlay() == null && !(client.screen instanceof ProbeScreen))
            client.setScreen(new ProbeScreen());
    }

    private static final class Category implements IRecipeCategory<Diagnostic> {
        private final IDrawable icon;
        Category(IGuiHelper gui) { icon = gui.createDrawableItemStack(new ItemStack(Items.CLOCK)); }
        @Override public RecipeType<Diagnostic> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.literal("Native timing scope"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 120; }
        @Override public int getHeight() { return 52; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, Diagnostic recipe, IFocusGroup focus) {
            builder.addInputSlot(1, 1).addItemStack(new ItemStack(Items.COAL));
            builder.addOutputSlot(100, 1).addItemStack(new ItemStack(Items.DIAMOND));
        }
        @Override public void draw(Diagnostic recipe, IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
            require(RecipeDrawContext.ticks() == recipe.duration(), "Category must use its own recipe duration");
            com.simibubi.create.foundation.gui.AllGuiTextures.JEI_ARROW.render(graphics, 45, 2);
        }
        @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, Diagnostic recipe, IFocusGroup focus) {
            builder.addWidget(new IRecipeWidget() {
                @Override public ScreenPosition getPosition() { return new ScreenPosition(0, 22); }
                @Override public void drawWidget(GuiGraphics graphics, double x, double y) {
                    require(RecipeDrawContext.ticks() == recipe.duration(), "Native JEI widget lambda must inherit the recipe duration");
                    com.simibubi.create.foundation.gui.AllGuiTextures.JEI_LONG_ARROW.render(graphics, 20, 0);
                }
            });
            builder.addDrawable(new IDrawable() {
                @Override public int getWidth() { return 20; }
                @Override public int getHeight() { return 20; }
                @Override public void draw(GuiGraphics graphics, int x, int y) {
                    require(RecipeDrawContext.ticks() == recipe.duration(), "Positioned drawable must inherit the recipe duration");
                }
            }, 1, 30);
        }
    }

    private static final class ProbeScreen extends Screen {
        private final java.util.List<IRecipeLayoutDrawable<?>> layouts = new java.util.ArrayList<>();
        private final java.util.List<FuelEntry> fuels;
        private int frames;
        private net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent nativeTooltip;
        private final java.util.List<snownee.jade.api.ui.IElement> jadeElements = new java.util.ArrayList<>();
        ProbeScreen() {
            super(Component.literal("DimensionWorks native JEI validation"));
            var manager = runtime.getRecipeManager();
            var focus = runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
            var category = manager.getRecipeCategory(TYPE);
            for (var recipe : java.util.List.of(new Diagnostic(43), new Diagnostic(271)))
                layouts.add(manager.createRecipeLayoutDrawable(category, recipe, focus).orElseThrow());
            for (String id : java.util.List.of("minecraft:smelting", "create:crushing", mekanism.client.recipe_viewer.type.RecipeViewerRecipeType.ENRICHING.id().toString()))
                addActual(id);
            require(!FuelScope.includes(new ItemStack(Items.FURNACE)), "Vanilla devices must stay outside fuel scope");
            require(!FuelScope.includes(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mekanism:enrichment_chamber")))),
                "Mekanism devices must stay outside fuel scope");
            fuels = FuelCatalog.entries(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:blaze_burner"))));
            require(!fuels.isEmpty(), "Native Create fuel catalog must load");
            var nativeEvent = new net.neoforged.neoforge.client.event.RenderTooltipEvent.GatherComponents(
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:blaze_burner"))),
                180, 180, new java.util.ArrayList<>(), 148);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(nativeEvent);
            require(nativeEvent.getTooltipElements().size() == 1, "Registered native gather event must add one fuel component");
            var data = (FuelTooltipData) nativeEvent.getTooltipElements().getFirst().right().orElseThrow();
            require(data.entries().size() == 1 && data.pages() == fuels.size() && data.width() == 148,
                "Small screen must page and bound each fuel card");
            nativeTooltip = net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(data);
            require(nativeTooltip instanceof FuelTooltipComponent, "Actual native tooltip factory must select the fuel renderer");

            var diesel = FuelCatalog.entries(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("createdieselgenerators:diesel_engine"))));
            require(!diesel.isEmpty(), "Native Diesel fuel registry must resolve");
            for (String device : java.util.List.of("create_enchantment_industry:blaze_enchanter",
                    "draconicevolution:reactor_core", "draconicevolution:generator", "draconicevolution:advanced_dislocator")) {
                var id = ResourceLocation.parse(device);
                require(BuiltInRegistries.ITEM.containsKey(id), "Expected retained native device " + device);
                var entries = FuelCatalog.entries(new ItemStack(BuiltInRegistries.ITEM.get(id)));
                require(entries.size() > (device.startsWith("create_enchantment_industry:") ? 1 : 0),
                    "Native optional fuel API must resolve actual fuels for " + device);
                LogUtils.getLogger().info("JEI_FUEL_PROVIDER: {} entries={}", device, entries.size());
            }
            require(fuels.stream().anyMatch(entry -> entry.icon().stack().is(Items.LAVA_BUCKET)),
                "Native liquid burner fuel must retain its lava fuel");

            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:blaze_burner"));
            var jade = snownee.jade.impl.WailaClientRegistration.instance();
            var providers = jade.getBlockProviders(block, provider -> provider.getUid().equals(
                ResourceLocation.parse("dimensionworks_jei_ticks:fuel_consumption")));
            require(providers.size() == 1, "Actual Jade plugin discovery must register exactly one fuel provider");
            var client = Minecraft.getInstance();
            var pos = client.player.blockPosition();
            var accessor = jade.blockAccessor().level(client.level).player(client.player)
                .blockState(block.defaultBlockState()).blockEntity(() -> null)
                .hit(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                    net.minecraft.core.Direction.UP, pos, false)).build();
            var tooltip = new snownee.jade.impl.Tooltip();
            providers.getFirst().appendTooltip(tooltip, accessor, snownee.jade.impl.config.PluginConfig.INSTANCE);
            require(tooltip.size() > 0, "Registered native Jade provider must create its fuel tooltip");
            for (int line = 0; line < tooltip.size(); line++)
                for (var align : snownee.jade.api.ui.IElement.Align.values()) jadeElements.addAll(tooltip.get(line, align));
            require(!jadeElements.isEmpty(), "Jade tooltip must contain rendered elements");

        }
        @SuppressWarnings({"rawtypes", "unchecked"}) private void addActual(String id) {
            var manager = runtime.getRecipeManager();
            RecipeType type = manager.getRecipeType(ResourceLocation.parse(id)).orElseThrow();
            IRecipeCategory category = manager.getRecipeCategory(type);
            Object recipe = manager.createRecipeLookup(type).get().findFirst().orElseThrow();
            long duration = RecipeTimeResolver.resolve(category, recipe);
            require(duration > 0, "Actual " + id + " recipe must expose its real duration");
            if (id.startsWith("mekanism:")) require(duration == 200, "Enrichment must use the native 200-tick base");
            layouts.add((IRecipeLayoutDrawable<?>) manager.createRecipeLayoutDrawable(category, recipe,
                runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow());
            LogUtils.getLogger().info("JEI_ACTUAL_RECIPE: {} duration={}", id, duration);
        }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
            graphics.fill(0, 0, width, height, 0xFF18212E);
            int row = 12;
            for (var layout : layouts) {
                layout.setPosition(12, row);
                layout.drawRecipe(graphics, -1, -1);
                require(RecipeDrawContext.ticks() == 0, "Recipe scope must be cleared after each native layout");
                row += layout.getRect().getHeight() + 10;
            }
            var content = new FuelTooltipComponent(new FuelTooltipData(fuels.subList(0, Math.min(2, fuels.size())), 0, 1, 230));
            content.renderImage(minecraft.font, width - 240, 12, graphics);
            nativeTooltip.renderImage(minecraft.font, width - 400, 12, graphics);
            int jadeY = 180;
            for (var element : jadeElements) {
                var size = element.getSize();
                require(size.x > 0 && size.y > 0, "Native Jade element must have a valid layout size");
                element.render(graphics, width - 240, jadeY, size.x, size.y);
                jadeY += (int) size.y + 2;
            }
            if (++frames == 20) {
                graphics.flush();
                try (var screenshot = net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                    var file = new java.io.File(minecraft.gameDirectory, "screenshots/native-jei-check.png");
                    file.getParentFile().mkdirs();
                    screenshot.writeToFile(file);
                } catch (java.io.IOException exception) {
                    throw new IllegalStateException("Could not save native UI verification image", exception);
                }
                LogUtils.getLogger().info("JEI NATIVE CLIENT CHECK PASSED: category/widget/drawable isolation, actual vanilla/Create/Mekanism layouts, fuel components and registered Jade tooltip rendering");
                minecraft.stop();
            }
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
