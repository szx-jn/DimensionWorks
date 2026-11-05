package dev.szx.dimensionworks.cavefactory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "dimensionworks_cave_factory", value = Dist.CLIENT)
public final class CrystallizedFluidClientCheck {
    private static boolean done;
    @SubscribeEvent public static void check(TickEvent.ClientTickEvent event) {
        if (done || !Boolean.getBoolean("dimensionworks.verifyCrystallizedFluids") || event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        if (client.screen == null || client.getOverlay() != null) return;
        done = true;
        int count = 0;
        for (var entry : ForgeRegistries.FLUIDS.getEntries()) {
            var id = entry.getKey().location();
            if (!id.getNamespace().equals("dimensionworks_cave_factory") || !id.getPath().startsWith("crystallized_molten_")) continue;
            var fluid = entry.getValue();
            if (!fluid.defaultFluidState().isSource()) continue;
            require(fluid.defaultFluidState().createLegacyBlock().getBlock() instanceof LiquidBlock, "Not a liquid: " + id);
            require(fluid.getBucket() instanceof BucketItem, "Missing bucket: " + id);
            require(!ForgeRegistries.ITEMS.containsKey(id), "Retired solid item still registered: " + id);
            var tank = new net.minecraftforge.fluids.capability.templates.FluidTank(1000);
            require(tank.fill(new net.minecraftforge.fluids.FluidStack(fluid, 1000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "Cannot fill tank: " + id);
            require(tank.drain(1000, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000,
                "Cannot drain tank: " + id);
            var extensions = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
            require(extensions.getStillTexture().equals(new ResourceLocation(id.getNamespace(), "block/" + id.getPath() + "_still")),
                "Fluid references wrong still sprite: " + id);
            require(extensions.getFlowingTexture().equals(new ResourceLocation(id.getNamespace(), "block/" + id.getPath() + "_flow")),
                "Fluid references wrong flowing sprite: " + id);
            for (String kind : java.util.List.of("still", "flow")) {
                var texture = new ResourceLocation(id.getNamespace(), "block/" + id.getPath() + "_" + kind);
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
        System.out.println("CRYSTALLIZED FLUID CHECK PASSED: 21 source fluids, 42 animated sprites, buckets, tank I/O, zero solid items");
        client.stop();
    }
    private static void require(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
