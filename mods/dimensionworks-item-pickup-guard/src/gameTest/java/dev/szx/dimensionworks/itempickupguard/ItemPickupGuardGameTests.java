package dev.szx.dimensionworks.itempickupguard;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksItemPickupGuard.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ItemPickupGuardGameTests {
    private ItemPickupGuardGameTests() {}

    @GameTest(template = "empty")
    public static void discardedEntityCannotBeTransferredAgain(GameTestHelper helper) {
        ItemEntity item = new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.DIAMOND, 4));
        ItemStack firstTransfer = item.getItem().copy();
        item.discard();
        helper.assertTrue(firstTransfer.getCount() == 4, "第一次转移必须保留全部物品");
        helper.assertTrue(item.getItem().isEmpty(), "第二个拾取器不能再次获取已移除实体的物品");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void partialTransferKeepsRemainderAvailable(GameTestHelper helper) {
        ItemEntity item = new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.DIAMOND, 4));
        ItemStack firstTransfer = item.getItem().split(1);
        helper.assertTrue(firstTransfer.getCount() == 1, "首次部分转移数量不正确");
        helper.assertTrue(item.getItem().getCount() == 3, "存活实体的剩余物品必须仍可拾取");
        helper.assertTrue(item.getItem().split(3).getCount() == 3, "后续拾取器必须能转移剩余物品");
        item.discard();
        helper.succeed();
    }
}
