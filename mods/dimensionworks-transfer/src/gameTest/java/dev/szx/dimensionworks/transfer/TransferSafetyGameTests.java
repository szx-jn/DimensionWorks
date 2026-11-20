package dev.szx.dimensionworks.transfer;

import dev.szx.dimensionworks.transfer.dispatch.DispatchBlocks;
import dev.szx.dimensionworks.transfer.dispatch.TwilightDestination;
import dev.szx.dimensionworks.transfer.territory.SafeLanding;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WDTransfer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TransferSafetyGameTests {
    @GameTest(template = "empty")
    public static void requiresTwoVerticalBlocksAndSafeSupport(GameTestHelper helper) {
        BlockPos feet = new BlockPos(3, 2, 3);
        BlockPos absolute = helper.absolutePos(feet);
        var level = helper.getLevel();
        helper.setBlock(feet.below(), Blocks.AIR);
        helper.assertTrue(!SafeLanding.safe(level, absolute), "Unsupported air is not a landing");
        helper.setBlock(feet.below(), Blocks.STONE);
        helper.assertTrue(SafeLanding.safe(level, absolute), "Stone and two air blocks must be safe");
        helper.setBlock(feet.above(), Blocks.STONE);
        helper.assertTrue(!SafeLanding.safe(level, absolute), "A packed player's one-block cavity must be rejected");
        helper.setBlock(feet.above(), Blocks.AIR);
        helper.setBlock(feet, Blocks.OAK_SLAB);
        helper.assertTrue(!SafeLanding.safe(level, absolute), "Partial collision at the feet must be rejected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectsHazardsFluidsAndPortals(GameTestHelper helper) {
        BlockPos feet = new BlockPos(3, 2, 3);
        BlockPos absolute = helper.absolutePos(feet);
        var level = helper.getLevel();
        helper.setBlock(feet.below(), Blocks.MAGMA_BLOCK);
        helper.assertTrue(!SafeLanding.safe(level, absolute), "Magma floor is dangerous");
        helper.setBlock(feet.below(), Blocks.STONE);
        helper.setBlock(feet.above(2), Blocks.LAVA);
        helper.assertTrue(!SafeLanding.safe(level, absolute), "Lava above the head is dangerous");
        helper.setBlock(feet.above(2), Blocks.AIR);
        helper.setBlock(feet, Blocks.WATER);
        helper.assertTrue(!SafeLanding.safe(level, absolute), "Water is not a dry landing");
        helper.setBlock(feet, Blocks.NETHER_PORTAL);
        helper.assertTrue(!SafeLanding.safe(level, absolute), "Portals must never be landing blocks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalDoesNotSuffocateAndTwilightApiIsPresent(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, DispatchBlocks.TERMINAL.get());
        var absolute = helper.absolutePos(pos);
        helper.assertTrue(!helper.getLevel().getBlockState(absolute).isSuffocating(helper.getLevel(), absolute),
            "Terminal must not suffocate packed travellers");
        try {
            var type = Class.forName("twilightforest.world.TFTeleporter");
            type.getDeclaredMethod("moveToSafeCoords", net.minecraft.server.level.ServerLevel.class,
                net.minecraft.world.entity.Entity.class, BlockPos.class);
            type.getMethod("isSafeAround", net.minecraft.world.level.Level.class, BlockPos.class,
                net.minecraft.world.entity.Entity.class, boolean.class);
            helper.assertTrue(TwilightDestination.getOrLoad(helper.getLevel().getServer()) != null,
                "Twilight must have a usable level even before any portal is visited");
        } catch (Exception e) { throw new AssertionError("Twilight dispatch API must load", e); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void loadsMissingTwilightLevelOnDemand(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = (dev.szx.dimensionworks.transfer.mixin.MinecraftServerAccess) server;
        var original = access.dimensionworks$levels().remove(TwilightDestination.KEY);
        net.minecraft.server.level.ServerLevel loaded = null;
        try {
            helper.assertTrue(server.getLevel(TwilightDestination.KEY) == null, "Test must start without a loaded Twilight level");
            loaded = TwilightDestination.getOrLoad(server);
            helper.assertTrue(loaded != null && loaded != original && server.getLevel(TwilightDestination.KEY) == loaded,
                "Dispatch must instantiate and register TF's missing level");
            helper.assertTrue(loaded.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator generator
                && generator.generatorSettings().unwrapKey().orElseThrow().location().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("twilightforest", "twilight_noise_gen")),
                "Lazy level must use TF's own generator");
        } catch (Exception e) { throw new AssertionError("Missing Twilight level must load", e); }
        finally {
            if (original != null) access.dimensionworks$levels().put(TwilightDestination.KEY, original);
            else access.dimensionworks$levels().remove(TwilightDestination.KEY);
            server.markWorldsDirty();
            if (loaded != null) try { loaded.close(); } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void dryOceanArrivalAddsOnlyAirFloorAndRejectsOccupiedSpace(GameTestHelper helper) {
        var level = helper.getLevel(); var surface = helper.absolutePos(new BlockPos(3, 2, 3));
        for (var floor : BlockPos.betweenClosed(surface.offset(-1, 0, -1), surface.offset(1, 0, 1))) {
            level.setBlockAndUpdate(floor.below(), Blocks.WATER.defaultBlockState());
            for (int dy = 0; dy <= 3; dy++) level.setBlockAndUpdate(floor.above(dy), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(surface.east().above(), Blocks.CHEST.defaultBlockState());
        var chest = level.getBlockEntity(surface.east().above());
        ((net.minecraft.world.Container) chest).setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
        helper.assertTrue(SafeLanding.prepareDryOceanPlatform(level, surface).isEmpty(), "Occupied arrival volume must be refused");
        helper.assertTrue(level.getBlockEntity(surface.east().above()) == chest && ((net.minecraft.world.Container) chest).getItem(0).getCount() == 7,
            "Refusal must retain the existing chest and inventory");
        helper.assertTrue(level.getBlockState(surface).isAir(), "Refusal must not build a partial platform");
        level.setBlockAndUpdate(surface.east().above(), Blocks.AIR.defaultBlockState());
        var landing = SafeLanding.prepareDryOceanPlatform(level, surface);
        helper.assertTrue(landing.orElseThrow().equals(surface.above()) && SafeLanding.safe(level, landing.get()), "Ocean arrival must be dry and supported");
        for (var floor : BlockPos.betweenClosed(surface.offset(-1, 0, -1), surface.offset(1, 0, 1))) {
            helper.assertTrue(level.getBlockState(floor).is(Blocks.STONE) && level.getFluidState(floor.below()).is(net.minecraft.tags.FluidTags.WATER),
                "Only the nine air floor cells may change; ocean must remain intact");
        }
        helper.assertTrue(SafeLanding.prepareDryOceanPlatform(level, surface).isEmpty(), "A platform must never overwrite an existing floor");
        helper.succeed();
    }
}
