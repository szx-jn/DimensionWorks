package dev.szx.dimensionworks.pressurecrystals;

import com.negodya1.vintageimprovements.content.kinetics.vacuum_chamber.VacuumChamberBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import dev.szx.dimensionworks.pressurecrystals.mixin.BasinTankAccessor;
import net.minecraft.core.BlockPos;
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

    private static void checkProduction(GameTestHelper helper, Float rpm, boolean pressurizing,
                                        int inputSlot, String metal, boolean blocked, Item expected) {
        // Clear the initial contents-change tick away from the global settlement tick.
        // This makes the old TAIL-only injection fail deterministically on the idle return.
        long setupDelay = (helper.getLevel().getGameTime() + 2) % 200L == 0 ? 3 : 2;
        helper.runAfterDelay(setupDelay, () -> {
            BasinBlockEntity basin = createBasin(helper, rpm, pressurizing);
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

    private static BasinBlockEntity createBasin(GameTestHelper helper, Float rpm, boolean pressurizing) {
        helper.setBlock(BASIN_POS, AllBlocks.BASIN.getDefaultState());
        if (rpm != null) {
            BlockPos compressorPos = BASIN_POS.above(2);
            var compressorBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("vintageimprovements", "vacuum_chamber"));
            helper.setBlock(compressorPos, compressorBlock.defaultBlockState());
            var compressor = (VacuumChamberBlockEntity) helper.getBlockEntity(compressorPos);
            // Create clears rotation without a live source during kinetic validation.
            // Initialize a real source and supply the fixture RPM through its network;
            // transmission geometry is outside this basin-settlement regression.
            BlockPos motorPos = new BlockPos(4, 1, 1);
            helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState());
            var motor = (CreativeMotorBlockEntity) helper.getBlockEntity(motorPos);
            for (var member : java.util.List.of(motor, compressor)) {
                member.getPersistentData().putUUID("dimensionworks_owner", new java.util.UUID(105, member.getBlockPos().asLong()));
                member.getPersistentData().putInt("dimensionworks_machine_rpm_limit", 4096);
            }
            motor.tick();
            compressor.tick();
            if (pressurizing) compressor.changeMode();
            compressor.setSource(helper.absolutePos(motorPos));
            compressor.setSpeed(rpm);
        }
        return (BasinBlockEntity) helper.getBlockEntity(BASIN_POS);
    }

    private static SmartFluidTank inputTank(BasinBlockEntity basin, int slot) {
        return ((BasinTankAccessor) (Object) basin.inputTank.getTanks()[slot]).dimensionworks$getTank();
    }
}
