package dev.szx.dimensionworks.cavefactory.gametest;

import dev.szx.dimensionworks.cavefactory.block.FactoryControllerBlock;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryControllerBlockEntity;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryPortBlockEntity;
import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.logic.PortMode;
import dev.szx.dimensionworks.cavefactory.logic.StructureLayout;
import dev.szx.dimensionworks.cavefactory.registry.CFBlocks;
import dev.szx.dimensionworks.cavefactory.registry.CFFluids;
import dev.szx.dimensionworks.cavefactory.registry.CFItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dimensionworks_cave_factory")
@PrefixGameTestTemplate(false)
public final class CaveFactoryGameTests {
    private static final BlockPos CONTROLLER_POS = new BlockPos(4, 4, 4);

    private CaveFactoryGameTests() {}

    @GameTest(template = "empty_9x9x9", timeoutTicks = 60)
    public static void assemblesCompleteCasing(GameTestHelper helper) {
        FactoryControllerBlockEntity controller = buildFactory(helper);
        helper.assertTrue(controller != null, "Controller block entity was not created");
        helper.assertTrue(controller.validateStructure(), "3x3x3 casing and port layout did not validate");
        helper.assertTrue(controller.structureValid(), "Validated structure state was not retained");
        helper.succeed();
    }

    @GameTest(template = "empty_9x9x9", timeoutTicks = 60)
    public static void exposesConfiguredPortCapabilities(GameTestHelper helper) {
        FactoryControllerBlockEntity controller = buildFactory(helper);
        Player owner = helper.makeMockSurvivalPlayer();
        controller.setOwner(owner);

        helper.assertTrue(controller.cyclePort(Direction.EAST, owner), "Owner could not configure kinetic port");
        helper.assertTrue(controller.cyclePort(Direction.WEST, owner), "Owner could not configure item port");

        helper.assertTrue(controller.portMode(Direction.EAST) == PortMode.KINETIC_INPUT,
            "East port did not become kinetic input");
        helper.assertTrue(controller.portMode(Direction.WEST) == PortMode.ITEM_INPUT,
            "West port did not become item input");
        helper.assertTrue(controller.kineticPort() == Direction.EAST, "More than one kinetic port was configured");

        FactoryPortBlockEntity westPort = (FactoryPortBlockEntity) helper.getBlockEntity(
            CONTROLLER_POS.offset(-1, 0, 0)
        );
        helper.assertTrue(westPort != null, "West port block entity is missing");
        helper.assertTrue(
            westPort.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST).isPresent(),
            "Configured west port did not expose its item capability"
        );
        helper.succeed();
    }

    @GameTest(template = "empty_9x9x9", timeoutTicks = 60)
    public static void enforcesOwnershipForConfiguration(GameTestHelper helper) {
        FactoryControllerBlockEntity controller = buildFactory(helper);
        Player owner = helper.makeMockSurvivalPlayer();
        Player stranger = helper.makeMockSurvivalPlayer();
        controller.setOwner(owner);

        helper.assertFalse(controller.cyclePort(Direction.EAST, stranger), "Stranger changed port configuration");
        helper.assertTrue(controller.portMode(Direction.EAST) == PortMode.DISABLED,
            "Unauthorized port change modified the controller");
        helper.assertTrue(controller.cyclePort(Direction.EAST, owner), "Owner could not change port configuration");
        helper.succeed();
    }

    @GameTest(template = "empty_9x9x9", timeoutTicks = 60)
    public static void installsCompatibleModulesAndRejectsMissingMapping(GameTestHelper helper) {
        FactoryControllerBlockEntity controller = buildFactory(helper);
        ItemStack buffer = new ItemStack(CFItems.module(FactoryModule.PRESSURE_BUFFER).get());
        ItemStack converter = new ItemStack(CFItems.module(FactoryModule.PHASE_CONVERTER).get());

        ItemStack rejected = controller.moduleInventory().insertItem(1, converter, false);
        helper.assertTrue(rejected.getCount() == 1, "Mechanism module without a recipe mapping occupied the slot");
        helper.assertTrue(controller.moduleInventory().getStackInSlot(1).isEmpty(),
            "Mechanism module remained installed after rejection");

        ItemStack accepted = controller.moduleInventory().insertItem(0, buffer, false);
        helper.assertTrue(accepted.isEmpty(), "Pressure buffer was rejected by a fluid-capable controller");
        helper.assertTrue(controller.fluidInputCapacity() == 5_000, "Pressure buffer did not raise tank capacity");

        helper.assertTrue(controller.ejectModules(), "Modules were not ejected");
        helper.assertItemEntityPresent(CFItems.module(FactoryModule.PRESSURE_BUFFER).get(), CONTROLLER_POS, 1.0D);
        helper.succeed();
    }

    @GameTest(template = "empty_9x9x9", timeoutTicks = 60)
    public static void refusesWrenchMoveWhileFluidIsStored(GameTestHelper helper) {
        FactoryControllerBlockEntity controller = buildFactory(helper);
        Player owner = helper.makeMockSurvivalPlayer();
        controller.setOwner(owner);
        controller.inputTank().setFluid(new FluidStack(CFFluids.get(MachineType.ABYSSAL).still().get(), 250));

        BlockPos absolute = helper.absolutePos(CONTROLLER_POS);
        BlockState state = helper.getBlockState(CONTROLLER_POS);
        UseOnContext context = new UseOnContext(
            helper.getLevel(),
            owner,
            InteractionHand.MAIN_HAND,
            ItemStack.EMPTY,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );
        InteractionResult result = ((FactoryControllerBlock) state.getBlock()).onSneakWrenched(state, context);

        helper.assertTrue(result == InteractionResult.FAIL, "Wrench move was not denied for a fluid-filled controller");
        helper.assertBlockPresent(CFBlocks.controller(MachineType.ABYSSAL).get(), CONTROLLER_POS);
        helper.succeed();
    }

    private static FactoryControllerBlockEntity buildFactory(GameTestHelper helper) {
        helper.setBlock(CONTROLLER_POS, CFBlocks.controller(MachineType.ABYSSAL).get());
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (StructureLayout.isCenter(x, y, z)) {
                        continue;
                    }
                    BlockPos part = CONTROLLER_POS.offset(x, y, z);
                    if (StructureLayout.isFaceCenter(x, y, z)) {
                        helper.setBlock(part, CFBlocks.FACTORY_PORT.get());
                    } else {
                        helper.setBlock(part, CFBlocks.FACTORY_CASING.get());
                    }
                }
            }
        }
        FactoryControllerBlockEntity controller =
            (FactoryControllerBlockEntity) helper.getBlockEntity(CONTROLLER_POS);
        if (controller != null) {
            controller.markStructureDirty();
            controller.lazyTick();
        }
        return controller;
    }
}
