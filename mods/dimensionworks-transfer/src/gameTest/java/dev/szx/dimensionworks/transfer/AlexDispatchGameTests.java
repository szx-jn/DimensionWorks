package dev.szx.dimensionworks.transfer;

import com.mojang.authlib.GameProfile;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState;
import dev.szx.dimensionworks.transfer.access.TransferAccess;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import dev.szx.dimensionworks.transfer.dispatch.AlexCavesDestination;
import dev.szx.dimensionworks.transfer.dispatch.DispatchController;
import dev.szx.dimensionworks.transfer.dispatch.DispatchItems;
import dev.szx.dimensionworks.transfer.dispatch.DispatchLevel;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPoint;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPointSavedData;
import dev.szx.dimensionworks.transfer.territory.SafeLanding;
import dev.szx.dimensionworks.transfer.territory.TerritorySavedData;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Opt-in checks with the actual Twilight, Alex cave, Cave Factory and KubeJS release dependencies. */
@GameTestHolder("dimensionworks_alex_dispatch")
@PrefixGameTestTemplate(false)
public final class AlexDispatchGameTests {
    private static final String TEMPLATE = "empty";

    private static ItemStack item(String id) {
        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
        if (stack.isEmpty()) throw new AssertionError("Missing item " + id);
        return stack;
    }

