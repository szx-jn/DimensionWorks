package dev.szx.dimensionworks.mekstress;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.world.level.block.Blocks;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import mekanism.api.Action;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.world.item.ItemStack;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import dev.szx.dimensionworks.mekstress.integration.gas.GasFluid;
import dev.szx.dimensionworks.mekstress.integration.gas.GasFluidHandler;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismGases;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(DimensionWorksMekStress.MOD_ID)
@PrefixGameTestTemplate(false)
public class GasTransportGameTests {
    @GameTest(template = "empty", timeoutTicks = 250)
    public static void poweredPumpTransportsGas(GameTestHelper helper) {
        var basinBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create:basin"));
        helper.setBlock(new BlockPos(1, 2, 2), basinBlock.defaultBlockState());
        helper.setBlock(new BlockPos(4, 2, 2), basinBlock.defaultBlockState());
        helper.setBlock(new BlockPos(3, 2, 2), ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create:fluid_pipe"))
            .defaultBlockState().setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST), true)
            .setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST), true));
        helper.setBlock(new BlockPos(2, 2, 2), ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create:mechanical_pump"))
            .defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(2, 2, 1), ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create:cogwheel"))
            .defaultBlockState().setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        helper.setBlock(new BlockPos(1, 2, 1), ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create:creative_motor"))
            .defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        IGasHandler source = helper.getBlockEntity(new BlockPos(1, 2, 2))
            .getCapability(Capabilities.GAS_HANDLER, Direction.EAST).orElseThrow(IllegalStateException::new);
        IGasHandler destination = helper.getBlockEntity(new BlockPos(4, 2, 2))
            .getCapability(Capabilities.GAS_HANDLER, Direction.WEST).orElseThrow(IllegalStateException::new);
        source.setChemicalInTank(0, new GasStack(MekanismGases.HYDROGEN, 10000));
        helper.runAfterDelay(1, () -> ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)
            helper.getBlockEntity(new BlockPos(1, 2, 1))).generatedSpeed.setValue(256));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(destination.getChemicalInTank(1).getAmount() > 0, "Powered pump must deliver gas to basin input");
            helper.assertTrue(destination.getChemicalInTank(1).getType() == MekanismGases.HYDROGEN.get(), "Pump must preserve gas identity");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void basinAdapterAndOpenPipe(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create:basin")).defaultBlockState());
        BlockEntity basin = helper.getBlockEntity(relative);
        IGasHandler gas = basin.getCapability(Capabilities.GAS_HANDLER, Direction.EAST).orElseThrow(IllegalStateException::new);
        gas.setChemicalInTank(0, new GasStack(MekanismGases.HYDROGEN, 1000));
        var bridgeCapability = GasFluidHandler.capability(basin, ForgeCapabilities.FLUID_HANDLER, Direction.EAST);
        IFluidHandler bridge = bridgeCapability.orElseThrow(IllegalStateException::new);
        helper.assertTrue(bridgeCapability == GasFluidHandler.capability(basin, ForgeCapabilities.FLUID_HANDLER, Direction.EAST),
            "Repeated endpoint lookup must reuse its capability");
        helper.assertTrue(bridgeCapability == GasFluidHandler.capability(basin, ForgeCapabilities.FLUID_HANDLER, Direction.WEST),
            "Shared capabilities on multiple sides must retain handler identity");
        FluidStack hydrogen = GasFluid.encode(new GasStack(MekanismGases.HYDROGEN, 250));
        FluidStack oxygen = GasFluid.encode(new GasStack(MekanismGases.OXYGEN, 250));
        helper.assertTrue(!hydrogen.isFluidEqual(oxygen), "Gas identities must remain distinct");
        ItemStack gasContainer = new ItemStack(MekanismBlocks.BASIC_CHEMICAL_TANK.asItem());
        IGasHandler container = gasContainer.getCapability(Capabilities.GAS_HANDLER).orElseThrow(IllegalStateException::new);
        helper.assertTrue(container.insertChemical(new GasStack(MekanismGases.HYDROGEN, 250), Action.EXECUTE).isEmpty(), "Fill gas filter container");
        FilterItemStack filter = FilterItemStack.of(gasContainer);
        helper.assertTrue(filter.test(helper.getLevel(), hydrogen, false), "Gas container filter must match its gas");
        helper.assertTrue(!filter.test(helper.getLevel(), oxygen, false), "Gas filter must distinguish gas types even without NBT matching");
        helper.assertTrue(container.getChemicalInTank(0).getAmount() == 250, "Filter inspection must not consume gas");
        helper.assertTrue(FluidPropagator.hasFluidCapability(helper.getLevel(), helper.absolutePos(relative), Direction.EAST), "Pipe must recognize endpoint");
        helper.assertTrue(bridge.drain(hydrogen, IFluidHandler.FluidAction.SIMULATE).getAmount() == 250, "Simulated gas extraction");
        helper.assertTrue(gas.getChemicalInTank(0).getAmount() == 1000, "Simulation must not extract gas");
        helper.assertTrue(bridge.drain(oxygen, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Wrong gas must not extract");
        FluidStack extracted = bridge.drain(hydrogen, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(extracted.getAmount() == 250 && gas.getChemicalInTank(0).getAmount() == 750, "Gas extraction must conserve amount");
        helper.assertTrue(bridge.fill(extracted, IFluidHandler.FluidAction.SIMULATE) == 250 && gas.getChemicalInTank(1).isEmpty(), "Simulated insertion");
        helper.assertTrue(bridge.fill(extracted, IFluidHandler.FluidAction.EXECUTE) == 250 && gas.getChemicalInTank(1).getAmount() == 250, "Gas insertion into basin input");
        BlockPos pipePos = helper.absolutePos(new BlockPos(4, 2, 2));
        OpenEndedPipe pipe = new OpenEndedPipe(new BlockFace(pipePos, Direction.EAST));
        pipe.manageSource(helper.getLevel());
        IFluidHandler outlet = pipe.provideHandler().orElseThrow(IllegalStateException::new);
        helper.assertTrue(outlet.fill(hydrogen, IFluidHandler.FluidAction.SIMULATE) == 250, "Open outlet accepts simulated gas");
        helper.assertTrue(outlet.fill(hydrogen, IFluidHandler.FluidAction.EXECUTE) == 250, "Open outlet vents gas");
        helper.assertTrue(helper.getLevel().getBlockState(pipePos.east()).isAir(), "Vented gas must not place a block");
        FluidFillingBehaviour hose = new FluidFillingBehaviour((SmartBlockEntity) basin);
        helper.assertTrue(hose.tryDeposit(GasFluid.SOURCE.get(), pipePos.east(), true), "Hose must simulate venting");
        helper.assertTrue(hose.tryDeposit(GasFluid.SOURCE.get(), pipePos.east(), false), "Hose must vent gas");
        helper.assertTrue(helper.getLevel().getBlockState(pipePos.east()).isAir(), "Hose must not place gas");
        helper.getLevel().setBlockAndUpdate(pipePos.east(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(outlet.fill(hydrogen, IFluidHandler.FluidAction.EXECUTE) == 0, "Blocked pipe must not vent");
        helper.assertTrue(!hose.tryDeposit(GasFluid.SOURCE.get(), pipePos.east(), false), "Blocked hose must not vent");
        helper.assertTrue(helper.getLevel().getBlockState(pipePos.east()).is(Blocks.STONE), "Gas must not replace a solid block");
        basin.setRemoved();
        helper.assertTrue(!bridgeCapability.isPresent(), "Bridge must invalidate with its endpoint");
        helper.succeed();
    }
}
