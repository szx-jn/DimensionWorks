package dev.szx.dimensionworks.maidaecrank;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.orientation.OrientationStrategies;
import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.core.definitions.AEBlocks;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksMaidAECrank.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MaidCrankGameTests {
    private static final BlockPos NEAR = new BlockPos(3, 1, 3);

    @GameTest(template = "empty", timeoutTicks = 10)
    public static void extensionRegistrationAndEntitySaveRetainTask(GameTestHelper helper) {
        var id = ResourceLocation.fromNamespaceAndPath(DimensionWorksMaidAECrank.MOD_ID, "ae2_crank_task");
        var task = TaskManager.findTask(id).orElseThrow();
        helper.assertTrue(task instanceof MaidAE2CrankTask, "Native LittleMaid extension must register the task");
        var icon = task.getIcon();
        icon.setCount(12);
        helper.assertTrue(task.getIcon().getCount() == 1 && task.getIcon().is(AEBlocks.CRANK.asItem()),
            "Task icon callers must not mutate shared state");
        EntityMaid maid = maid(helper, NEAR.west());
        maid.setTask(task);
        maid.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
        CompoundTag saved = new CompoundTag();
        maid.saveWithoutId(saved);
        var restored = new EntityMaid(helper.getLevel());
        restored.load(saved);
        helper.assertTrue(restored.getTask().getUid().equals(id) && restored.getUUID().equals(maid.getUUID())
            && restored.getMainHandItem().getCount() == 7 && restored.getMainHandItem().is(net.minecraft.world.item.Items.DIAMOND),
            "Current native save must retain task, identity and inventory");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void nearMaidPowersNativeChargerWithoutFakePlayer(GameTestHelper helper) {
        var charger = charger(helper, NEAR);
        var maid = maid(helper, NEAR.west());
        var task = new UseAE2Crank();
        helper.assertTrue(task.checkExtraStartConditions(helper.getLevel(), maid), "Nearby available crank must start");
        task.start(helper.getLevel(), maid, helper.getLevel().getGameTime());
        task.tick(helper.getLevel(), maid, helper.getLevel().getGameTime());
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(charger.getInternalCurrentPower() == 160,
                "Native crank rotation must deliver exactly 160 AE");
            helper.assertBlockPresent(AEBlocks.CRANK.block(), NEAR);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void fullChargerDoesNotStartOrBreakCrank(GameTestHelper helper) {
        var charger = charger(helper, NEAR);
        charger.injectExternalPower(PowerUnit.AE, charger.getInternalMaxPower(), Actionable.MODULATE);
        var maid = maid(helper, NEAR.west());
        var task = new UseAE2Crank();
        helper.assertTrue(!task.checkExtraStartConditions(helper.getLevel(), maid), "Full charger must not start work");
        for (int tick = 0; tick < 250; tick++) task.tick(helper.getLevel(), maid, tick);
        helper.runAfterDelay(25, () -> {
            helper.assertBlockPresent(AEBlocks.CRANK.block(), NEAR);
            helper.assertTrue(charger.getInternalCurrentPower() == charger.getInternalMaxPower(),
                "No invalid turn may drain or alter a full charger");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void distantMaidWalksBeforeTurning(GameTestHelper helper) {
        var charger = charger(helper, NEAR);
        var maid = maid(helper, new BlockPos(9, 1, 3));
        var task = new UseAE2Crank();
        helper.assertTrue(!task.checkExtraStartConditions(helper.getLevel(), maid), "Distant maid must walk first");
        var walk = maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElseThrow();
        helper.assertTrue(walk.getTarget().currentBlockPosition().equals(helper.absolutePos(NEAR.west())),
            "Walk target must be the outside of the actual crank");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(charger.getInternalCurrentPower() == 0, "Out-of-reach maid must not power the charger");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void closestUsableCrankIsSelected(GameTestHelper helper) {
        var near = charger(helper, NEAR);
        var far = charger(helper, NEAR.east(5));
        var maid = maid(helper, NEAR.west());
        var task = new UseAE2Crank();
        helper.assertTrue(task.checkExtraStartConditions(helper.getLevel(), maid), "Nearest usable crank should start");
        task.start(helper.getLevel(), maid, 0);
        task.tick(helper.getLevel(), maid, 0);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(near.getInternalCurrentPower() == 160 && far.getInternalCurrentPower() == 0,
                "Only the closest usable native charger may receive the turn");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 10)
    public static void removingTargetStopsWorkAndClearsMovement(GameTestHelper helper) {
        charger(helper, NEAR);
        var maid = maid(helper, NEAR.west());
        var task = new UseAE2Crank();
        helper.assertTrue(task.checkExtraStartConditions(helper.getLevel(), maid), "Nearby crank should start");
        task.start(helper.getLevel(), maid, 0);
        helper.assertTrue(maid.getScheduleDetail() == Activity.WORK, "Fixture must be in actual work schedule");
        helper.setBlock(NEAR, Blocks.AIR);
        helper.assertTrue(!task.canStillUse(helper.getLevel(), maid, 1), "Removed target must stop the behavior");
        task.stop(helper.getLevel(), maid, 1);
        helper.assertTrue(maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty()
            && maid.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).isEmpty(),
            "Stop must clear task movement and look memories");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 10)
    public static void unloadedSearchDoesNotCreateChunks(GameTestHelper helper) {
        var maid = new EntityMaid(helper.getLevel());
        maid.setHomeModeEnable(false);
        maid.moveTo(7340032.5, 64, 7340032.5, 0, 0);
        helper.assertTrue(!helper.getLevel().hasChunkAt(maid.blockPosition()), "Search fixture must be unloaded");
        int loaded = helper.getLevel().getChunkSource().getLoadedChunksCount();
        var task = new UseAE2Crank();
        helper.assertTrue(!task.checkExtraStartConditions(helper.getLevel(), maid), "Unloaded search must find no crank");
        helper.assertTrue(helper.getLevel().getChunkSource().getLoadedChunksCount() == loaded
            && !helper.getLevel().hasChunkAt(maid.blockPosition()), "Task must not create or load search chunks");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void actualBrainWalksAndTurnsWithoutManualTaskTicks(GameTestHelper helper) {
        for (int x = 0; x < 12; x++)
            for (int z = 0; z < 12; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        var charger = charger(helper, NEAR);
        var maid = maid(helper, new BlockPos(9, 1, 3));
        maid.setTame(true, true);
        maid.setOwnerUUID(new java.util.UUID(120, helper.absolutePos(NEAR).asLong()));
        maid.setTask(TaskManager.findTask(ResourceLocation.fromNamespaceAndPath(
            DimensionWorksMaidAECrank.MOD_ID, "ae2_crank_task")).orElseThrow());
        Vec3 initial = maid.position();
        helper.assertTrue(helper.getLevel().addFreshEntity(maid), "Native maid must enter the test world");
        helper.succeedWhen(() -> {
            helper.assertTrue(charger.getInternalCurrentPower() > 0, "Native maid brain must eventually power the charger");
            helper.assertTrue(maid.position().distanceToSqr(initial) > 1,
                "Actual navigation must move the maid before turning");
            helper.assertBlockPresent(AEBlocks.CRANK.block(), NEAR);
            maid.discard();
        });
    }

    private static EntityMaid maid(GameTestHelper helper, BlockPos pos) {
        var maid = new EntityMaid(helper.getLevel());
        maid.setHomeModeEnable(false);
        maid.setSchedule(MaidSchedule.ALL);
        maid.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(pos)));
        return maid;
    }

    private static ChargerBlockEntity charger(GameTestHelper helper, BlockPos crankPos) {
        BlockPos chargerPos = crankPos.east();
        helper.setBlock(chargerPos, AEBlocks.CHARGER.block().defaultBlockState());
        helper.setBlock(crankPos, OrientationStrategies.facing().setFacing(AEBlocks.CRANK.block().defaultBlockState(), Direction.WEST));
        var charger = (ChargerBlockEntity) helper.getBlockEntity(chargerPos);
        helper.assertTrue(AEBlocks.CRANK.block().getCrankable(helper.getBlockState(crankPos), helper.getLevel(), helper.absolutePos(crankPos)).canTurn(),
            "Actual native crank capability must accept work");
        return charger;
    }
}
