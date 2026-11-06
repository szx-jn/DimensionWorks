package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.memory.CloudDiskSavedData;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksMekStress.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CloudDiskGameTests {
    @GameTest(template = "empty")
    public static void preservesPayloadAndOwnerAcrossSave(GameTestHelper helper) {
        CloudDiskSavedData data = new CloudDiskSavedData();
        UUID owner = UUID.randomUUID();
        ItemStack ball = new ItemStack(DimensionWorksMekStress.MATTER_BALL.get());
        CompoundTag payload = new CompoundTag();
        payload.putString("Origin", "twilightforest:twilight_forest");
        payload.putInt("Amount", 16000);
        ball.getOrCreateTag().put("MatterBall", payload);
        data.deposit(owner, List.of(ball));
        ball.setCount(0);
        CloudDiskSavedData loaded = CloudDiskSavedData.load(data.save(new CompoundTag()));
        helper.assertTrue(loaded.size(owner) == 1, "Stored ball survives save/load and caller mutation");
        helper.assertTrue(payload.equals(loaded.view(owner, 0).getTag().getCompound("MatterBall")), "Complete NBT survives save/load");
        helper.assertTrue(loaded.size(UUID.randomUUID()) == 0, "Other players cannot see this stock");
        loaded.view(owner, 0).setCount(0);
        helper.assertTrue(loaded.view(owner, 0).getCount() == 1, "Previews cannot mutate cloud stock");
        helper.assertTrue(loaded.consumeNotices(owner) == 1 && loaded.consumeNotices(owner) == 0, "Notify once per delivery");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void keepsFullInventoryAndPartialClaimRemainders(GameTestHelper helper) {
        CloudDiskSavedData data = new CloudDiskSavedData();
        UUID owner = UUID.randomUUID();
        data.deposit(owner, List.of(new ItemStack(Items.DIAMOND, 12)));
        helper.assertTrue(!data.claim(owner, 0, stack -> {}), "Full inventory cannot claim stock");
        helper.assertTrue(data.view(owner, 0).getCount() == 12, "Full inventory keeps all stock");
        helper.assertTrue(data.claim(owner, 0, stack -> stack.shrink(5)), "Partial insertion claims only space available");
        helper.assertTrue(data.view(owner, 0).getCount() == 7, "Partial insertion preserves remainder");
        CloudDiskSavedData loaded = CloudDiskSavedData.load(data.save(new CompoundTag()));
        helper.assertTrue(loaded.claim(owner, 0, stack -> stack.setCount(0)) && loaded.size(owner) == 0, "Last claim removes stock");
        helper.assertTrue(!loaded.claim(owner, 0, stack -> { throw new AssertionError("Duplicate claim"); }), "Duplicate claims fail");
        helper.assertTrue(!loaded.claim(owner, -1, stack -> { throw new AssertionError("Invalid index"); }), "Invalid indices fail");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void splitsUpgradeStacksAndCountsDeliveries(GameTestHelper helper) {
        CloudDiskSavedData data = new CloudDiskSavedData();
        UUID owner = UUID.randomUUID();
        data.deposit(owner, List.of(new ItemStack(Items.DIAMOND, 130)));
        data.deposit(owner, List.of(new ItemStack(Items.EMERALD)));
        helper.assertTrue(data.size(owner) == 4 && data.view(owner, 0).getCount() == 64
            && data.view(owner, 2).getCount() == 2, "Over-sized returned upgrades split into legal stacks");
        helper.assertTrue(data.consumeNotices(owner) == 2 && data.consumeNotices(owner) == 0, "Each delivery gets exactly one notice");
        helper.succeed();
    }
}
