package dev.szx.dimensionworks.transfer;

import com.mojang.authlib.GameProfile;
import dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(WDTransfer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TransferAeBlockGameTests {
    private static ServerLevel transfer(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(WDKeys.TRANSFER_LEVEL);
        helper.assertTrue(level != null, "Transfer dimension must be loaded");
        return level;
    }

    private static Block block(String id) {
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        if (block == null || block == Blocks.AIR) throw new AssertionError("Missing runtime block: " + id);
        return block;
    }

    private static BlockPos pos(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(3, 0, 3)).offset(4096, 0, 4096);
    }

    private static FakePlayer player(ServerLevel level) {
        return FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "[AERestrictionTest]"));
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    @GameTest(template = "empty")
    public static void deniesAeAndAddonWritesButKeepsOtherDimensions(GameTestHelper helper) {
        var level = transfer(helper);
        var pos = pos(helper);
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        for (var entry : ForgeRegistries.BLOCKS.getEntries()) {
            if (java.util.Set.of("ae2", "megacells", "appmek").contains(entry.getKey().location().getNamespace())) {
                var state = entry.getValue().defaultBlockState();
                helper.assertTrue(TransferBlockPolicy.forbidden(state), "All loaded AE/addon blocks must be recognized");
                helper.assertTrue(!level.setBlockAndUpdate(pos, state), "Level writes must refuse " + entry.getKey());
                helper.assertTrue(level.getChunkAt(pos).setBlockState(pos, state, false) == null,
                    "Direct chunk writes must refuse " + entry.getKey());
                helper.assertTrue(level.getBlockState(pos).is(Blocks.STONE), "Rejected writes must retain the old block");
            }
        }
        helper.setBlock(new BlockPos(3, 2, 3), block("ae2:charger"));
        helper.assertBlockPresent(block("ae2:charger"), new BlockPos(3, 2, 3));
        helper.assertTrue(level.setBlockAndUpdate(pos, Blocks.OAK_PLANKS.defaultBlockState()), "Ordinary construction must work");
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void survivalAndCreativePlacementKeepItems(GameTestHelper helper) {
        var level = transfer(helper);
        var pos = pos(helper);
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        var player = player(level);
        try {
            for (boolean creative : new boolean[]{false, true}) {
                player.getAbilities().instabuild = creative;
                var stack = new ItemStack(block("ae2:charger"), 3);
                stack.getOrCreateTag().putString("preserve", "original-nbt");
                var before = stack.copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var context = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos.below())));
                var result = ((BlockItem) stack.getItem()).place(context);
                helper.assertTrue(result == InteractionResult.FAIL, "Survival and creative must reject block placement");
                helper.assertTrue(ItemStack.matches(before, stack), "Rejected placement must keep count and NBT");
                helper.assertTrue(level.getBlockState(pos).isAir(), "No AE block may appear");
            }
        } finally { player.discard(); level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState()); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deniesAePartsBeforeCreatingHostOrConsumingItems(GameTestHelper helper) {
        var level = transfer(helper);
        var pos = pos(helper);
        var player = player(level);
        var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ae2:fluix_glass_cable")), 3);
        helper.assertTrue(!stack.isEmpty(), "Actual AE cable item must exist");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        try {
            var type = Class.forName("appeng.parts.PartPlacement");
            var result = type.getMethod("place", UseOnContext.class)
                .invoke(null, new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
            helper.assertTrue(result == InteractionResult.FAIL && stack.getCount() == 3, "Part placement must fail before consumption");
            var direct = type.getMethod("placePart", net.minecraft.world.entity.player.Player.class,
                net.minecraft.world.level.Level.class, Class.forName("appeng.api.parts.IPartItem"),
                CompoundTag.class, BlockPos.class, Direction.class)
                .invoke(null, player, level, stack.getItem(), null, pos, Direction.UP);
            helper.assertTrue(direct == null && level.getBlockState(pos).isAir(), "Direct part API must not create a cable host");
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        finally { player.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cardboardAndMobilePackagerUnboxKeepPayload(GameTestHelper helper) {
        var level = transfer(helper);
        var pos = pos(helper);
        var player = player(level);
        player.setShiftKeyDown(true);
        try {
            for (boolean mobile : new boolean[]{false, true}) {
                level.setBlockAndUpdate(pos, block("mekanism:cardboard_box").defaultBlockState());
                var tile = level.getBlockEntity(pos);
                helper.assertTrue(tile != null, "Actual Mekanism box tile must exist");
                CompoundTag nbt = new CompoundTag();
                CompoundTag data = new CompoundTag();
                data.put("blockState", NbtUtils.writeBlockState(block("ae2:charger").defaultBlockState()));
                CompoundTag payload = new CompoundTag();
                payload.putString("preserve", "full-machine-inventory");
                data.put("tileTag", payload);
                data.putBoolean("create_mobile_packages:from_mobile_packager", mobile);
                nbt.put("data", data);
                nbt.putBoolean("create_mobile_packages:from_mobile_packager", mobile);
                tile.load(nbt);
                var before = tile.saveWithoutMetadata().copy();
                var result = level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit(pos));
                helper.assertTrue(result == InteractionResult.FAIL, "AE cardboard/tool unboxing must fail");
                helper.assertTrue(level.getBlockEntity(pos) == tile && before.equals(tile.saveWithoutMetadata()),
                    "Rejected unboxing must preserve box, full NBT and CMP origin");
                helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty(),
                    "Rejected unboxing must not drop an empty box");
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        } finally { player.discard(); level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allowedUnboxingAndExistingBlockUpdatesStillWork(GameTestHelper helper) {
        var level = transfer(helper);
        var pos = pos(helper);
        var player = player(level);
        player.setShiftKeyDown(true);
        try {
            level.setBlockAndUpdate(pos, block("mekanism:cardboard_box").defaultBlockState());
            CompoundTag nbt = new CompoundTag();
            CompoundTag data = new CompoundTag();
            data.put("blockState", NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
            nbt.put("data", data);
            level.getBlockEntity(pos).load(nbt);
            var result = level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit(pos));
            helper.assertTrue(result.consumesAction() && level.getBlockState(pos).is(Blocks.STONE), "Ordinary unboxing must work");
            // Simulate a pre-existing save / Forge undo, then verify updates and removal remain possible.
            level.restoringBlockSnapshots = true;
            try { level.setBlockAndUpdate(pos, block("ae2:charger").defaultBlockState()); }
            finally { level.restoringBlockSnapshots = false; }
            helper.assertTrue(level.getBlockState(pos).is(block("ae2:charger")), "Forge snapshot restoration must work");
            var rotated = level.getBlockState(pos).rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
            helper.assertTrue(!TransferBlockPolicy.deniesWrite(level, pos, rotated), "Existing machine state updates must work");
            helper.assertTrue(level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()), "Existing machines can be removed");
        } finally { player.discard(); level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
        helper.succeed();
    }
}
