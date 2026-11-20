package dev.szx.dimensionworks.droplifetime;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksDropLifetime.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DropLifetimeGameTests {
    private DropLifetimeGameTests() {}

    @GameTest(template = "empty")
    public static void unclaimedDropsUseComponentRarity(GameTestHelper helper) {
        Rarity[] rarities = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
        int[] expected = {500, 1000, 1500, 3600};
        for (int i = 0; i < rarities.length; i++) {
            ItemStack stack = new ItemStack(Items.DIAMOND);
            stack.set(DataComponents.RARITY, rarities[i]);
            ItemEntity item = new ItemEntity(helper.getLevel(), 0, 0, 0, stack);
            helper.getLevel().addFreshEntity(item);
            helper.assertTrue(item.lifespan == expected[i], "掉落寿命未按组件稀有度设置：" + rarities[i]);
            item.discard();
        }
        helper.succeed();
    }
}
