package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksRpmLimit.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RpmPersistenceGameTests {
    private static CompoundTag repairedOfflinePlayer() {
        CompoundTag heart = new CompoundTag(); heart.putString("id", "kubejs:gear_heart_enchanted");
        heart.putByte("Count", (byte) 1);
        CompoundTag tag = new CompoundTag(); tag.putInt(GearHeartState.REPAIRED_MASK_KEY, 127); heart.put("tag", tag);
        ListTag items = new ListTag(); items.add(heart);
        CompoundTag stacks = new CompoundTag(); stacks.put("Items", items);
        CompoundTag handler = new CompoundTag(); handler.put("Stacks", stacks);
        CompoundTag slot = new CompoundTag(); slot.putString("Identifier", "gear_heart"); slot.put("StacksHandler", handler);
        ListTag slots = new ListTag(); slots.add(slot);
        CompoundTag inventory = new CompoundTag(); inventory.put("Curios", slots);
        CompoundTag caps = new CompoundTag(); caps.put("curios:inventory", inventory);
        CompoundTag player = new CompoundTag(); player.put("ForgeCaps", caps); return player;
    }
    @GameTest(template = "empty")
    public static void repairedOfflineOwnerRecoversLost512Limit(GameTestHelper helper) {
        RpmPlayerState.Profile profile = RpmPlayerState.fromPlayerNbt(repairedOfflinePlayer());
        helper.assertTrue(profile.base() == 512 && profile.effective() == 512,
            "Fully repaired offline owner was reset to the default 32 RPM");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void repairMigrationPreservesExplicitCommandLimit(GameTestHelper helper) {
        CompoundTag player = repairedOfflinePlayer(), data = new CompoundTag();
        data.putInt(RpmPlayerState.LIMIT, 64); data.putBoolean(RpmPlayerState.OVERRIDE, true);
        player.put("ForgeData", data);
        RpmPlayerState.Profile profile = RpmPlayerState.fromPlayerNbt(player);
        helper.assertTrue(profile.base() == 64 && profile.effective() == 64 && profile.commandOverride(),
            "Repair migration overwrote an explicit command limit");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void rpmAndCommandOverrideSurvivePlayerClone(GameTestHelper helper) {
        CompoundTag oldData = new CompoundTag(), newData = new CompoundTag();
        oldData.putInt(RpmPlayerState.LIMIT, 10240); oldData.putBoolean(RpmPlayerState.INITIALIZED, true);
        oldData.putBoolean(RpmPlayerState.OVERRIDE, true); RpmPlayerState.persist(oldData);
        // Forge only guarantees PlayerPersisted data across a player replacement.
        CompoundTag persistedOnly = new CompoundTag();
        persistedOnly.put(Player.PERSISTED_NBT_TAG, oldData.getCompound(Player.PERSISTED_NBT_TAG).copy());
        RpmPlayerState.copy(persistedOnly, newData);
        helper.assertTrue(newData.getInt(RpmPlayerState.LIMIT) == 10240 && newData.getBoolean(RpmPlayerState.OVERRIDE),
            "Clone lost the stored RPM or command override");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void unownedRelayInheritsRealParticipantInsteadOfDefault32(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 2), b = new BlockPos(5, 1, 2);
        var motorBlock = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("create", "creative_motor"));
        var shaftBlock = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("create", "shaft"));
        helper.setBlock(a, motorBlock.defaultBlockState()); helper.setBlock(b, shaftBlock.defaultBlockState());
        helper.runAfterDelay(20, () -> {
            var motor = (com.simibubi.create.content.kinetics.base.KineticBlockEntity) helper.getBlockEntity(a);
            var shaft = (com.simibubi.create.content.kinetics.base.KineticBlockEntity) helper.getBlockEntity(b);
            motor.getPersistentData().putUUID("dimensionworks_owner", new java.util.UUID(104, motor.getBlockPos().asLong()));
            motor.getPersistentData().putInt("dimensionworks_machine_rpm_limit", 512);
            shaft.setNetwork(motor.network);
            var network = motor.getOrCreateNetwork();
            dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer.invalidate(network);
            shaft.setSpeed(256);
            helper.assertTrue(shaft.getTheoreticalSpeed() == 256 && shaft.getSpeed() == 256,
                "Unowned relay was irreversibly capped to default RPM");
            helper.assertTrue(RpmLimitManager.clamp(motor, 512) == 512,
                "Unowned relay lowered the whole network to default RPM");
            helper.succeed();
        });
    }
}
