package dev.szx.dimensionworks.mekstress;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.world.level.block.Blocks;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import mekanism.api.Action;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.world.item.ItemStack;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import dev.szx.dimensionworks.mekstress.integration.BasinGasHandler;
import dev.szx.dimensionworks.mekstress.integration.gas.GasFluid;
import dev.szx.dimensionworks.mekstress.integration.gas.GasFluidHandler;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismChemicals;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.BuiltInRegistries;

@GameTestHolder(DimensionWorksMekStress.MOD_ID)
@PrefixGameTestTemplate(false)
public class GasTransportGameTests {
    @GameTest(template = "empty", timeoutTicks = 250)
    public static void poweredPumpTransportsGas(GameTestHelper helper) {
        var basinBlock = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:basin"));
        helper.setBlock(new BlockPos(1, 2, 2), basinBlock.defaultBlockState());
        helper.setBlock(new BlockPos(4, 2, 2), basinBlock.defaultBlockState());
        helper.setBlock(new BlockPos(3, 2, 2), BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:fluid_pipe"))
            .defaultBlockState().setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST), true)
            .setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST), true));
        helper.setBlock(new BlockPos(2, 2, 2), BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:mechanical_pump"))
            .defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(2, 2, 1), BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:cogwheel"))
            .defaultBlockState().setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        helper.setBlock(new BlockPos(1, 2, 1), BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:creative_motor"))
            .defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        IChemicalHandler source = helper.getLevel().getCapability(Capabilities.CHEMICAL.block(),
            helper.absolutePos(new BlockPos(1, 2, 2)), Direction.EAST);
        IChemicalHandler destination = helper.getLevel().getCapability(Capabilities.CHEMICAL.block(),
            helper.absolutePos(new BlockPos(4, 2, 2)), Direction.WEST);
        source.setChemicalInTank(0, new ChemicalStack(MekanismChemicals.HYDROGEN, 10000));
        helper.runAfterDelay(1, () -> ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)
            helper.getBlockEntity(new BlockPos(1, 2, 1))).generatedSpeed.setValue(256));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(destination.getChemicalInTank(1).getAmount() > 0, "Powered pump must deliver gas to basin input");
            helper.assertTrue(destination.getChemicalInTank(1).getChemical() == MekanismChemicals.HYDROGEN.get(), "Pump must preserve gas identity");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void basinAdapterAndOpenPipe(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative, BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:basin")).defaultBlockState());
        BlockEntity basin = helper.getBlockEntity(relative);
        IChemicalHandler gas = helper.getLevel().getCapability(Capabilities.CHEMICAL.block(), basin.getBlockPos(), Direction.EAST);
        gas.setChemicalInTank(0, new ChemicalStack(MekanismChemicals.HYDROGEN, 1000));
        var bridgeCapability = GasFluidHandler.capability(helper.getLevel(), basin.getBlockPos(), Direction.EAST);
        IFluidHandler bridge = bridgeCapability;
        helper.assertTrue(bridgeCapability == GasFluidHandler.capability(helper.getLevel(), basin.getBlockPos(), Direction.EAST),
            "Repeated endpoint lookup must reuse its capability");
        helper.assertTrue(GasFluidHandler.capability(helper.getLevel(), basin.getBlockPos(), Direction.WEST) != null,
            "The opposite side must expose an independent valid cached endpoint");
        FluidStack hydrogen = GasFluid.encode(new ChemicalStack(MekanismChemicals.HYDROGEN, 250));
        FluidStack oxygen = GasFluid.encode(new ChemicalStack(MekanismChemicals.OXYGEN, 250));
        helper.assertTrue(!FluidStack.isSameFluidSameComponents(hydrogen, oxygen), "Gas identities must remain distinct");
        ItemStack gasContainer = new ItemStack(MekanismBlocks.BASIC_CHEMICAL_TANK.asItem());
        IChemicalHandler container = gasContainer.getCapability(Capabilities.CHEMICAL.item());
        helper.assertTrue(container.insertChemical(new ChemicalStack(MekanismChemicals.HYDROGEN, 250), Action.EXECUTE).isEmpty(), "Fill gas filter container");
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
        pipe.manageSource(helper.getLevel(), basin);
        IFluidHandler outlet = pipe.provideHandler().getCapability();
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
        helper.assertTrue(bridge.getTanks() == 0 && bridge.drain(hydrogen, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Bridge must invalidate with its endpoint");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inputOnlyGasRecipeConsumesGasWithoutOutput(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative, BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:basin")).defaultBlockState());
        BasinGasHandler handler = new BasinGasHandler((BasinBlockEntity) helper.getBlockEntity(relative));
        ChemicalStack spentWaste = new ChemicalStack(MekanismChemicals.SPENT_NUCLEAR_WASTE, 100);
        handler.setChemicalInTank(1, spentWaste);

        helper.assertTrue(handler.processRecipeGas(spentWaste.getChemical(), 100, null, 0, true),
            "Input-only simulation must accept sufficient spent nuclear waste");
        helper.assertTrue(handler.processRecipeGas(spentWaste.getChemical(), 101, null, 0, true) == false,
            "Input-only recipe must reject a shortage");
        helper.assertTrue(handler.processRecipeGas(MekanismChemicals.NUCLEAR_WASTE.get(), 100, null, 0, true) == false,
            "Input-only recipe must reject the wrong gas");
        helper.assertTrue(handler.processRecipeGas(spentWaste.getChemical(), 100, null, 0, false),
            "Input-only execution must accept the exact input");
        helper.assertTrue(handler.getChemicalInTank(1).isEmpty(), "Input-only execution must consume all required gas");
        helper.assertTrue(handler.getChemicalInTank(0).isEmpty(), "Input-only execution must not occupy the output buffer");
        helper.succeed();
    }
}
