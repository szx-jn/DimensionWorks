package dev.szx.dimensionworks.transfer;

import com.mojang.authlib.GameProfile;
import dev.szx.dimensionworks.transfer.dispatch.DispatchBlocks;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Exercises actual CMP and Mekanism JARs in an isolated world. */
@GameTestHolder("dimensionworks_cardboard")
@PrefixGameTestTemplate(false)
public final class CardboardPackingGameTests {
    private static final List<String> PACKERS = List.of("mekanism:cardboard_box", "create_mobile_packages:mobile_packager");
    private static final int FLAGS = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;

    private static FakePlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "[CardboardTest]"));
    }

    private static Block block(String id) {
        var result = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        if (result == null || result == Blocks.AIR) throw new AssertionError("Missing runtime block: " + id);
        return result;
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static ItemStack tool(String id) {
        var result = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
        if (result.isEmpty()) throw new AssertionError("Missing runtime packing item: " + id);
        return result;
    }

    private static InteractionResult pack(FakePlayer player, BlockPos pos, ItemStack stack) {
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.getItem().onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
    }

    private static void unpack(FakePlayer player, BlockPos pos) {
        player.setShiftKeyDown(true);
        player.level().getBlockState(pos).use(player.level(), player, InteractionHand.MAIN_HAND, hit(pos));
    }

    private static long emptyBoxes(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
            .filter(e -> e.getItem().is(ForgeRegistries.ITEMS.getValue(new ResourceLocation("mekanism:cardboard_box"))))
            .mapToLong(e -> e.getItem().getCount()).sum();
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).forEach(ItemEntity::discard);
    }

    @GameTest(template = "empty")
    public static void transferRestrictionsRetainSealedAePayload(GameTestHelper h) {
        TransferAeBlockGameTests.realPackingAndBoxItemPlacementCannotBypassUnboxingGuard(h);
    }

    @GameTest(template = "empty")
    public static void registryPolicyExcludesExactlyTwoBlocks(GameTestHelper h) throws ReflectiveOperationException {
        var policy = Class.forName("de.theidler.create_mobile_packages.items.mobile_packager.CardboardBoxPackagingPolicy")
            .getMethod("canPack", ResourceLocation.class);
        int excluded = 0;
        for (var entry : ForgeRegistries.BLOCKS.getEntries()) {
            if (entry.getValue().defaultBlockState().isAir()) continue;
            boolean allowed = (boolean) policy.invoke(null, entry.getKey().location());
            boolean forbidden = entry.getValue() == Blocks.BARRIER || entry.getValue() == DispatchBlocks.TERMINAL.get();
            h.assertTrue(allowed != forbidden, "Unexpected exclusion: " + entry.getKey());
            if (!allowed) excluded++;
        }
        h.assertTrue(excluded == 2, "The actual registry must contain exactly two protected blocks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bothPackersBypassAllMekanismTypeRestrictions(GameTestHelper h) throws ReflectiveOperationException {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        var p = player(h);
        var general = Class.forName("mekanism.common.config.MekanismConfig").getField("general").get(null);
        var blacklist = general.getClass().getField("cardboardModBlacklist").get(general);
        var set = blacklist.getClass().getMethod("set", Object.class);
        Object original = blacklist.getClass().getMethod("get").invoke(blacklist);
        try {
            set.invoke(blacklist, List.of("minecraft", "mekanism", "ae2", "create"));
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), FLAGS);
            for (String packer : PACKERS) {
                for (String id : List.of("minecraft:stone", "minecraft:bedrock", "minecraft:end_portal_frame",
                        "minecraft:end_portal", "minecraft:nether_portal", "minecraft:command_block",
                        "minecraft:structure_block", "minecraft:jigsaw", "minecraft:spawner", "minecraft:piston_head",
                        "minecraft:water", "minecraft:chest", "ae2:controller", "ae2:cable_bus",
                        "mekanism:ultimate_bin", "create:track")) {
                    var target = block(id);
                    level.setBlock(pos, target.defaultBlockState(), FLAGS);
                    var tile = level.getBlockEntity(pos);
                    if (tile != null) tile.getPersistentData().putString("dw_cardboard_probe", id);
                    if (id.equals("minecraft:stone")) h.assertTrue(level.getBlockState(pos).is(TagKey.create(Registries.BLOCK,
                        new ResourceLocation("mekanism:cardboard_blacklist"))), "Exercise an actual block blacklist");
                    if (id.equals("minecraft:chest")) h.assertTrue(ForgeRegistries.BLOCK_ENTITY_TYPES.tags()
                        .getTag(TagKey.create(Registries.BLOCK_ENTITY_TYPE, new ResourceLocation("mekanism:cardboard_blacklist")))
                        .contains(tile.getType()), "Exercise an actual block-entity blacklist");
                    var stack = tool(packer);
                    h.assertTrue(pack(p, pos, stack).consumesAction() && level.getBlockState(pos).is(block("mekanism:cardboard_box")),
                        packer + " must package " + id);
                    h.assertTrue(stack.getCount() == (packer.equals(PACKERS.get(0)) ? 0 : 1), "Only an ordinary empty box is consumed");
                    unpack(p, pos);
                    h.assertTrue(level.getBlockState(pos).is(target), "Unpacking must restore " + id);
                    if (tile != null) h.assertTrue(level.getBlockEntity(pos).getPersistentData().getString("dw_cardboard_probe").equals(id),
                        "Block-entity data must survive for " + id);
                    h.assertTrue(emptyBoxes(level, pos) == (packer.equals(PACKERS.get(0)) ? 1 : 0), "Empty box return must follow origin");
                    clear(level, pos);
                }
            }
        } finally {
            set.invoke(blacklist, original);
            p.discard();
            clear(level, pos);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void doorsBedsAndTallPlantsSurviveSeparatePacking(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        var p = player(h);
        try {
            for (String packer : PACKERS) for (Block target : List.of(Blocks.OAK_DOOR, Blocks.RED_BED, Blocks.SUNFLOWER)) {
                boolean bed = target == Blocks.RED_BED;
                var other = bed ? pos.north() : pos.above();
                var first = bed ? target.defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT)
                    : target.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
                var second = bed ? first.setValue(BedBlock.PART, BedPart.HEAD)
                    : first.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), FLAGS);
                if (bed) level.setBlock(other.below(), Blocks.DIRT.defaultBlockState(), FLAGS);
                level.setBlock(pos, first, FLAGS);
                level.setBlock(other, second, FLAGS);
                h.assertTrue(pack(p, pos, tool(packer)).consumesAction(), "First part must pack");
                h.assertTrue(level.getBlockState(other).equals(second), "Packing one part must retain the other");
                h.assertTrue(pack(p, other, tool(packer)).consumesAction(), "Other part must pack");
                unpack(p, pos);
                h.assertTrue(level.getBlockState(pos).equals(first), "Restore the first part while the other is still boxed");
                unpack(p, other);
                h.assertTrue(level.getBlockState(pos).equals(first) && level.getBlockState(other).equals(second),
                    "Both parts must retain their orientation and part state");
                clear(level, other);
                clear(level, pos);
            }
        } finally {
            p.discard();
            clear(level, pos.above());
            clear(level, pos.north());
            clear(level, pos);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void transportedChestRetainsInventoryAndOrigin(GameTestHelper h) {
        var level = h.getLevel();
        var source = h.absolutePos(new BlockPos(2, 2, 3));
        var destination = source.east(4);
        var p = player(h);
        try {
            level.setBlock(destination.below(), Blocks.STONE.defaultBlockState(), FLAGS);
            for (String packer : PACKERS) {
                level.setBlock(source, Blocks.CHEST.defaultBlockState(), FLAGS);
                ((Container) level.getBlockEntity(source)).setItem(0, new ItemStack(Items.DIAMOND, 7));
                h.assertTrue(pack(p, source, tool(packer)).consumesAction(), "Chest must pack");
                var box = level.getBlockEntity(source);
                var saved = box.saveWithFullMetadata();
                box.load(saved);
                h.assertTrue(saved.getCompound("data").getBoolean("create_mobile_packages:from_mobile_packager")
                    == packer.equals(PACKERS.get(1)), "Origin must persist in BlockData");
                var drops = Block.getDrops(level.getBlockState(source), level, source, box, p, new ItemStack(Items.DIAMOND_PICKAXE));
                h.assertTrue(drops.size() == 1, "The packed box must yield exactly one transported item");
                var transported = drops.get(0);
                clear(level, source);
                p.setShiftKeyDown(false);
                p.setItemInHand(InteractionHand.MAIN_HAND, transported);
                h.assertTrue(((BlockItem) transported.getItem()).place(new BlockPlaceContext(
                    new UseOnContext(p, InteractionHand.MAIN_HAND, hit(destination.below())))).consumesAction(), "Transported box must place");
                h.assertTrue(saved.getCompound("data").equals(level.getBlockEntity(destination).saveWithoutMetadata().getCompound("data")),
                    "The complete payload must survive harvesting, moving and placement");
                unpack(p, destination);
                h.assertTrue(level.getBlockState(destination).is(Blocks.CHEST), "Transported chest must restore");
                h.assertTrue(((Container) level.getBlockEntity(destination)).getItem(0).getCount() == 7, "All seven diamonds must remain");
                h.assertTrue(emptyBoxes(level, destination) == (packer.equals(PACKERS.get(0)) ? 1 : 0), "Transport must preserve empty box returns");
                clear(level, destination);
            }
        } finally { p.discard(); clear(level, source); clear(level, destination); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void protectedBlocksAndBreakPermissionsRemainEffective(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        var p = player(h);
        Consumer<BlockEvent.BreakEvent> denial = event -> {
            if (event.getLevel() == level && event.getPos().equals(pos)) event.setCanceled(true);
        };
        try {
            for (String packer : PACKERS) for (Block target : List.of(Blocks.BARRIER, DispatchBlocks.TERMINAL.get())) {
                level.setBlock(pos, target.defaultBlockState(), FLAGS);
                var stack = tool(packer);
                h.assertTrue(!pack(p, pos, stack).consumesAction(), "Protected block must reject " + packer);
                h.assertTrue(level.getBlockState(pos).is(target) && stack.getCount() == 1, "Retain protected block and packing item");
            }
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, denial);
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), FLAGS);
            ((Container) level.getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 7));
            var tile = level.getBlockEntity(pos);
            var before = tile.saveWithFullMetadata();
            for (String packer : PACKERS) {
                var stack = tool(packer);
                h.assertTrue(pack(p, pos, stack) == InteractionResult.FAIL, "Break permission must reject " + packer);
                h.assertTrue(level.getBlockEntity(pos) == tile && before.equals(tile.saveWithFullMetadata()) && stack.getCount() == 1,
                    "Permission denial must retain complete inventory, block and packing item");
            }
        } finally { MinecraftForge.EVENT_BUS.unregister(denial); p.discard(); clear(level, pos); }
        h.succeed();
    }
}
