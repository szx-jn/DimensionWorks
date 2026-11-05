package dev.szx.dimensionworks.cavefactory;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("dimensionworks_cave_factory")
@PrefixGameTestTemplate(false)
public final class CrystallizedFluidGameTests {
    @GameTest(template = "empty", timeoutTicks = 700)
    public static void exposedMoltenSourceBecomesLiquidNotSolid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 200, 1);
        helper.setBlock(pos.below(), Blocks.BARRIER);
        for (var face : net.minecraft.core.Direction.Plane.HORIZONTAL) helper.setBlock(pos.relative(face), Blocks.BARRIER);
        var original = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("dimensionworks", "molten_iron"));
        helper.setBlock(pos, original.defaultFluidState().createLegacyBlock());
        var absolute = helper.absolutePos(pos);
        // GameTest surrounds its structures with barriers; open the column for real exposure.
        for (int y = absolute.getY() + 1; y < helper.getLevel().getMaxBuildHeight(); y++)
            helper.getLevel().setBlock(new BlockPos(absolute.getX(), y, absolute.getZ()), Blocks.AIR.defaultBlockState(), 3);
        MoltenCrystallizationData.get(helper.getLevel()).positions().put(absolute.asLong(), helper.getLevel().getGameTime());
        helper.runAfterDelay(650, () -> {
            var actual = helper.getLevel().getFluidState(absolute);
            var expected = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("dimensionworks_cave_factory", "crystallized_molten_iron"));
            helper.assertTrue(actual.getType() == expected && actual.isSource(), "Exposure must produce a crystallized source fluid; actual=" + ForgeRegistries.FLUIDS.getKey(actual.getType())
                + ", sky=" + helper.getLevel().canSeeSky(absolute.above()) + ", time=" + helper.getLevel().getGameTime()
                + ", tracked=" + MoltenCrystallizationData.get(helper.getLevel()).positions().get(absolute.asLong()));
            helper.assertTrue(helper.getBlockState(pos).getBlock() instanceof LiquidBlock, "Exposure must never create a solid block");
            helper.succeed();
        });
    }
}
