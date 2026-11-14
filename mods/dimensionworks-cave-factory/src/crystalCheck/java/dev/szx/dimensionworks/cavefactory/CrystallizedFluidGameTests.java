package dev.szx.dimensionworks.cavefactory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("dimensionworks_cave_factory")
@PrefixGameTestTemplate(false)
public final class CrystallizedFluidGameTests {
    @GameTest(template = "fluid_grid", timeoutTicks = 700)
    public static void allTwentyOnePlacedMoltenSourcesCrystallize(GameTestHelper helper) {
        List<ResourceLocation> metals = ForgeRegistries.FLUIDS.getKeys().stream()
            .filter(id -> id.getNamespace().equals("dimensionworks") && id.getPath().startsWith("molten_"))
            .filter(id -> ForgeRegistries.FLUIDS.getValue(id) instanceof FlowingFluid fluid
                && fluid.getSource() == fluid).sorted().toList();
        helper.assertTrue(metals.size() == 21, "Test every registered molten metal, expected 21: " + metals);
        for (int i = 0; i < metals.size(); i++) {
            BlockPos pos = gridPos(i);
            contain(helper, pos);
            helper.setBlock(pos, fluid(metals.get(i)).defaultFluidState().createLegacyBlock());
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(pos)).getType() == fluid(metals.get(i)),
                "Placed block must contain its own metal: " + metals.get(i) + ", block=" + helper.getBlockState(pos));
        }
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < metals.size(); i++)
                helper.assertTrue(MoltenCrystallizationData.get(helper.getLevel()).positions()
                    .containsKey(helper.absolutePos(gridPos(i)).asLong()),
                    "Actual placement must register exposure: " + metals.get(i));
        });
        helper.runAfterDelay(590, () -> {
            for (int i = 0; i < metals.size(); i++)
                helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(gridPos(i))).getType()
                    == fluid(metals.get(i)), "Do not crystallize before 600 ticks: " + metals.get(i));
        });
        helper.runAfterDelay(650, () -> {
            for (int i = 0; i < metals.size(); i++) {
                BlockPos pos = gridPos(i);
                var actual = helper.getLevel().getFluidState(helper.absolutePos(pos));
                helper.assertTrue(actual.getType() == crystal(metals.get(i).getPath().substring(7))
                    && actual.isSource(), "Expected crystallized source: " + metals.get(i) + ", actual=" + actual);
                helper.assertTrue(helper.getBlockState(pos).getBlock() instanceof LiquidBlock,
                    "Crystallization must retain a liquid block: " + metals.get(i));
            }
            helper.succeed();
        });
    }

    @GameTest(template = "fluid_grid", timeoutTicks = 1100)
    public static void realBucketRequiresContinuousSkyExposure(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 200, 2);
        contain(helper, pos);
        helper.setBlock(pos.above(), Blocks.STONE);
        pour(helper, pos);
        helper.runAfterDelay(400, () -> {
            assertMolten(helper, pos);
            helper.setBlock(pos.above(), Blocks.AIR);
        });
        helper.runAfterDelay(950, () -> assertMolten(helper, pos));
        helper.runAfterDelay(1050, () -> {
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(pos)).getType() == crystal("iron"),
                "Bucket fluid must crystallize after 600 continuously uncovered ticks");
            helper.succeed();
        });
    }

    @GameTest(template = "fluid_grid", timeoutTicks = 1250)
    public static void replacingRealBucketResetsExposure(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 200, 2);
        contain(helper, pos);
        pour(helper, pos);
        helper.runAfterDelay(500, () -> {
            helper.setBlock(pos, Blocks.AIR);
            pour(helper, pos);
        });
        helper.runAfterDelay(1050, () -> assertMolten(helper, pos));
        helper.runAfterDelay(1170, () -> {
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(pos)).getType() == crystal("iron"),
                "A replacement bucket needs its own 600-tick exposure");
            helper.succeed();
        });
    }

    @GameTest(template = "fluid_grid", timeoutTicks = 700)
    public static void flowingMetalRemainsFlowingAfterCrystallization(GameTestHelper helper) {
        BlockPos source = new BlockPos(2, 200, 2);
        BlockPos flow = source.east();
        contain(helper, source);
        contain(helper, flow);
        helper.setBlock(source, fluid(new ResourceLocation("dimensionworks", "molten_iron"))
            .defaultFluidState().createLegacyBlock());
        helper.setBlock(flow, ((FlowingFluid) fluid(new ResourceLocation("dimensionworks", "molten_iron")))
            .getFlowing(7, false).createLegacyBlock());
        helper.runAfterDelay(650, () -> {
            var actual = helper.getLevel().getFluidState(helper.absolutePos(flow));
            helper.assertTrue(actual.getType() == ((FlowingFluid) crystal("iron")).getFlowing()
                && !actual.isSource() && actual.getAmount() == 7,
                "The flowing segment must retain its level after crystallization: " + actual);
            helper.succeed();
        });
    }

    private static BlockPos gridPos(int i) { return new BlockPos(1 + i % 7 * 3, 200, 1 + i / 7 * 3); }
    private static Fluid fluid(ResourceLocation id) { return ForgeRegistries.FLUIDS.getValue(id); }
    private static Fluid crystal(String metal) {
        return fluid(new ResourceLocation("dimensionworks_cave_factory", "crystallized_molten_" + metal));
    }
    private static void assertMolten(GameTestHelper helper, BlockPos pos) {
        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(pos)).getType()
            == fluid(new ResourceLocation("dimensionworks", "molten_iron")),
            "Metal must remain molten before continuous exposure completes; actual="
                + helper.getLevel().getFluidState(helper.absolutePos(pos)) + ", block=" + helper.getBlockState(pos));
    }
    private static void pour(GameTestHelper helper, BlockPos pos) {
        ItemStack bucket = new ItemStack(ForgeRegistries.ITEMS.getValue(
            new ResourceLocation("dimensionworks", "molten_iron_bucket")));
        helper.assertTrue(((BucketItem) bucket.getItem()).emptyContents(
            null, helper.getLevel(), helper.absolutePos(pos), null, bucket), "Actual molten bucket must place its fluid");
        assertMolten(helper, pos);
    }
    private static void contain(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.BARRIER);
        for (Direction face : Direction.Plane.HORIZONTAL) helper.setBlock(pos.relative(face), Blocks.BARRIER);
        BlockPos absolute = helper.absolutePos(pos);
        for (int y = absolute.getY() + 1; y < helper.getLevel().getMaxBuildHeight(); y++)
            helper.getLevel().setBlock(new BlockPos(absolute.getX(), y, absolute.getZ()), Blocks.AIR.defaultBlockState(), 3);
    }
}
