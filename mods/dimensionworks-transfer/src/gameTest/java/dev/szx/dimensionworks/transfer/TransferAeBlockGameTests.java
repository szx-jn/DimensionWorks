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
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.BuiltInRegistries;

@GameTestHolder(WDTransfer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TransferAeBlockGameTests {
    private static ServerLevel transfer(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level;
        try { level = dev.szx.dimensionworks.transfer.dispatch.DispatchLevel.getOrLoad(helper.getLevel().getServer(), WDKeys.TRANSFER_LEVEL); }
        catch (java.io.IOException e) { throw new AssertionError("Transfer data must load", e); }
        helper.assertTrue(level != null, "Transfer dimension must be loaded");
        return level;
    }

    private static Block block(String id) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
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
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
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
                net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("preserve", "original-nbt"));
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
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable")), 3);
        helper.assertTrue(!stack.isEmpty(), "Actual AE cable item must exist");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        try {
            var result = appeng.parts.PartPlacement.place(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
            helper.assertTrue(result == InteractionResult.FAIL && stack.getCount() == 3, "Part placement must fail before consumption");
            var direct = appeng.parts.PartPlacement.placePart(player, level, (appeng.api.parts.IPartItem<?>) stack.getItem(),
                net.minecraft.core.component.DataComponentMap.EMPTY, pos, Direction.UP);
            helper.assertTrue(direct == null && level.getBlockState(pos).isAir(), "Direct part API must not create a cable host");
        } finally { player.discard(); }
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
                CompoundTag payload = new CompoundTag();
                payload.putString("preserve", "full-machine-inventory");
                var boxTile = (mekanism.common.tile.TileEntityCardboardBox) tile;
                boxTile.setComponents(net.minecraft.core.component.DataComponentMap.builder().addAll(boxTile.components())
                    .set(mekanism.common.registries.MekanismDataComponents.BLOCK_DATA.get(),
                        new mekanism.common.attachments.BlockData(block("ae2:charger").defaultBlockState(), payload))
                    .set(de.theidler.create_mobile_packages.index.CMPDataComponents.PACKAGER_ORIGIN, mobile).build());
                var before = tile.saveWithoutMetadata(level.registryAccess()).copy();
                var result = level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
                helper.assertTrue(result == InteractionResult.FAIL, "AE cardboard/tool unboxing must fail");
                helper.assertTrue(level.getBlockEntity(pos) == tile && before.equals(tile.saveWithoutMetadata(level.registryAccess())),
                    "Rejected unboxing must preserve box, full NBT and CMP origin");
                helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty(),
                    "Rejected unboxing must not drop an empty box");
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        } finally { player.discard(); level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void realPackingAndBoxItemPlacementCannotBypassUnboxingGuard(GameTestHelper helper) {
        var source = helper.getLevel();
        var level = transfer(helper);
        var sourcePos = helper.absolutePos(new BlockPos(3, 2, 3));
        var pos = pos(helper);
        var packer = player(source);
        var receiver = player(level);
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        try {
            for (String itemId : new String[]{"dimensionworks_transfer:advanced_packager"}) {
                source.setBlockAndUpdate(sourcePos, block("ae2:charger").defaultBlockState());
                source.getBlockEntity(sourcePos).getPersistentData().putString("preserve", "packed-machine-data");
                var tool = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId)));
                helper.assertTrue(!tool.isEmpty(), "Actual packing item must exist: " + itemId);
                packer.setShiftKeyDown(false);
                packer.setItemInHand(InteractionHand.MAIN_HAND, tool);
                var packed = tool.getItem().onItemUseFirst(tool, new UseOnContext(packer, InteractionHand.MAIN_HAND, hit(sourcePos)));
                helper.assertTrue(packed.consumesAction() && source.getBlockState(sourcePos).is(block("mekanism:cardboard_box")),
                    "Actual item must pack the AE machine: " + itemId);
                var sourceData = ((mekanism.common.tile.TileEntityCardboardBox) source.getBlockEntity(sourcePos)).components().get(mekanism.common.registries.MekanismDataComponents.BLOCK_DATA.get());
                var sealed = source.getBlockState(sourcePos).getBlock().getCloneItemStack(source.getBlockState(sourcePos),
                    hit(sourcePos), source, sourcePos, packer);
                receiver.setShiftKeyDown(false);
                receiver.setItemInHand(InteractionHand.MAIN_HAND, sealed);
                var placed = ((BlockItem) sealed.getItem()).place(new BlockPlaceContext(
                    new UseOnContext(receiver, InteractionHand.MAIN_HAND, hit(pos.below()))));
                helper.assertTrue(placed.consumesAction(), "Sealed boxes can be placed without unpacking");
                var tile = level.getBlockEntity(pos);
                helper.assertTrue(tile != null && sourceData.equals(((mekanism.common.tile.TileEntityCardboardBox) tile).components().get(mekanism.common.registries.MekanismDataComponents.BLOCK_DATA.get())),
                    "Real packing and item placement must preserve the original payload");
                var before = tile.saveWithoutMetadata(level.registryAccess()).copy();
                receiver.setShiftKeyDown(true);
                var result = level.getBlockState(pos).useWithoutItem(level, receiver, hit(pos));
                helper.assertTrue(result == InteractionResult.FAIL && tile == level.getBlockEntity(pos)
                    && before.equals(tile.saveWithoutMetadata(level.registryAccess())), "Both actual packing routes must refuse unboxing without data loss");
                // The same box may still be unpacked in the source dimension.
                packer.setShiftKeyDown(true);
                var outside = source.getBlockState(sourcePos).useWithoutItem(source, packer, hit(sourcePos));
                helper.assertTrue(outside.consumesAction() && source.getBlockState(sourcePos).is(block("ae2:charger"))
                    && source.getBlockEntity(sourcePos).getPersistentData().getString("preserve").equals("packed-machine-data"),
                    "AE unboxing outside transfer must preserve real machine data");
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        } finally {
            packer.discard(); receiver.discard();
            source.setBlockAndUpdate(sourcePos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        }
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
            var boxTile = (mekanism.common.tile.TileEntityCardboardBox) level.getBlockEntity(pos);
            boxTile.setComponents(net.minecraft.core.component.DataComponentMap.builder().addAll(boxTile.components())
                .set(mekanism.common.registries.MekanismDataComponents.BLOCK_DATA.get(),
                    new mekanism.common.attachments.BlockData(Blocks.STONE.defaultBlockState(), new CompoundTag())).build());
            var result = level.getBlockState(pos).useWithoutItem(level, player, hit(pos));
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
