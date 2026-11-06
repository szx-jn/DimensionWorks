package dev.szx.dimensionworks.transfer.handler;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget.ChainConveyorFrogportTarget;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import dev.szx.dimensionworks.transfer.WDTransfer;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WDTransfer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FrogportPlayerGameTests {
    private record Fixture(ServerPlayer player, BlockPos frog, ChainConveyorBlockEntity chain) {
        void near() { player.setPos(frog.getX() + 2.5, frog.getY() + .5, frog.getZ() + .5); }
        void packed(boolean packed) { player.getPersistentData().putBoolean("cmp_packed_player", packed); }
        void close() {
            TransferEvents.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
            ServerChainConveyorHandler.handleStopRidingPacket(player);
            player.discard();
        }
    }

    private static Fixture setup(GameTestHelper helper) {
        BlockPos frog = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos chain = frog.above(2);
        var level = helper.getLevel();
        level.setBlockAndUpdate(frog, AllBlocks.PACKAGE_FROGPORT.getDefaultState());
        level.setBlockAndUpdate(chain, AllBlocks.CHAIN_CONVEYOR.getDefaultState());
        var conveyor = (ChainConveyorBlockEntity) level.getBlockEntity(chain);
        conveyor.setSpeed(32);
        ((FrogportBlockEntity) level.getBlockEntity(frog)).target =
            new ChainConveyorFrogportTarget(chain.subtract(frog), 0, null);
        // A server player with a real packet listener, but no client or automatic world ticks.
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "FrogportTest"));
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player);
        var fixture = new Fixture(player, frog, conveyor);
        fixture.near();
        fixture.packed(true);
        player.tickCount = 5;
        TransferEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        player.tickCount = 6;
        return fixture;
    }

    private static void unchanged(GameTestHelper helper, ServerPlayer player, Vec3 before, String message) {
        helper.assertTrue(player.position().distanceToSqr(before) < 1e-10, message);
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void spitCooldownLastsFiveSecondsAndRidersAreNotSwallowed(GameTestHelper helper) {
        var f = setup(helper);
        try {
            for (int i = 0; i < 9; i++) TransferEvents.tickFrogport(f.player());
            helper.assertTrue(ServerChainConveyorHandler.hangingPlayers.containsKey(f.player().getUUID()),
                "Spitting must hand the player to Create's rider handler");
            ServerChainConveyorHandler.handleStopRidingPacket(f.player());
            f.near();
            f.player().tickCount = 10;
            Vec3 before = f.player().position();
            for (int i = 0; i < 12; i++) TransferEvents.tickFrogport(f.player());
            unchanged(helper, f.player(), before, "A nearby packed player must not be swallowed again immediately");
            helper.runAtTickTime(99, () -> {
                f.chain().setSpeed(32);
                TransferEvents.tickFrogport(f.player());
                TransferEvents.tickFrogport(f.player());
                unchanged(helper, f.player(), before, "Cooldown must still block swallowing at tick 99");
            });
            helper.runAtTickTime(100, () -> {
                try {
                    f.chain().setSpeed(32);
                    ServerChainConveyorHandler.handleTTLPacket(f.player());
                    TransferEvents.tickFrogport(f.player());
                    TransferEvents.tickFrogport(f.player());
                    unchanged(helper, f.player(), before, "A rider must remain excluded after cooldown expires");
                    ServerChainConveyorHandler.handleStopRidingPacket(f.player());
                    TransferEvents.tickFrogport(f.player());
                    TransferEvents.tickFrogport(f.player());
                    helper.assertTrue(f.player().position().distanceToSqr(before) > .01,
                        "Swallowing must resume after 100 ticks when the player has dismounted");
                    helper.succeed();
                } finally { f.close(); }
            });
        } catch (RuntimeException | Error e) { f.close(); throw e; }
    }

    @GameTest(template = "empty")
    public static void escapingClearsPullBeforeRepackagingElsewhere(GameTestHelper helper) {
        var f = setup(helper);
        try {
            TransferEvents.tickFrogport(f.player());
            f.packed(false);
            // Exercise the owning tick event: previously it skipped cleanup for unpacked players.
            TransferEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, f.player()));
            Vec3 afterEscape = f.player().position();
            f.packed(true);
            TransferEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, f.player()));
            unchanged(helper, f.player(), afterEscape, "Even immediate repackaging near the frog must not revive the cancelled task");
            f.player().setPos(f.frog().getX() + 100, f.frog().getY(), f.frog().getZ());
            f.packed(true);
            Vec3 before = f.player().position();
            TransferEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, f.player()));
            unchanged(helper, f.player(), before, "Repackaging elsewhere must not resume the previous frog's pull");
            helper.succeed();
        } finally { f.close(); }
    }

    @GameTest(template = "empty")
    public static void teleportsAndDimensionChangesCannotResumeOldPull(GameTestHelper helper) {
        var far = setup(helper);
        try {
            far.player().setPos(far.frog().getX() + 100, far.frog().getY(), far.frog().getZ());
            Vec3 before = far.player().position();
            TransferEvents.tickFrogport(far.player());
            unchanged(helper, far.player(), before, "A teleport out of range must cancel before any pull");
        } finally { far.close(); }
        var cross = setup(helper);
        try {
            var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether != null, "Nether must exist for the dimension guard test");
            cross.player().setServerLevel(nether); // Deliberately bypass the dimension event to test the stored key.
            Vec3 before = cross.player().position();
            TransferEvents.tickFrogport(cross.player());
            unchanged(helper, cross.player(), before, "Same coordinates in another dimension must not resume an old pull");
            cross.player().setServerLevel(helper.getLevel());
            TransferEvents.tickFrogport(cross.player());
            unchanged(helper, cross.player(), before, "Returning to the original dimension must not revive the old task");
            helper.succeed();
        } finally { cross.close(); }
    }

    @GameTest(template = "empty")
    public static void changedOrBrokenChainCancelsBeforeMovingPlayer(GameTestHelper helper) {
        for (int scenario = 0; scenario < 3; scenario++) {
            var f = setup(helper);
            try {
                if (scenario == 0) f.chain().setSpeed(0);
                if (scenario == 1) ((FrogportBlockEntity) helper.getLevel().getBlockEntity(f.frog())).target = null;
                if (scenario == 2) ((ChainConveyorFrogportTarget)
                    ((FrogportBlockEntity) helper.getLevel().getBlockEntity(f.frog())).target).connection = new BlockPos(5, 0, 0);
                Vec3 before = f.player().position();
                TransferEvents.tickFrogport(f.player());
                unchanged(helper, f.player(), before, "Unavailable or changed chain target must cancel before moving the player");
            } finally { f.close(); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deathRespawnLogoutAndDimensionEventsClearPendingPulls(GameTestHelper helper) {
        for (int scenario = 0; scenario < 4; scenario++) {
            var f = setup(helper);
            try {
                if (scenario == 0) {
                    f.player().setHealth(0);
                    TransferEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, f.player()));
                    f.player().setHealth(20);
                }
                if (scenario == 1) TransferEvents.onRespawn(new PlayerEvent.PlayerRespawnEvent(f.player(), false));
                if (scenario == 2) TransferEvents.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(f.player()));
                if (scenario == 3) TransferEvents.onChangedDimension(
                    new PlayerEvent.PlayerChangedDimensionEvent(f.player(), Level.OVERWORLD, Level.NETHER));
                Vec3 before = f.player().position();
                TransferEvents.tickFrogport(f.player());
                unchanged(helper, f.player(), before, "Lifecycle cleanup must discard the previous swallow task");
            } finally { f.close(); }
        }
        helper.succeed();
    }
}
