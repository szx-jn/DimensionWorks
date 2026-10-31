package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.api.IRuntimeKineticMember;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticPersistence;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksRpmLimit.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KineticAccountingGameTests {
    private static KineticBlockEntity block(GameTestHelper helper, String id, BlockPos pos) {
        helper.setBlock(pos, net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
            new net.minecraft.resources.ResourceLocation("create", id)).defaultBlockState());
        return (KineticBlockEntity) helper.getBlockEntity(pos);
    }
    private static void highLimit(KineticBlockEntity member) {
        member.getPersistentData().putInt("dimensionworks_machine_rpm_limit", 10240);
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void actualContributionsSurviveNbtRoundTrip(GameTestHelper helper) {
        block(helper, "creative_motor", new BlockPos(2, 1, 2));
        block(helper, "mechanical_press", new BlockPos(5, 1, 2));
        helper.runAfterDelay(20, () -> {
            KineticBlockEntity motor = (KineticBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
            KineticBlockEntity press = (KineticBlockEntity) helper.getBlockEntity(new BlockPos(5, 1, 2));
            highLimit(motor); highLimit(press);
            KineticNetwork network = motor.getOrCreateNetwork();
            press.setNetwork(motor.network);
            KineticNetworkOptimizer.invalidate(network);
            press.setSpeed(1024);
            helper.assertTrue(OverspeedBonus.stressMultiplier(press) > 1, "Fixture must exercise overspeed");
            List<KineticBlockEntity> members = new ArrayList<>(network.members.keySet());
            float expectedStress = 0, expectedCapacity = 0;
            for (KineticBlockEntity member : members) {
                expectedStress += network.getActualStressOf(member);
                if (network.sources.containsKey(member)) expectedCapacity += network.getActualCapacityOf(member);
            }
            List<CompoundTag> saved = new ArrayList<>();
            for (KineticBlockEntity member : members) saved.add(member.saveWithFullMetadata());
            KineticNetwork restored = new KineticNetwork();
            // Decode the saved accounting into a fresh network, then load in reverse order.
            for (int i = members.size() - 1; i >= 0; i--) KineticPersistence.read(members.get(i), saved.get(i), false);
            KineticPersistence.initialize(members.get(0), restored);
            for (int i = members.size() - 1; i >= 0; i--) restored.addSilently(members.get(i), 0, 0);
            helper.assertTrue(Math.abs(restored.calculateStress() - expectedStress) < .1f, "Load duplicated overspeed stress");
            helper.assertTrue(Math.abs(restored.calculateCapacity() - expectedCapacity) < .1f, "Load duplicated natural capacity");
            helper.assertTrue(restored.getSize() == members.size(), "Member count did not settle after restore");
            helper.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void virtualMembersAreExcludedFromPersistentTotals(GameTestHelper helper) {
        block(helper, "creative_motor", new BlockPos(2, 1, 2));
        helper.runAfterDelay(20, () -> {
            KineticBlockEntity motor = (KineticBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
            KineticNetwork network = motor.getOrCreateNetwork();
            float physical = 0;
            for (KineticBlockEntity member : network.members.keySet()) physical += network.getActualStressOf(member);
            int count = network.getSize();
            RuntimeMember virtual = new RuntimeMember(motor);
            network.members.put(virtual, 256f);
            KineticNetworkOptimizer.invalidate(network);
            CompoundTag saved = motor.saveWithFullMetadata().getCompound("DWKineticAccounting");
            helper.assertTrue(Math.abs(saved.getFloat("Stress") - physical) < .1f, "Virtual stress leaked into saved totals");
            helper.assertTrue(saved.getInt("Members") == count, "Virtual member leaked into saved count");
            network.members.remove(virtual); KineticNetworkOptimizer.invalidate(network);
            helper.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void lowestParticipantLimitRecoversAfterRemoval(GameTestHelper helper) {
        block(helper, "creative_motor", new BlockPos(2, 1, 2));
        block(helper, "shaft", new BlockPos(5, 1, 2));
        helper.runAfterDelay(20, () -> {
            KineticBlockEntity motor = (KineticBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
            KineticBlockEntity shaft = (KineticBlockEntity) helper.getBlockEntity(new BlockPos(5, 1, 2));
            highLimit(motor); motor.setSpeed(1024);
            shaft.getPersistentData().putInt("dimensionworks_machine_rpm_limit", 64);
            shaft.setNetwork(motor.network);
            KineticNetwork network = motor.getOrCreateNetwork();
            KineticNetworkOptimizer.onTopologyChanged(network);
            helper.assertTrue(Math.abs(RpmLimitManager.clamp(motor, 1024)) == 64, "Lowest member was ignored");
            shaft.onChunkUnloaded();
            shaft.setRemoved();
            helper.assertTrue(network.members.containsKey(shaft), "Fixture must retain the removed member before map cleanup");
            helper.assertTrue(Math.abs(RpmLimitManager.clamp(motor, 1024)) == 1024, "Unloaded member retained a stale limit");
            shaft.setNetwork(null);
            helper.assertTrue(Math.abs(RpmLimitManager.clamp(motor, 1024)) == 1024, "Removed member left a stale network limit");
            helper.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void stoppedSourceStillDeductsItsSavedCapacity(GameTestHelper helper) {
        KineticBlockEntity shaft = block(helper, "shaft", new BlockPos(2, 1, 2));
        CompoundTag accounting = new CompoundTag();
        accounting.putInt("Version", 1); accounting.putFloat("Capacity", 65536);
        accounting.putFloat("OwnCapacity", 65536); accounting.putInt("Members", 1);
        CompoundTag saved = new CompoundTag(); saved.put("DWKineticAccounting", accounting);
        KineticPersistence.read(shaft, saved, false);
        KineticNetwork restored = new KineticNetwork();
        KineticPersistence.initialize(shaft, restored);
        restored.addSilently(shaft, 0, 0);
        helper.assertTrue(restored.calculateCapacity() == 0, "Stopped source retained its old capacity");
        helper.assertTrue(restored.getSize() == 1, "Stopped source was not restored exactly once");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void legacyPhantomTotalsAreNotImported(GameTestHelper helper) {
        KineticBlockEntity shaft = block(helper, "shaft", new BlockPos(2, 1, 2));
        CompoundTag legacy = new CompoundTag();
        CompoundTag networkTag = new CompoundTag();
        networkTag.putFloat("Stress", 999999); networkTag.putFloat("Capacity", 999999);
        legacy.put("Network", networkTag);
        KineticPersistence.read(shaft, legacy, false);
        KineticNetwork restored = new KineticNetwork();
        KineticPersistence.initialize(shaft, restored);
        restored.addSilently(shaft, 0, 0);
        helper.assertTrue(restored.calculateStress() == 0, "Legacy phantom stress was imported");
        helper.assertTrue(restored.calculateCapacity() == 0, "Legacy phantom capacity was imported");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void topologyFocusAndIconsSurvivePaginationAndPacket(GameTestHelper helper) {
        block(helper, "creative_motor", new BlockPos(2, 1, 2));
        helper.runAfterDelay(20, () -> {
            KineticBlockEntity motor = (KineticBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
            KineticNetwork network = motor.getOrCreateNetwork();
            BlockPos clickedMachine = motor.getBlockPos().offset(1000, 0, 0);
            java.util.List<RuntimeMember> virtuals = new java.util.ArrayList<>();
            for (int i = 0; i < 256; i++) {
                RuntimeMember member = new RuntimeMember(motor, motor.getBlockPos().offset(i + 1, 0, 0));
                virtuals.add(member);
                network.members.put(member, 0f);
            }
            RuntimeMember clicked = new RuntimeMember(motor, clickedMachine);
            virtuals.add(clicked);
            network.members.put(clicked, 0f);
            try {
                var snapshot = dev.szx.dimensionworks.rpmlimit.diagnostics.NetworkSnapshot.capture(motor, -1, clickedMachine);
                helper.assertTrue(snapshot.page() == 1, "Opening did not choose the page containing the clicked node");
                helper.assertTrue(snapshot.focus().equals(clickedMachine), "Virtual machine focus was replaced by its input shaft");
                net.minecraft.network.FriendlyByteBuf buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    snapshot.encode(buffer);
                    var decoded = dev.szx.dimensionworks.rpmlimit.diagnostics.NetworkSnapshot.decode(buffer);
                    helper.assertTrue(decoded.origin().equals(clickedMachine) && decoded.focus().equals(clickedMachine),
                        "Entry/focus coordinates changed during packet serialization");
                    var node = decoded.nodes().stream().filter(n -> n.pos().equals(clickedMachine)).findFirst().orElseThrow();
                    helper.assertTrue(node.icon().is(motor.getBlockState().getBlock().asItem()), "Node lost its inventory item icon");
                    helper.assertTrue(node.icon().getCount() == 1 && !node.icon().hasTag(), "Icon must not carry machine inventory NBT");
                } finally { buffer.release(); }
            } finally {
                for (RuntimeMember member : virtuals) network.members.remove(member);
                KineticNetworkOptimizer.invalidate(network);
            }
            helper.succeed();
        });
    }
    private static final class RuntimeMember extends KineticBlockEntity implements IRuntimeKineticMember {
        RuntimeMember(KineticBlockEntity real) { this(real, real.getBlockPos()); }
        RuntimeMember(KineticBlockEntity real, BlockPos pos) { super(real.getType(), pos, real.getBlockState()); }
        @Override public float getTheoreticalSpeed() { return 64; }
    }
}
