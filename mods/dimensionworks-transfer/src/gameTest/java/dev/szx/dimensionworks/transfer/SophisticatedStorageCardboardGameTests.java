package dev.szx.dimensionworks.transfer;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dimensionworks_cardboard_storage")
@PrefixGameTestTemplate(false)
public final class SophisticatedStorageCardboardGameTests {
    private static final int FLAGS = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static FakePlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "[StorageCardboardTest]"));
    }

    private static InteractionResult pack(FakePlayer player, BlockPos pos) {
        ItemStack box = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("dimensionworks_transfer:advanced_packager")));
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        return box.getItem().onItemUseFirst(box, new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
    }

    private static void clear(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(ItemEntity::discard);
    }

    @GameTest(template = "empty", templateNamespace = "dimensionworks_cardboard")
    public static void sophisticatedStorageInventorySurvivesPacking(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var player = player(helper);
        try {
            for (String id : new String[] {"sophisticatedstorage:chest", "sophisticatedstorage:barrel"}) {
                var storage = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
                helper.assertTrue(storage != Blocks.AIR, "Missing test block: " + id);
                level.setBlock(pos, storage.defaultBlockState(), FLAGS);

                var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                helper.assertTrue(inventory != null, id + " must expose its inventory");
                helper.assertTrue(inventory.insertItem(0, new ItemStack(Items.DIAMOND, 7), false).isEmpty(),
                    id + " must accept test contents");

                helper.assertTrue(pack(player, pos).consumesAction(), id + " must pack");
                var box = (mekanism.common.tile.TileEntityCardboardBox) level.getBlockEntity(pos);
                var payload = box.components().get(mekanism.common.registries.MekanismDataComponents.BLOCK_DATA.get());
                helper.assertTrue(payload != null && payload.blockEntityTag().getBoolean("packed"),
                    id + " must be marked packed before the payload snapshot");
                helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                    .noneMatch(entity -> entity.getItem().is(Items.DIAMOND)), id + " contents must not spill");

                player.setShiftKeyDown(true);
                level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
                var restored = level.getBlockEntity(pos);
                helper.assertTrue(!restored.saveWithoutMetadata(level.registryAccess()).getBoolean("packed"),
                    id + " must be active after unpacking");
                var restoredInventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                helper.assertTrue(restoredInventory != null && restoredInventory.getStackInSlot(0).is(Items.DIAMOND)
                    && restoredInventory.getStackInSlot(0).getCount() == 7, id + " inventory must survive packing");
                clear(level, pos);
            }
        } finally {
            player.discard();
            clear(level, pos);
        }
        helper.succeed();
    }
}
