package dev.szx.dimensionworks.cavefactory.twilight;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

/** Opt-in: real TF, KubeJS registration/data and Create processing in an ignored disposable world. */
@GameTestHolder("dimensionworks_cave_factory")
@PrefixGameTestTemplate(false)
public final class TwilightIndustryGameTests {
    private static final String TEMPLATE = "empty_9x9x9";
    private static final String BOAR_DATA = "dimensionworks_twilight_work";
    private TwilightIndustryGameTests() {}

    private static ServerLevel forest(GameTestHelper h) {
        ServerLevel level = h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, TwilightIndustry.DIMENSION));
        h.assertTrue(level != null, "Real Twilight Forest dimension missing");
        return level;
    }
    private static BlockPos position(GameTestHelper h) {
        BlockPos absolute = h.absolutePos(new BlockPos(3, 3, 3));
        // Separate the custom-dimensional fixture regions of concurrently running tests.
        return new BlockPos(absolute.getX() * 4, 200, absolute.getZ() * 4);
    }
    private static TwilightWorkstationBlockEntity station(ServerLevel level, BlockPos pos, boolean bio, boolean bed) {
        // Match the industrial capacity assumed at Twilight entry; fresh test profiles default to 32 RPM.
        dev.szx.dimensionworks.rpmlimit.RpmLimitConfig.DEFAULT_RPM.set(128);
        // GameTest's overworld template cleanup does not reset our separate Twilight fixture area.
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        // Test fixtures only. Production code must never create a chunk-loading ticket.
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            BlockPos floor = pos.offset(x, -1, z);
            level.setChunkForced(floor.getX() >> 4, floor.getZ() >> 4, true);
            level.setBlockAndUpdate(floor, bed && Math.abs(x) <= 1 && Math.abs(z) <= 1
                ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());
        }
        level.getEntitiesOfClass(Animal.class, new net.minecraft.world.phys.AABB(pos).inflate(6, 2, 6))
            .forEach(Animal::discard);
        level.setBlockAndUpdate(pos, (bio ? TwilightIndustry.CONDITIONER : TwilightIndustry.LEACHER).get()
            .defaultBlockState().setValue(TwilightWorkstationBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        level.setBlockAndUpdate(pos.east(), AllBlocks.CREATIVE_MOTOR.getDefaultState()
            .setValue(CreativeMotorBlock.FACING, Direction.WEST));
        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) level.getBlockEntity(pos.east());
        motor.generatedSpeed.setValue(128);
        motor.updateGeneratedRotation();
        TwilightWorkstationBlockEntity machine = (TwilightWorkstationBlockEntity) level.getBlockEntity(pos);
        Item leaves = ForgeRegistries.ITEMS.getValue(new ResourceLocation("twilightforest", "twilight_oak_leaves"));
        ((ItemStackHandler) machine.inventory()).setStackInSlot(0, new ItemStack(leaves, 8));
        ((ItemStackHandler) machine.inventory()).setStackInSlot(1,
            new ItemStack(bio ? Items.CARROT : TwilightIndustry.HUMUS.get()));
        machine.waterTank().fill(new FluidStack(Fluids.WATER, bio ? 250 : 1000), IFluidHandler.FluidAction.EXECUTE);
        return machine;
    }
    private static Animal[] boars(ServerLevel level, BlockPos pos) {
        Animal[] animals = new Animal[2];
        for (int i = 0; i < 2; i++) {
            Animal animal = (Animal) ForgeRegistries.ENTITY_TYPES.getValue(TwilightIndustry.BOAR).create(level);
            animal.setPos(pos.getX() + .5 + i, pos.getY(), pos.getZ() + 1.5);
            animal.setNoAi(true); animal.setPersistenceRequired();
            level.addFreshEntity(animal);
            animals[i] = animal;
        }
        return animals;
    }
    private static FluidStack extract(int amount) {
        return new FluidStack(ForgeRegistries.FLUIDS.getValue(TwilightIndustry.EXTRACT), amount);
    }
    private static void untouched(GameTestHelper h, TwilightWorkstationBlockEntity machine, int water) {
        h.assertTrue(machine.inventory().getStackInSlot(0).getCount() == 8, "Leaves consumed while blocked");
        h.assertTrue(machine.inventory().getStackInSlot(1).getCount() == 1, "Auxiliary material consumed while blocked");
        h.assertTrue(machine.waterTank().getFluidAmount() == water, "Water consumed while blocked");
    }

    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 300)
    public static void leachingConsumesOneExactBatch(GameTestHelper h) {
        var machine = station(forest(h), position(h), false, true);
        h.runAfterDelay(260, () -> {
            h.assertTrue(Math.abs(machine.getSpeed()) == 128, "Real Create motor did not power the vat");
            h.assertTrue(machine.outputTank().getFluidAmount() == 1000, "Incorrect extract output");
            h.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(), "Leaf input not consumed");
            h.assertTrue(machine.inventory().getStackInSlot(1).isEmpty(), "Humus input not consumed");
            h.assertTrue(machine.waterTank().isEmpty(), "Water input not consumed");
            h.succeed();
        });
    }
    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 300)
    public static void missingBedDoesNotConsume(GameTestHelper h) {
        var machine = station(forest(h), position(h), false, false);
        h.runAfterDelay(240, () -> { untouched(h, machine, 1000);
            h.assertTrue(machine.status() == TwilightWorkstationBlockEntity.Status.NO_BED, "Missing-bed reason not visible");
            h.succeed(); });
    }
    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 300)
    public static void wrongDimensionDoesNotConsume(GameTestHelper h) {
        var machine = station(h.getLevel(), h.absolutePos(new BlockPos(3, 3, 3)), false, true);
        h.runAfterDelay(240, () -> { untouched(h, machine, 1000);
            h.assertTrue(machine.status() == TwilightWorkstationBlockEntity.Status.WRONG_DIMENSION, "Overworld collection allowed");
            h.succeed(); });
    }
    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 300)
    public static void fullOutputDoesNotConsume(GameTestHelper h) {
        var machine = station(forest(h), position(h), false, true);
        machine.outputTank().setFluid(extract(10000));
        var outlet = machine.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
        h.assertTrue(outlet.fill(extract(1000), IFluidHandler.FluidAction.EXECUTE) == 0, "Outlet accepted external input");
        h.runAfterDelay(240, () -> { untouched(h, machine, 1000);
            h.assertTrue(machine.outputTank().getFluidAmount() == 10000, "Full output changed"); h.succeed(); });
    }
    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 300)
    public static void missingWaterDoesNotConsume(GameTestHelper h) {
        var machine = station(forest(h), position(h), false, true);
        machine.waterTank().setFluid(FluidStack.EMPTY);
        h.runAfterDelay(240, () -> { untouched(h, machine, 0); h.succeed(); });
    }
    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 400)
    public static void boarsWorkAndKeepCooldownInCapturedNbt(GameTestHelper h) {
        ServerLevel level = forest(h);
        var machine = station(level, position(h), true, false);
        Animal[] animals = boars(level, position(h));
        h.runAfterDelay(240, () -> {
            h.assertTrue(machine.inventory().getStackInSlot(2).getCount() == 4, "Boars did not produce 4 humus");
            h.assertTrue(machine.waterTank().isEmpty(), "Incorrect biological water consumption");
            h.assertTrue(animals[0].isAlive() && animals[1].isAlive(), "Work consumed a boar");
            CompoundTag captured = new CompoundTag();
            animals[0].saveWithoutId(captured);
            int rest = captured.getCompound("ForgeData").getCompound(BOAR_DATA).getInt("Cooldown");
            h.assertTrue(rest > 0, "CMP-compatible ForgeData did not preserve cooldown");
            animals[0].discard();
            h.runAfterDelay(60, () -> {
                Animal restored = (Animal) ForgeRegistries.ENTITY_TYPES.getValue(TwilightIndustry.BOAR).create(level);
                restored.load(captured);
                h.assertTrue(restored.getPersistentData().getCompound(BOAR_DATA).getInt("Cooldown") == rest,
                    "Packaged time reduced animal cooldown");
                level.addFreshEntity(restored);
                h.assertTrue(!TwilightBoarWork.available(restored), "Unpacking refreshed animal availability");
                h.succeed();
            });
        });
    }
    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 300)
    public static void twoVatsCannotShareWorkingBoars(GameTestHelper h) {
        ServerLevel level = forest(h);
        BlockPos pos = position(h);
        var first = station(level, pos, true, false);
        var second = station(level, pos.south(2), true, false);
        boars(level, pos);
        h.runAfterDelay(260, () -> {
            int total = first.inventory().getStackInSlot(2).getCount() + second.inventory().getStackInSlot(2).getCount();
            h.assertTrue(total == 4, "Two stations used the same boars concurrently"); h.succeed();
        });
    }
    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 200)
    public static void removingBoarStopsPendingBatch(GameTestHelper h) {
        ServerLevel level = forest(h);
        var machine = station(level, position(h), true, false);
        Animal[] animals = boars(level, position(h));
        h.runAfterDelay(80, () -> animals[0].discard());
        h.runAfterDelay(150, () -> {
            untouched(h, machine, 250);
            h.assertTrue(machine.progress() == 0 && machine.inventory().getStackInSlot(2).isEmpty(),
                "Missing boar retained a productive pending batch: progress=" + machine.progress()
                    + ", status=" + machine.status()); h.succeed();
        });
    }
    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 60)
    public static void storageSurvivesBlockItemAndCapabilities(GameTestHelper h) {
        var machine = station(forest(h), position(h), false, true);
        machine.outputTank().setFluid(extract(3000));
        var copy = new TwilightWorkstationBlockEntity(position(h).north(5), machine.getBlockState());
        copy.load(machine.portableStorage());
        untouched(h, copy, 1000);
        h.assertTrue(copy.outputTank().getFluidAmount() == 3000, "Portable block lost fluid");
        var out = copy.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new);
        h.assertTrue(out.insertItem(0, new ItemStack(Items.CARROT), false).getCount() == 1, "Output accepted items");
        copy.invalidateCaps(); copy.reviveCaps();
        h.assertTrue(copy.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).isPresent(), "Revived fluid capability missing");
        h.succeed();
    }

    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 60)
    public static void createSeparationChecksHeatAndProducesBothOutputs(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 3, 4);
        h.setBlock(pos, AllBlocks.BASIN.get());
        h.runAfterDelay(3, () -> {
            BasinBlockEntity basin = (BasinBlockEntity) h.getBlockEntity(pos);
            var fluid = basin.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new);
            fluid.fill(extract(1000), IFluidHandler.FluidAction.EXECUTE);
            var basic = h.getLevel().getRecipeManager().byKey(new ResourceLocation("dimensionworks_cave_factory", "twilight/basic_separation")).orElseThrow();
            h.assertFalse(BasinRecipe.match(basin, basic), "Unheated separation incorrectly matched");
            h.setBlock(pos.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
            basin.tick(); // Heat is cached for one native basin tick.
            h.assertTrue(BasinRecipe.match(basin, basic) && BasinRecipe.apply(basin, basic), "Native Create basic processing failed");
            h.assertTrue(count(basin, TwilightIndustry.RESIN.get()) == 2 && count(basin, TwilightIndustry.FIBER.get()) == 2,
                "Basic separation outputs differ");
            clear(basin);
            fluid.fill(extract(1000), IFluidHandler.FluidAction.EXECUTE);
            basin.getInputInventory().setStackInSlot(0, new ItemStack(TwilightIndustry.FILTER.get()));
            var filtered = h.getLevel().getRecipeManager().byKey(new ResourceLocation("dimensionworks_cave_factory", "twilight/filtered_separation")).orElseThrow();
            h.assertTrue(BasinRecipe.match(basin, filtered) && BasinRecipe.apply(basin, filtered), "Native Create filtered processing failed");
            h.assertTrue(count(basin, TwilightIndustry.RESIN.get()) == 4 && count(basin, TwilightIndustry.FIBER.get()) == 2,
                "Filtered route has no expected yield advantage");
            h.assertTrue(basin.getInputInventory().isEmpty(), "Filter was not consumed"); h.succeed();
        });
    }
    @GameTest(templateNamespace = "dimensionworks_cave_factory", template = TEMPLATE, timeoutTicks = 60)
    public static void createCompactsFramesAndFilters(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 3, 4);
        h.setBlock(pos, AllBlocks.BASIN.get());
        h.runAfterDelay(3, () -> {
            BasinBlockEntity basin = (BasinBlockEntity) h.getBlockEntity(pos);
            Item ironwood = ForgeRegistries.ITEMS.getValue(new ResourceLocation("twilightforest", "ironwood_ingot"));
            basin.getInputInventory().setStackInSlot(0, new ItemStack(ironwood, 4));
            basin.getInputInventory().setStackInSlot(1, new ItemStack(TwilightIndustry.RESIN.get(), 2));
            var frame = h.getLevel().getRecipeManager().byKey(new ResourceLocation("dimensionworks_cave_factory", "twilight/ironwood_frame")).orElseThrow();
            h.assertTrue(BasinRecipe.match(basin, frame) && BasinRecipe.apply(basin, frame), "Frame compacting failed");
            h.assertTrue(count(basin, TwilightIndustry.FRAME.get()) == 1, "Incorrect frame count");
            clear(basin);
            basin.getInputInventory().setStackInSlot(0, new ItemStack(TwilightIndustry.FIBER.get(), 2));
            basin.getInputInventory().setStackInSlot(1, new ItemStack(Items.WHITE_WOOL));
            var filter = h.getLevel().getRecipeManager().byKey(new ResourceLocation("dimensionworks_cave_factory", "twilight/forest_filter")).orElseThrow();
            h.assertTrue(BasinRecipe.match(basin, filter) && BasinRecipe.apply(basin, filter), "Filter compacting failed");
            h.assertTrue(count(basin, TwilightIndustry.FILTER.get()) == 2, "Incorrect filter count"); h.succeed();
        });
    }
    private static int count(BasinBlockEntity basin, Item item) {
        int count = 0;
        for (int i = 0; i < basin.getOutputInventory().getSlots(); i++)
            if (basin.getOutputInventory().getStackInSlot(i).is(item)) count += basin.getOutputInventory().getStackInSlot(i).getCount();
        return count;
    }
    private static void clear(BasinBlockEntity basin) {
        for (int i = 0; i < basin.getOutputInventory().getSlots(); i++) basin.getOutputInventory().setStackInSlot(i, ItemStack.EMPTY);
        for (int i = 0; i < basin.getInputInventory().getSlots(); i++) basin.getInputInventory().setStackInSlot(i, ItemStack.EMPTY);
    }
}
