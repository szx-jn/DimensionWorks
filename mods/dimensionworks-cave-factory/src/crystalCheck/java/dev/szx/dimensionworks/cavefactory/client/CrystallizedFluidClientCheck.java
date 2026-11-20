package dev.szx.dimensionworks.cavefactory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@EventBusSubscriber(modid = "dimensionworks_cave_factory", value = Dist.CLIENT)
public final class CrystallizedFluidClientCheck {
    private static boolean done;
    private static boolean launched;
    private static final long DEADLINE = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(2);
    @SubscribeEvent public static void check(ClientTickEvent.Post event) {
        if (done || !Boolean.getBoolean("dimensionworks.verifyCrystallizedFluids")) return;
        var client = Minecraft.getInstance();
        require(System.nanoTime() < DEADLINE, "Timed out waiting for isolated client world: " + client.screen);
        if (client.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            client.options.onboardingAccessibilityFinished();
            client.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
        }
        if (!launched && client.screen instanceof net.minecraft.client.gui.screens.TitleScreen && client.getOverlay() == null) {
            launched = true;
            client.options.renderDistance().set(2); client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(30); client.options.pauseOnLostFocus = false;
            var settings = new net.minecraft.world.level.LevelSettings("DW Crystal Check", net.minecraft.world.level.GameType.CREATIVE,
                false, net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(),
                net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
            client.createWorldOpenFlows().createFreshLevel("dw-crystal-check", settings,
                new net.minecraft.world.level.levelgen.WorldOptions(7, false, false),
                access -> access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                    .getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).createWorldDimensions(), client.screen);
        }
        if (client.player == null || client.getConnection() == null || client.getOverlay() != null) return;
        done = true;
        int count = 0;
        for (var entry : net.minecraft.core.registries.BuiltInRegistries.FLUID.entrySet()) {
            var id = entry.getKey().location();
            if (!id.getNamespace().equals("dimensionworks_cave_factory") || !id.getPath().startsWith("crystallized_molten_")) continue;
            var fluid = entry.getValue();
            if (!fluid.defaultFluidState().isSource()) continue;
            require(fluid.defaultFluidState().createLegacyBlock().getBlock() instanceof LiquidBlock, "Not a liquid: " + id);
            require(fluid.getBucket() instanceof BucketItem, "Missing bucket: " + id);
            require(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id), "Retired solid item still registered: " + id);
            var tank = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1000);
            require(tank.fill(new net.neoforged.neoforge.fluids.FluidStack(fluid, 1000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "Cannot fill tank: " + id);
            require(tank.drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000,
                "Cannot drain tank: " + id);
            var extensions = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
            require(extensions.getStillTexture().equals(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/generated/" + id.getPath() + "_still")),
                "Fluid references wrong still sprite: " + id);
            require(extensions.getFlowingTexture().equals(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/generated/" + id.getPath() + "_flow")),
                "Fluid references wrong flowing sprite: " + id);
            for (String kind : java.util.List.of("still", "flow")) {
                var texture = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/generated/" + id.getPath() + "_" + kind);
                var sprite = client.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
                require(sprite.contents().name().equals(texture), "Missing stitched texture: " + texture);
                require(sprite.contents().getUniqueFrames().count() > 1, "Lost molten animation: " + texture);
                var image = sprite.contents().getOriginalImage();
                require(image.getHeight() > image.getWidth(), "Not an animation strip: " + texture);
                if (id.getPath().endsWith("iron") && kind.equals("still")) {
                    try { image.writeToFile(java.nio.file.Path.of("crystallized_iron_preview.png")); }
                    catch (java.io.IOException error) { throw new RuntimeException(error); }
                }
            }
            count++;
        }
        require(count == 21, "Expected 21 registered liquid metals, got " + count);
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS, true,
            client.getConnection().registryAccess());
        var shared = dev.szx.dimensionworks.cavefactory.registry.CFItems.CREATIVE_TAB.get();
        int dedicatedCount = 0;
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (!packOwned(id)) continue;
            require(shared.getDisplayItems().stream().anyMatch(stack -> stack.is(item)), "Dedicated item missing from shared tab: " + id);
            require(net.minecraft.world.item.CreativeModeTabs.searchTab().getDisplayItems().stream().anyMatch(stack -> stack.is(item)), "Dedicated item missing from global search: " + id);
            dedicatedCount++;
        }
        for (var tab : net.minecraft.world.item.CreativeModeTabs.tabs()) {
            if (tab == shared || tab == net.minecraft.world.item.CreativeModeTabs.searchTab()) continue;
            for (var stack : tab.getDisplayItems()) require(!packOwned(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())), "Dedicated item leaked into other tab");
        }
        dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory.LOGGER.info("SHARED CREATIVE TAB CHECK PASSED: " + dedicatedCount + " dedicated items and global search, no duplicate tabs");
        dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory.LOGGER.info("CRYSTALLIZED FLUID CHECK PASSED: 21 source fluids, 42 animated sprites, buckets, tank I/O, zero solid items");
        client.stop();
    }
    private static boolean packOwned(ResourceLocation id) {
        return id.getNamespace().equals("dimensionworks") || id.getNamespace().startsWith("dimensionworks_") || id.getNamespace().equals("kubejs");
    }
    private static void require(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