    private static ServerPlayer player(GameTestHelper helper) throws java.io.IOException {
        var server = helper.getLevel().getServer();
        var level = DispatchLevel.getOrLoad(server, WDKeys.TRANSFER_LEVEL);
        var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "AlexDispatchTest"));
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player);
        player.addTag(TransferAccess.UNLOCK_TAG);
        var territory = TerritorySavedData.get(server.overworld()).getOrAssign(player.getUUID());
        DispatchController.ensure(level, territory);
        BlockPos terminal = DispatchController.terminal(territory);
        player.setPos(terminal.getX() + .5, terminal.getY(), terminal.getZ() + .5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DispatchItems.TWILIGHT_UPGRADE.get(), 2));
        return player;
    }

    private static BlockPos terminal(ServerPlayer player) {
        return DispatchController.terminal(TerritorySavedData.get(player.server.overworld()).get(player.getUUID()));
    }

    private static InteractionResult install(ServerPlayer player, BlockPos pos) {
        return DispatchItems.TWILIGHT_UPGRADE.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }

    private static void close(ServerPlayer player) {
        DispatchController.forget(player);
        ReturnPointSavedData.get(player.server.overworld()).remove(player.getUUID());
        PlayerParcel.unpack(player);
        player.discard();
    }

    @GameTest(template = TEMPLATE)
    public static void actualRecipeAcceptsRepeatedTrophiesAndRejectsWrongMaterials(GameTestHelper helper) {
        var recipe = (CraftingRecipe) helper.getLevel().getRecipeManager()
            .byKey(new ResourceLocation("dimensionworks_transfer:twilight_dispatch_upgrade")).orElseThrow();
        var menu = new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        grid.setItem(0, item("twilightforest:twilight_oak_log"));
        for (int slot = 1; slot < 4; slot++) grid.setItem(slot, item("twilightforest:naga_trophy"));
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Three identical Boss trophies must be accepted");
        helper.assertTrue(recipe.assemble(grid, helper.getLevel().registryAccess()).is(DispatchItems.TWILIGHT_UPGRADE.get()),
            "Actual recipe must produce the registered terminal upgrade");
        grid.setItem(0, item("twilightforest:stripped_canopy_wood"));
        grid.setItem(2, item("twilightforest:lich_trophy"));
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Mixed trophies and stripped Twilight wood must work");
        grid.setItem(3, ItemStack.EMPTY);
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "Two trophies are insufficient");
        grid.setItem(3, item("twilightforest:naga_trophy"));
        grid.setItem(0, new ItemStack(Items.OAK_LOG));
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "Vanilla wood must not substitute for Twilight wood");
        grid.setItem(0, item("twilightforest:twilight_oak_log"));
        grid.setItem(3, item("twilightforest:quest_ram_trophy"));
        helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "Quest Ram is not a killed Boss trophy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void installationIsPersonalIdempotentAndPersistsWithThePlot(GameTestHelper helper) throws Exception {
        var owner = player(helper);
        var visitor = player(helper);
        try {
            visitor.setPos(owner.getX(), owner.getY(), owner.getZ());
            helper.assertTrue(install(visitor, terminal(owner)) == InteractionResult.FAIL, "Visitor cannot upgrade another plot");
            helper.assertTrue(visitor.getMainHandItem().getCount() == 2 && !DispatchController.alexCavesUnlocked(visitor),
                "Rejected installation must preserve card and visitor access");
            helper.assertTrue(install(owner, terminal(owner)) == InteractionResult.CONSUME, "Owner must be able to install");
            helper.assertTrue(owner.getMainHandItem().getCount() == 1 && DispatchController.alexCavesUnlocked(owner),
                "Installation consumes exactly one card and unlocks routes");
            helper.assertTrue(install(owner, terminal(owner)) == InteractionResult.FAIL && owner.getMainHandItem().getCount() == 1,
                "Repeated installation must not consume another card");
            CompoundTag saved = TerritorySavedData.get(owner.server.overworld()).save(new CompoundTag());
            var reloaded = TerritorySavedData.load(saved);
            helper.assertTrue(reloaded.alexCavesUnlocked(owner.getUUID()) && !reloaded.alexCavesUnlocked(visitor.getUUID()),
                "Save/load must retain the owner upgrade without unlocking other plots");
        } finally { close(owner); close(visitor); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lockedRequestsPreserveParcelAndReturnPointAndAetherStaysLocked(GameTestHelper helper) throws Exception {
        var player = player(helper);
        try {
            var source = new ReturnPoint(Level.OVERWORLD.location(), 0, 64, 0, 0, 0);
            var returns = ReturnPointSavedData.get(player.server.overworld());
            returns.put(player.getUUID(), source);
            PackedPlayerState.setPacked(player, true, false);
            DispatchController.touch(player, terminal(player));
            helper.assertTrue(!DispatchController.send(player, terminal(player), AlexCavesDestination.MAGNETIC.route()),
                "A forged cave request cannot bypass the missing upgrade");
            helper.assertTrue(player.level().dimension().equals(WDKeys.TRANSFER_LEVEL) && PlayerParcel.isPacked(player)
                && returns.get(player.getUUID()).orElseThrow().equals(source), "Rejected dispatch must preserve traveller and source");
            var blocked = new EntityTravelToDimensionEvent(player, AlexCavesDestination.MAGNETIC.key());
            MinecraftForge.EVENT_BUS.post(blocked);
            helper.assertTrue(blocked.isCanceled(), "Cave Factory must still reject unauthorized cave travel");
            player.getPersistentData().putBoolean(AlexCavesDestination.AUTHORIZATION, true);
            var aether = new EntityTravelToDimensionEvent(player, net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation("aether", "the_aether")));
            MinecraftForge.EVENT_BUS.post(aether);
            helper.assertTrue(aether.isCanceled(), "Cave authorization must never unlock Aether");
            player.getPersistentData().remove(AlexCavesDestination.AUTHORIZATION);
            TerritorySavedData.get(player.server.overworld()).unlockAlexCaves(player.getUUID());
            helper.assertTrue(!DispatchController.send(player, terminal(player), AlexCavesDestination.MAGNETIC.route()),
                "Consumed terminal sessions cannot be replayed after unlocking");
        } finally { player.getPersistentData().remove(AlexCavesDestination.AUTHORIZATION); close(player); }
        helper.succeed();
    }

    private static void arrive(GameTestHelper helper, AlexCavesDestination cave) throws Exception {
        var player = player(helper);
        try {
            install(player, terminal(player));
            PackedPlayerState.setPacked(player, true, false);
            ReturnPointSavedData.get(player.server.overworld()).put(player.getUUID(),
                new ReturnPoint(Level.OVERWORLD.location(), 0, 64, 0, 0, 0));
            DispatchController.touch(player, terminal(player));
            helper.assertTrue(DispatchController.send(player, terminal(player), cave.route()), "Actual dispatch must reach " + cave.key().location());
            helper.assertTrue(player.level().dimension().equals(cave.key()), "Traveller must arrive in the selected real dimension");
            helper.assertTrue(SafeLanding.safe(player.serverLevel(), player.blockPosition()), "Arrival must have safe support and full player clearance");
            helper.assertTrue(!PlayerParcel.isPacked(player) && !player.getPersistentData().getBoolean(AlexCavesDestination.AUTHORIZATION)
                && ReturnPointSavedData.get(player.server.overworld()).get(player.getUUID()).isEmpty(),
                "Successful dispatch must unpack, clear temporary authorization and consume the source point");
            WDTransfer.LOGGER.info("ALEX_DISPATCH_ARRIVAL: {} at {}", cave.key().location(), player.blockPosition());
        } finally { close(player); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600) public static void magneticArrival(GameTestHelper h) throws Exception { arrive(h, AlexCavesDestination.MAGNETIC); }
    @GameTest(template = TEMPLATE, timeoutTicks = 600) public static void primordialArrival(GameTestHelper h) throws Exception { arrive(h, AlexCavesDestination.PRIMORDIAL); }
    @GameTest(template = TEMPLATE, timeoutTicks = 600) public static void toxicArrival(GameTestHelper h) throws Exception { arrive(h, AlexCavesDestination.TOXIC); }
    @GameTest(template = TEMPLATE, timeoutTicks = 600) public static void abyssalArrival(GameTestHelper h) throws Exception { arrive(h, AlexCavesDestination.ABYSSAL); }
    @GameTest(template = TEMPLATE, timeoutTicks = 600) public static void forlornArrival(GameTestHelper h) throws Exception { arrive(h, AlexCavesDestination.FORLORN); }
    @GameTest(template = TEMPLATE, timeoutTicks = 600) public static void candyArrival(GameTestHelper h) throws Exception { arrive(h, AlexCavesDestination.CANDY); }
}
