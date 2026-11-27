package dev.szx.dimensionworks.rpmlimit;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.networking.EnergyAcceptorBlockEntity;
import appeng.crafting.execution.CraftingSubmitResult;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksRpmLimit.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProductionIntegrationGameTests {
    /** 测试用计划仅隔离模式计算；提交、CPU、容量模拟与最终输出均调用真实 AE2。 */
    private record TestPlan(boolean simulation) implements ICraftingPlan {
        public GenericStack finalOutput() { return new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1); }
        public long bytes() { return 1; }
        public boolean multiplePaths() { return false; }
        public KeyCounter usedItems() { return new KeyCounter(); }
        public KeyCounter emittedItems() {
            var emitted = new KeyCounter(); emitted.add(AEItemKey.of(Items.IRON_INGOT), 1); return emitted;
        }
        public KeyCounter missingItems() { return new KeyCounter(); }
        public Map<appeng.api.crafting.IPatternDetails, Long> patternTimes() { return Map.of(); }
    }

    private static final class TestRequester implements appeng.api.networking.crafting.ICraftingRequester {
        private final appeng.api.networking.IGridNode node;
        long received;
        TestRequester(appeng.api.networking.IGridNode node) { this.node = node; }
        public appeng.api.networking.IGridNode getActionableNode() { return node; }
        public com.google.common.collect.ImmutableSet<appeng.api.networking.crafting.ICraftingLink> getRequestedJobs() {
            return com.google.common.collect.ImmutableSet.of();
        }
        public long insertCraftedItems(appeng.api.networking.crafting.ICraftingLink link, appeng.api.stacks.AEKey key,
                                      long amount, appeng.api.config.Actionable mode) {
            if (mode == appeng.api.config.Actionable.MODULATE) received += amount;
            return amount;
        }
        public void jobStateChange(appeng.api.networking.crafting.ICraftingLink link) {}
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void actualAe2GridAndCraftingCpuLoadWithSubmissionHooks(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 1), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:energy_acceptor")).defaultBlockState());
        h.setBlock(new BlockPos(2, 1, 1), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:crafting_unit")).defaultBlockState());
        h.setBlock(new BlockPos(3, 1, 1), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:1k_crafting_storage")).defaultBlockState());
        h.setBlock(new BlockPos(1, 1, 2), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")).defaultBlockState());
        h.setBlock(new BlockPos(1, 1, 3), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:1k_crafting_storage")).defaultBlockState());
        h.runAfterDelay(30, () -> {
            var be = (EnergyAcceptorBlockEntity) h.getBlockEntity(new BlockPos(1, 1, 1));
            var grid = be.getMainNode().getNode().getGrid();
            var service = grid.getCraftingService();
            h.assertTrue(!service.getCpus().isEmpty(), "实际 AE2 CPU 多方块结构必须建立");
            var source = appeng.api.networking.security.IActionSource.empty();
            var requester = new TestRequester(grid.getPivot());
            h.assertTrue(service.submitJob(new TestPlan(true), null, null, false, source)
                == CraftingSubmitResult.INCOMPLETE_PLAN, "自研提交拦截不得改变 AE2 原生模拟分支");
            var player = GearHeartEffectGameTests.equipped(h, new GearHeartEffectGameTests.RecordingConnection());
            try {
                var cpus = service.getCpus().stream()
                    .map(cpu -> (appeng.me.cluster.implementations.CraftingCPUCluster) cpu).toList();
                h.assertTrue(cpus.size() == 2, "测试必须建立两个独立、供电正常的实际 CPU");
                h.assertTrue(service.submitJob(new TestPlan(false), requester, cpus.get(0), false, source).successful(),
                    "未修复的玩家应能提交第一项实际任务");
                h.assertTrue(cpus.get(0).isBusy() && !cpus.get(1).isBusy(), "首项提交必须只占用第一个 CPU");
                h.assertTrue(service.submitJob(new TestPlan(false), requester, cpus.get(1), false, source)
                    == CraftingSubmitResult.NO_CPU_FOUND && !cpus.get(1).isBusy(),
                    "第二个 CPU 虽空闲，未解除的单任务配额仍须拒绝提交");
                for (int stage = 0; stage < 4; stage++) GearHeartEffects.repairEquippedCurse(player, 1 << stage);
                h.assertTrue(service.submitJob(new TestPlan(false), requester, cpus.get(1), false, source).successful(),
                    "修复第四阶段后必须立即允许向第二个 CPU 提交");
                var key = AEItemKey.of(Items.IRON_INGOT);
                var logic = cpus.get(0).craftingLogic;
                h.assertTrue(logic.insert(key, 1, appeng.api.config.Actionable.SIMULATE) == 1,
                    "最终产物的容量模拟不能消耗生产配额");
                h.assertTrue(logic.insert(key, 1, appeng.api.config.Actionable.MODULATE) == 0
                    && logic.getWaitingFor(key) == 1 && requester.received == 0, "最终输出配额拒绝时必须保留等待量");
                for (int stage = 4; stage < 7; stage++) GearHeartEffects.repairEquippedCurse(player, 1 << stage);
                h.assertTrue(logic.insert(key, 1, appeng.api.config.Actionable.MODULATE) == 1 && requester.received == 1,
                    "完全修复后必须允许实际最终输出");
            } finally { GearHeartEffectGameTests.close(player); }

            h.succeed();
        });
    }
}
