package dev.szx.dimensionworks.mekstress;

import com.mojang.authlib.GameProfile;
import dev.szx.dimensionworks.mekstress.integration.mek.MekProcessingContext;
import dev.szx.dimensionworks.rpmlimit.GearHeartEffects;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import java.util.UUID;
import mekanism.api.recipes.basic.BasicCrushingRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(DimensionWorksMekStress.MOD_ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = DimensionWorksMekStress.MOD_ID)
public final class MekCachedRecipeGameTests {
    @SubscribeEvent
    public static void registerGearHeartFixtures(RegisterEvent event) {
        event.register(Registries.ITEM, registry -> {
            registry.register(ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart"),
                new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
            registry.register(ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_enchanted"),
                new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
        });
    }

    private static final class RecordingConnection extends Connection {
        RecordingConnection() {
            super(PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(this);
            net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(this);
        }

        @Override
        public void send(Packet<?> packet, PacketSendListener listener, boolean flush) {
        }
    }

    private static final class CountingRecipe extends CachedRecipe<BasicCrushingRecipe> {
        int completed;

        CountingRecipe() {
            super(new BasicCrushingRecipe(IngredientCreatorAccess.item().from(Items.COBBLESTONE),
                new ItemStack(Items.GRAVEL)), () -> false);
            setBaselineMaxOperations(() -> 1);
        }

        @Override
        public boolean isInputValid() {
            return true;
        }

        @Override
        protected void finishProcessing(int operations) {
            completed += operations;
        }
    }

    private static ServerPlayer equippedPlayer(GameTestHelper helper) {
        var level = helper.getLevel();
        var connection = new RecordingConnection();
        var player = new ServerPlayer(level.getServer(), level,
            new GameProfile(UUID.randomUUID(), "MekStressTest"), net.minecraft.server.level.ClientInformation.createDefault());
        var cookie = new CommonListenerCookie(player.getGameProfile(), 0,
            net.minecraft.server.level.ClientInformation.createDefault(), false,
            net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie);
        RpmLimitManager.init(player);
        helper.assertTrue(GearHeartEffects.ensureGearHeartSlots(player),
            "Test player must receive the existing Gear Heart slot/item");
        GearHeartEffects.tickPlayer(player);
        return player;
    }

    private static void process(CountingRecipe recipe, TileEntityMekanism machine) {
        var previous = MekProcessingContext.push(machine);
        try {
            recipe.process();
        } finally {
            MekProcessingContext.pop(previous);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void actualMekanismCachedRecipeObeysOreRateAndRepair(GameTestHelper helper) {
        var player = equippedPlayer(helper);
        var position = new BlockPos(2, 1, 2);
        helper.setBlock(position,
            BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:crusher")).defaultBlockState());
        var machine = (TileEntityMekanism) helper.getBlockEntity(position);
        var recipe = new CountingRecipe();
        process(recipe, machine);
        helper.assertTrue(recipe.completed == 0,
            "Ore processing quota must not accumulate enough for one operation on the first tick");
        helper.runAfterDelay(3, () -> {
            try {
                process(recipe, machine);
                helper.assertTrue(recipe.completed == 1,
                    "The 8 operations/second quota must allow one CachedRecipe operation after three ticks");
                process(recipe, machine);
                helper.assertTrue(recipe.completed == 1,
                    "A second CachedRecipe operation in the same tick must be denied");
                for (int stage = 0; stage < 3; stage++) {
                    helper.assertTrue(GearHeartEffects.repairEquippedCurse(player, 1 << stage)
                            == GearHeartEffects.RepairResult.REPAIRED,
                        "Sequential Gear Heart repair must succeed");
                }
                process(recipe, machine);
                helper.assertTrue(recipe.completed == 2,
                    "Repairing the third stage must release the ore-processing quota");
                helper.assertTrue(MekProcessingContext.current() == null,
                    "Mekanism processing context must be cleared after each operation");
                helper.succeed();
            } finally {
                GearHeartEffects.clearPlayer(player);
                player.serverLevel().removePlayerImmediately(player,
                    net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                ((io.netty.channel.embedded.EmbeddedChannel) player.connection.getConnection().channel())
                    .finishAndReleaseAll();
            }
        });
    }
}
