package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.memory.ChannelStockSavedData;
import dev.szx.dimensionworks.mekstress.memory.MatterBallPacking;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksMekStress.MOD_ID)
@PrefixGameTestTemplate(false)
public class CrossDimensionTransportGameTests {
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
        List<net.minecraft.world.item.ItemStack> balls = MatterBallPacking.pack(stock);
        helper.assertTrue(balls != null && balls.size() == 4,
            "50 B of one fluid must fit into four Matter Balls");
        int packedAmount = 0;
        for (var ball : balls) {
            var entries = ball.getTag().getCompound("MatterBall").getList("Fluids", net.minecraft.nbt.Tag.TAG_COMPOUND);
            helper.assertTrue(entries.size() == 1, "Each ball contains one split fluid stack");
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(entries.getCompound(0).getCompound("Stack"));
            helper.assertTrue(fluid.getAmount() <= 16_000, "No Matter Ball may exceed 16 B of fluid");
            packedAmount += fluid.getAmount();
        }
        helper.assertTrue(packedAmount == originalAmount, "Packing must conserve the full fluid amount");
        helper.succeed();
    }
}
