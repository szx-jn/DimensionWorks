package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.cavefactory.api.AmpouleExpiry;
import dev.szx.dimensionworks.mekstress.memory.ChannelStockSavedData;
import dev.szx.dimensionworks.mekstress.memory.MatterBallPacking;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksMekStress.MOD_ID)
@PrefixGameTestTemplate(false)
public class CrossDimensionTransportGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void inputAloneReadsRealMotorAndConsumesStress(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var motorPos = pos.west();
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(DimensionWorksMekStress.MOD_ID, "cross_dimension_transmitter_tier1"));
        helper.setBlock(motorPos, net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "creative_motor")).defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.EAST));
        helper.setBlock(pos, block.defaultBlockState()
            .setValue(com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock.HORIZONTAL_AXIS, net.minecraft.core.Direction.Axis.X));
        helper.runAfterDelay(20, () -> {
            var endpoint = (dev.szx.dimensionworks.mekstress.blockentity.CrossDimensionTransportBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(Math.abs(endpoint.getSpeed()) == 16, "A lone input must read the motor's actual 16 RPM");
            endpoint.setStressPerRpm(1250.5F);
            helper.assertTrue(endpoint.powerNetwork().members.get(endpoint) == 1250.5F,
                "Changing coefficient must update the real network member immediately");
            helper.assertTrue(endpoint.calculateStressApplied() * Math.abs(endpoint.getSpeed()) == 20008,
                "Input must consume the configured SU even without an output");
            helper.destroyBlock(motorPos);
        });
        helper.runAfterDelay(40, () -> {
            var endpoint = (dev.szx.dimensionworks.mekstress.blockentity.CrossDimensionTransportBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(endpoint.getSpeed() == 0, "Removing the physical source must stop the input");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void allTransportStatesHaveMatchingModelVariants(GameTestHelper helper) {
        var gson = new com.google.gson.Gson();
        for (String path : java.util.List.of("cross_dimension_transmitter_tier1", "cross_dimension_transmitter_tier2",
            "cross_dimension_transmitter_tier3", "matter_ball_unpacker")) {
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(DimensionWorksMekStress.MOD_ID, path);
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
            try (var input = CrossDimensionTransportGameTests.class.getResourceAsStream(
                "/assets/" + id.getNamespace() + "/blockstates/" + path + ".json")) {
                var variants = gson.fromJson(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8),
                    com.google.gson.JsonObject.class).getAsJsonObject("variants");
                for (var state : block.getStateDefinition().getPossibleStates()) {
                    String key = state.getValues().entrySet().stream().map(entry -> entry.getKey().getName() + "="
                        + entry.getValue().toString().toLowerCase(java.util.Locale.ROOT))
                        .sorted().collect(java.util.stream.Collectors.joining(","));
                    helper.assertTrue(variants.has(key), "Missing placed model variant for " + id + "[" + key + "]");
                }
            } catch (java.io.IOException exception) { throw new RuntimeException(exception); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quantumBridgeRestrictionMixinApplies(GameTestHelper helper) {
        try {
            Class.forName("appeng.me.cluster.implementations.QuantumCluster", false,
                CrossDimensionTransportGameTests.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("AE2 QuantumCluster must be available for the restriction mixin", exception);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void matterBallPackingSplitsLargeFluidStock(GameTestHelper helper) {
        int originalAmount = 50_000;
        var stock = new ChannelStockSavedData.StockSnapshot(
            List.of(), List.of(new ChannelStockSavedData.FluidSnapshot(
                new FluidStack(Fluids.WATER, originalAmount), "minecraft:overworld")),
            originalAmount, 0, 0);
        List<net.minecraft.world.item.ItemStack> balls = MatterBallPacking.pack(stock, helper.getLevel().registryAccess());
        helper.assertTrue(balls != null && balls.size() == 4,
            "50 B of one fluid must fit into four Matter Balls");
        int packedAmount = 0;
        for (var ball : balls) {
            var entries = ball.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompound("MatterBall").getList("Fluids", net.minecraft.nbt.Tag.TAG_COMPOUND);
            helper.assertTrue(entries.size() == 1, "Each ball contains one split fluid stack");
            FluidStack fluid = FluidStack.parse(helper.getLevel().registryAccess(), entries.getCompound(0).getCompound("Stack")).orElseThrow();
            helper.assertTrue(fluid.getAmount() <= 16_000, "No Matter Ball may exceed 16 B of fluid");
            packedAmount += fluid.getAmount();
        }
        helper.assertTrue(packedAmount == originalAmount, "Packing must conserve the full fluid amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void channelStoresAmpoulesThroughCaveFactoryApi(GameTestHelper helper) {
        var ampouleId = ResourceLocation.parse("dimensionworks_cave_factory:fluid_ampoule");
        var ampouleItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ampouleId);
        ItemStack ampoule = new ItemStack(ampouleItem);
        var fluidId = ResourceLocation.parse("dimensionworks_cave_factory:abyssal_brine");
        var brine = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(fluidId);
        CompoundTag legacyFluid = new CompoundTag();
        legacyFluid.putString("FluidName", net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(brine).toString());
        legacyFluid.putInt("Amount", 250);
        CompoundTag legacyData = new CompoundTag();
        legacyData.put("Fluid", legacyFluid);
        legacyData.putString("OriginDimension", "alex_caves_dimensions:abyssal_chasm");
        ampoule.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

        ChannelStockSavedData stock = new ChannelStockSavedData(helper.getLevel().registryAccess());
        stock.tickChannel(1, helper.getLevel().getGameTime(), 1);
        helper.assertTrue(stock.insertItems(1, "alex_caves_dimensions:abyssal_chasm", ampoule, false) == 1,
            "Filled ampoule should be admitted through the public contract");
        ItemStack stored = stock.snapshot(1).items().getFirst().stack();
        helper.assertTrue(AmpouleExpiry.isFilled(stored), "Channel prototype retains the ampoule component");
        CompoundTag metadata = stored.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        helper.assertTrue(metadata.getLong("ExpiresAtEpochMs") > System.currentTimeMillis(),
            "Channel preparation writes the existing expiry key");
        helper.assertTrue(stored.getCount() == 1, "Channel storage keeps a count-one item prototype");
        helper.succeed();
    }
}
