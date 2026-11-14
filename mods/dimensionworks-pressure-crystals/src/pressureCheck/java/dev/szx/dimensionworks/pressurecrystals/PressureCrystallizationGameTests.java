package dev.szx.dimensionworks.pressurecrystals;

import com.negodya1.vintageimprovements.content.kinetics.vacuum_chamber.VacuumChamberBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.szx.dimensionworks.pressurecrystals.mixin.BasinTankAccessor;
import dev.szx.dimensionworks.pressurecrystals.mixin.VacuumChamberModeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(DimensionWorksPressureCrystals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PressureCrystallizationGameTests {
    private static final BlockPos BASIN_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void idleBasinProducesUselessCrystal(GameTestHelper helper) {
        checkProduction(helper, null, false, 0, "iron", false,
            DimensionWorksPressureCrystals.USELESS_CRYSTAL.get());
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void stoppedCompressorProducesUselessCrystal(GameTestHelper helper) {
        checkProduction(helper, 0F, true, 0, "iron", false,
            DimensionWorksPressureCrystals.USELESS_CRYSTAL.get());
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void vacuumModeProducesUselessCrystal(GameTestHelper helper) {
        checkProduction(helper, 4096F, false, 0, "iron", false,
            DimensionWorksPressureCrystals.USELESS_CRYSTAL.get());
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void lowPressureUsesIdleTickAndRetainsMetal(GameTestHelper helper) {
        checkProduction(helper, 512F, true, 0, "iron", false,
            DimensionWorksPressureCrystals.LOW_PRESSURE_CRYSTAL.get());
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void mediumPressureUsesSecondInputTank(GameTestHelper helper) {
        checkProduction(helper, 2048F, true, 1, "azure_neodymium", false,
            DimensionWorksPressureCrystals.MEDIUM_PRESSURE_CRYSTAL.get());
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void highPressureProducesNetheriteTrait(GameTestHelper helper) {
        checkProduction(helper, 4096F, true, 0, "netherite", false,
            DimensionWorksPressureCrystals.HIGH_PRESSURE_CRYSTAL.get());
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void blockedOutputPreservesCrystallizedFluid(GameTestHelper helper) {
        checkProduction(helper, 512F, true, 0, "iron", true,
            DimensionWorksPressureCrystals.LOW_PRESSURE_CRYSTAL.get());
    }

    @GameTest(template = "pressure_grid", timeoutTicks = 280)
    public static void allTwentyOneMetalsProduceAllFourPressureTiers(GameTestHelper helper) {
        var metals = moltenMetals();
        helper.assertTrue(metals.size() == 21, "Expected every one of the 21 molten metals");
        java.util.List<BasinBlockEntity> basins = new java.util.ArrayList<>();
        Float[] rpms = {null, 512F, 2048F, 4096F};
        Item[] outputs = {DimensionWorksPressureCrystals.USELESS_CRYSTAL.get(),
            DimensionWorksPressureCrystals.LOW_PRESSURE_CRYSTAL.get(),
            DimensionWorksPressureCrystals.MEDIUM_PRESSURE_CRYSTAL.get(),
            DimensionWorksPressureCrystals.HIGH_PRESSURE_CRYSTAL.get()};
        for (int tier = 0; tier < rpms.length; tier++) {
            for (int i = 0; i < metals.size(); i++) {
                BasinBlockEntity basin = createBasin(helper, matrixPos(i, tier), rpms[tier], true);
                fill(helper, basin, crystal(metals.get(i), 1000));
                basins.add(basin);
            }
        }
        helper.runAfterDelay(205, () -> {
            for (int tier = 0; tier < rpms.length; tier++) {
                for (int i = 0; i < metals.size(); i++) {
                    var basin = basins.get(tier * metals.size() + i);
                    assertResult(helper, basin, outputs[tier], metals.get(i), 400);
                    if (rpms[tier] != null) {
                        var compressor = (VacuumChamberBlockEntity) helper.getLevel().getBlockEntity(basin.getBlockPos().above(2));
                        helper.assertTrue(Math.abs(compressor.getSpeed()) == rpms[tier],
                            "Real source must drive compressor at " + rpms[tier] + ", got " + compressor.getSpeed());
                    }
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = "pressure_grid", timeoutTicks = 720)
    public static void allTwentyOneOpenBasinsCrystallizeMoltenMetal(GameTestHelper helper) {
        var metals = moltenMetals();
        java.util.List<BasinBlockEntity> basins = new java.util.ArrayList<>();
        for (int i = 0; i < metals.size(); i++) {
            var basin = createBasin(helper, matrixPos(i, 0), null, false);
            blockOutput(basin);
            fill(helper, basin, molten(metals.get(i), 1000));
            basins.add(basin);
        }
        helper.runAfterDelay(590, () -> {
            for (int i = 0; i < metals.size(); i++)
                helper.assertTrue(inputTank(basins.get(i), 0).getFluid().isFluidStackIdentical(molten(metals.get(i), 1000)),
                    "Open basin must wait 600 ticks: " + metals.get(i));
        });
        helper.runAfterDelay(650, () -> {
            for (int i = 0; i < metals.size(); i++)
                helper.assertTrue(inputTank(basins.get(i), 0).getFluid().isFluidStackIdentical(crystal(metals.get(i), 1000)),
                    "An open basin must crystallize all its molten " + metals.get(i));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void moltenBasinCompletesRealHighPressureCycle(GameTestHelper helper) {
        var basin = createBasin(helper, BASIN_POS, 4096F, true);
        fill(helper, basin, molten("netherite", 1000));
        helper.runAfterDelay(590, () -> helper.assertTrue(basin.getOutputInventory().isEmpty(),
            "Molten metal cannot precipitate before crystallizing"));
        helper.runAfterDelay(850, () -> {
            assertResult(helper, basin, DimensionWorksPressureCrystals.HIGH_PRESSURE_CRYSTAL.get(), "netherite", 400);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 1100)
    public static void closedLidRestartsBasinExposure(GameTestHelper helper) {
        var basin = createBasin(helper, BASIN_POS, null, false);
        var lid = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("createdieselgenerators", "basin_lid"));
        helper.setBlock(BASIN_POS.above(), lid.defaultBlockState().setValue(BlockStateProperties.OPEN, false));
        blockOutput(basin);
        fill(helper, basin, molten("iron", 1000));
        helper.runAfterDelay(400, () -> {
            helper.assertTrue(inputTank(basin, 0).getFluid().isFluidStackIdentical(molten("iron", 1000)),
                "A closed actual basin lid must prevent crystallization");
            helper.setBlock(BASIN_POS.above(), lid.defaultBlockState().setValue(BlockStateProperties.OPEN, true));
        });
        helper.runAfterDelay(950, () -> helper.assertTrue(inputTank(basin, 0).getFluid().isFluidStackIdentical(molten("iron", 1000)),
            "Opening the lid starts a fresh 600-tick exposure"));
        helper.runAfterDelay(1050, () -> {
            helper.assertTrue(inputTank(basin, 0).getFluid().isFluidStackIdentical(crystal("iron", 1000)),
                "An open actual basin lid must allow crystallization");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 1250)
    public static void toppingUpAndChangingMetalRestartBothInputTimers(GameTestHelper helper) {
        var basin = createBasin(helper, BASIN_POS, null, false);
        blockOutput(basin);
        fill(helper, basin, molten("iron", 400));
        fill(helper, basin, molten("copper", 1000));
        helper.runAfterDelay(500, () -> {
            fill(helper, basin, molten("iron", 600));
            IFluidHandler handler = basin.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new);
            helper.assertTrue(handler.drain(molten("copper", 1000), IFluidHandler.FluidAction.EXECUTE).getAmount() == 1000,
                "Drain the second input through the actual basin fluid capability");
            fill(helper, basin, molten("gold", 1000));
        });
        helper.runAfterDelay(1050, () -> {
            helper.assertTrue(inputTank(basin, 0).getFluid().isFluidStackIdentical(molten("iron", 1000)), "Topping up restarts exposure");
            helper.assertTrue(inputTank(basin, 1).getFluid().isFluidStackIdentical(molten("gold", 1000)), "Changing metal restarts exposure");
        });
        helper.runAfterDelay(1170, () -> {
            helper.assertTrue(inputTank(basin, 0).getFluid().isFluidStackIdentical(crystal("iron", 1000)), "First input must crystallize");
            helper.assertTrue(inputTank(basin, 1).getFluid().isFluidStackIdentical(crystal("gold", 1000)), "Second input must crystallize");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 720)
    public static void currentBasinSaveLoadPreservesExposure(GameTestHelper helper) {
        var basin = createBasin(helper, BASIN_POS, null, false);
        blockOutput(basin);
        fill(helper, basin, molten("iron", 1000));
        BasinBlockEntity[] current = {basin};
        helper.runAfterDelay(500, () -> {
            var saved = current[0].saveWithFullMetadata();
            helper.setBlock(BASIN_POS, Blocks.AIR);
            current[0] = createBasin(helper, BASIN_POS, null, false);
            current[0].load(saved);
        });
        helper.runAfterDelay(650, () -> {
            helper.assertTrue(inputTank(current[0], 0).getFluid().isFluidStackIdentical(crystal("iron", 1000)),
                "Current basin NBT must retain continuous exposure across save/load");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 280)
    public static void lessThanSixHundredMbDoesNotProduceOrConsume(GameTestHelper helper) {
        var basin = createBasin(helper, BASIN_POS, null, false);
        fill(helper, basin, crystal("iron", 599));
        helper.runAfterDelay(205, () -> {
            helper.assertTrue(inputTank(basin, 0).getFluid().isFluidStackIdentical(crystal("iron", 599)),
                "599 mB must be preserved");
            helper.assertTrue(basin.getOutputInventory().isEmpty(), "Insufficient fluid must not produce crystals");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 280)
    public static void exactlySixHundredFlowingMbProducesThreeWithoutRemainder(GameTestHelper helper) {
        var basin = createBasin(helper, BASIN_POS, 512F, true);
        var source = (FlowingFluid) crystal("iron", 600).getFluid();
        fill(helper, basin, new FluidStack(source.getFlowing(), 600));
        helper.runAfterDelay(205, () -> {
            assertResult(helper, basin, DimensionWorksPressureCrystals.LOW_PRESSURE_CRYSTAL.get(), "iron", 0);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 480)
    public static void clearingBlockedOutputProcessesExactlyOnce(GameTestHelper helper) {
        var basin = createBasin(helper, BASIN_POS, null, false);
        blockOutput(basin);
        fill(helper, basin, crystal("iron", 1000));
        helper.runAfterDelay(205, () -> {
            helper.assertTrue(inputTank(basin, 0).getFluid().isFluidStackIdentical(crystal("iron", 1000)),
                "Blocked output must not consume fluid");
            for (int slot = 0; slot < basin.getOutputInventory().getSlots(); slot++)
                basin.getOutputInventory().setStackInSlot(slot, ItemStack.EMPTY);
        });
        helper.runAfterDelay(410, () -> {
            assertResult(helper, basin, DimensionWorksPressureCrystals.USELESS_CRYSTAL.get(), "iron", 400);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 280)
    public static void blockedFirstMetalDoesNotPreventSecondMetalFromSettling(GameTestHelper helper) {
        var basin = createBasin(helper, BASIN_POS, 512F, true);
        blockOutput(basin);
        ItemStack copperCrystals = new ItemStack(DimensionWorksPressureCrystals.LOW_PRESSURE_CRYSTAL.get(), 61);
        copperCrystals.getOrCreateTagElement("DimensionWorks").putString("Metal", "copper");
        basin.getOutputInventory().setStackInSlot(0, copperCrystals);
        fill(helper, basin, crystal("iron", 1000));
        fill(helper, basin, crystal("copper", 1000));
        helper.runAfterDelay(205, () -> {
            helper.assertTrue(inputTank(basin, 0).getFluid().isFluidStackIdentical(crystal("iron", 1000)),
                "The blocked first metal must remain untouched");
            helper.assertTrue(inputTank(basin, 1).getFluid().isFluidStackIdentical(molten("copper", 400)),
                "The second metal must settle when its own output fits");
            helper.assertTrue(basin.getOutputInventory().getStackInSlot(0).getCount() == 64,
                "Exactly three copper crystals must join the matching output stack");
            helper.succeed();
        });
    }

    private static java.util.List<String> moltenMetals() {
        return ForgeRegistries.FLUIDS.getKeys().stream()
            .filter(id -> id.getNamespace().equals("dimensionworks") && id.getPath().startsWith("molten_"))
            .filter(id -> ForgeRegistries.FLUIDS.getValue(id) instanceof FlowingFluid fluid && fluid.getSource() == fluid)
            .map(id -> id.getPath().substring(7)).sorted().toList();
    }
    private static BlockPos matrixPos(int metal, int tier) {
        return new BlockPos(1 + metal % 7 * 4, 1, 1 + metal / 7 * 4 + tier * 12);
    }
    private static FluidStack molten(String metal, int amount) {
        return new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("dimensionworks", "molten_" + metal)), amount);
    }
    private static FluidStack crystal(String metal, int amount) {
        return new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("dimensionworks_cave_factory", "crystallized_molten_" + metal)), amount);
    }
    private static void fill(GameTestHelper helper, BasinBlockEntity basin, FluidStack fluid) {
        IFluidHandler handler = basin.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new);
        helper.assertTrue(handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE) == fluid.getAmount(),
            "Actual basin capability must accept " + fluid);
    }
    private static void blockOutput(BasinBlockEntity basin) {
        for (int slot = 0; slot < basin.getOutputInventory().getSlots(); slot++)
            basin.getOutputInventory().setStackInSlot(slot, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
    }
    private static void assertResult(GameTestHelper helper, BasinBlockEntity basin, Item expected, String metal, int remainder) {
        ItemStack result = basin.getOutputInventory().getStackInSlot(0);
        helper.assertTrue(result.is(expected) && result.getCount() == 3, "Expected three " + expected + " from " + metal + ", got " + result);
        if (expected == DimensionWorksPressureCrystals.USELESS_CRYSTAL.get())
            helper.assertTrue(!result.hasTag(), "No-pressure crystals must not acquire a metal trait");
        else helper.assertTrue(result.hasTag() && metal.equals(result.getTag().getCompound("DimensionWorks").getString("Metal")),
            "Pressure output must retain metal " + metal);
        FluidStack actual = inputTank(basin, 0).getFluid();
        helper.assertTrue(remainder == 0 ? actual.isEmpty() : actual.isFluidStackIdentical(molten(metal, remainder)),
            "Consume exactly 600 mB and restore the ordinary molten remainder: " + actual);
    }

    private static void checkProduction(GameTestHelper helper, Float rpm, boolean pressurizing,
                                        int inputSlot, String metal, boolean blocked, Item expected) {
        // Clear the initial contents-change tick away from the global settlement tick.
        // This makes the old TAIL-only injection fail deterministically on the idle return.
        long setupDelay = (helper.getLevel().getGameTime() + 2) % 200L == 0 ? 3 : 2;
        helper.runAfterDelay(setupDelay, () -> {
            BasinBlockEntity basin = createBasin(helper, BASIN_POS, rpm, pressurizing);
            SmartFluidTank tank = inputTank(basin, inputSlot);
            FluidStack original = new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(
                "dimensionworks_cave_factory", "crystallized_molten_" + metal)), 1000);
            tank.setFluid(original);
            if (blocked) {
                for (int slot = 0; slot < basin.getOutputInventory().getSlots(); slot++)
                    basin.getOutputInventory().setStackInSlot(slot, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
            }
            basin.tick();
            helper.runAfterDelay(205, () -> {
                if (blocked) {
                    helper.assertTrue(tank.getFluid().isFluidStackIdentical(original),
                        "Full output must preserve the input fluid");
                } else {
                    ItemStack result = basin.getOutputInventory().getStackInSlot(0);
                    helper.assertTrue(result.is(expected) && result.getCount() == 3,
                        "An idle basin must produce three crystals of the expected pressure tier; got " + result);
                    if (expected == DimensionWorksPressureCrystals.USELESS_CRYSTAL.get()) {
                        helper.assertTrue(!result.hasTag(), "Useless crystals must not carry a metal trait");
                    } else {
                        helper.assertTrue(result.hasTag()
                            && metal.equals(result.getTag().getCompound("DimensionWorks").getString("Metal")),
                            "Pressure crystals must retain their source metal");
                    }
                    FluidStack remaining = tank.getFluid();
                    helper.assertTrue(remaining.getAmount() == 400 && remaining.getFluid() == ForgeRegistries.FLUIDS.getValue(
                        new ResourceLocation("dimensionworks", "molten_" + metal)),
                        "Consume 600 mB and turn the remaining 400 mB into ordinary molten metal");
                }
                helper.succeed();
            });
        });
    }

    private static BasinBlockEntity createBasin(GameTestHelper helper, BlockPos pos, Float rpm, boolean pressurizing) {
        helper.setBlock(pos, AllBlocks.BASIN.getDefaultState());
        helper.setBlock(pos.above(), Blocks.AIR);
        if (rpm != null) {
            BlockPos compressorPos = pos.above(2);
            var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("vintageimprovements", "vacuum_chamber"));
            helper.setBlock(compressorPos, block.defaultBlockState());
            var compressor = (VacuumChamberBlockEntity) helper.getBlockEntity(compressorPos);
            BlockPos gearPos = compressorPos.east().south();
            helper.setBlock(gearPos, AllBlocks.LARGE_COGWHEEL.getDefaultState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
            var gear = (SimpleKineticBlockEntity) helper.getBlockEntity(gearPos);
            BlockPos motorPos = gearPos.above();
            helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
            var motor = (CreativeMotorBlockEntity) helper.getBlockEntity(motorPos);
            java.util.UUID owner = new java.util.UUID(105, helper.absolutePos(motorPos).asLong());
            for (var member : java.util.List.of(motor, gear, compressor)) {
                member.getPersistentData().putUUID("dimensionworks_owner", owner);
                member.getPersistentData().putInt("dimensionworks_machine_rpm_limit", 8192);
            }
            motor.tick();
            gear.tick();
            compressor.tick();
            if (((VacuumChamberModeAccessor) compressor).dimensionworks$isPressurizing() != pressurizing)
                compressor.changeMode();
            // The compressor meshes as a small cog; a diagonal large cog doubles source RPM.
            motor.generatedSpeed.between(-8192, 8192).setValue(Math.round(rpm / 2));
            motor.updateGeneratedRotation();
        }
        return (BasinBlockEntity) helper.getBlockEntity(pos);
    }

    private static SmartFluidTank inputTank(BasinBlockEntity basin, int slot) {
        return ((BasinTankAccessor) (Object) basin.inputTank.getTanks()[slot]).dimensionworks$getTank();
    }
}
