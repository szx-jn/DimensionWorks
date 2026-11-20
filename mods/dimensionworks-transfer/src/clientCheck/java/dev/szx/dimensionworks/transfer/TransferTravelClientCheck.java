package dev.szx.dimensionworks.transfer;

import dev.szx.dimensionworks.transfer.access.TransferAccess;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPoint;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPointSavedData;
import dev.szx.dimensionworks.transfer.territory.Territory;
import dev.szx.dimensionworks.transfer.territory.TerritorySavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class TransferTravelClientCheck {
    private static int stage, ticks;
    private static final long DEADLINE = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(2);
    private static volatile ItemStack original;
    private static volatile ReturnPoint source;
    private static volatile Territory plot;
    private static volatile boolean ready;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("dimensionworks.transferTravelClientCheck") || stage == 8) return;
        var client = Minecraft.getInstance();
        require(System.nanoTime() < DEADLINE, "Travel client timed out at stage " + stage);
        if (client.player == null || client.level == null || client.getOverlay() != null || client.getSingleplayerServer() == null) return;
        ticks++;
        if (stage == 0) {
            stage = 1; ticks = 0;
            var id = client.player.getUUID();
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayer(id);
                var stack = new ItemStack(Items.DIAMOND, 7);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("unchanged transfer inventory"));
                net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong("snapshot", 987654321234L));
                original = stack.copy(); player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                player.inventoryMenu.broadcastChanges();
            });
        } else if (stage == 1 && original != null && ItemStack.matches(client.player.getMainHandItem(), original)) {
            TransferNetwork.sendToggleRequest(); stage = 2; ticks = 0;
        } else if (stage == 2 && ticks >= 20) {
            require(client.level.dimension().equals(Level.OVERWORLD), "Locked native key payload bypassed access");
            stage = 3; ready = false;
            var id = client.player.getUUID();
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayer(id);
                player.addTag(TransferAccess.UNLOCK_TAG);
                source = new ReturnPoint(player.level().dimension().location(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                ready = true;
            });
        } else if (stage == 3 && ready) {
            TransferNetwork.sendToggleRequest(); stage = 4; ticks = 0; ready = false;
        } else if (stage == 4 && client.level.dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            var id = client.player.getUUID();
            if (plot == null) client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                plot = TerritorySavedData.get(server.overworld()).get(id);
                if (plot != null && TerritorySavedData.get(server.overworld()).isComplete(id)) ready = true;
            });
            else if (!ready) client.getSingleplayerServer().execute(() -> ready = TerritorySavedData.get(client.getSingleplayerServer().overworld()).isComplete(id));
            if (plot == null || !ready || !allPlotChunks(client)) return;
            require(ItemStack.matches(client.player.getMainHandItem(), original), "Entry changed item count or components");
            WDTransfer.LOGGER.info("TRANSFER CLIENT COVERAGE CHECK PASSED: native key access, unchanged item components and all 25 plot/border chunks");
            stage = 5; ticks = 0;
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayer(id);
                player.teleportTo(plot.chunkX() * 16 + 1.5, player.getY(), plot.chunkZ() * 16 + 1.5);
            });
        } else if (stage == 5 && ticks >= 20) {
            require(allPlotChunks(client), "Moving to the plot edge unloaded its fixed coverage");
            require(!client.level.getChunkSource().hasChunk(plot.chunkX() + 4, plot.chunkZ() + 1), "A chunk outside the render border reached the client");
            TransferNetwork.sendToggleRequest(); stage = 6; ticks = 0;
        } else if (stage == 6 && client.level.dimension().equals(Level.OVERWORLD) && ticks > 10) {
            require(ItemStack.matches(client.player.getMainHandItem(), original), "Return changed original inventory");
            require(client.player.position().distanceTo(new net.minecraft.world.phys.Vec3(source.x(), source.y(), source.z())) < .1,
                "Return did not restore the saved source location");
            stage = 7; ready = false; var id = client.player.getUUID();
            client.getSingleplayerServer().execute(() -> {
                require(ReturnPointSavedData.get(client.getSingleplayerServer().overworld()).get(id).isEmpty(), "Successful return retained a stale return point");
                ready = true;
            });
        } else if (stage == 7 && ready) {
            WDTransfer.LOGGER.info("TRANSFER TRAVEL CLIENT CHECK PASSED: actual locked/unlocked payloads, fixed plot coverage at edges, source return and inventory preservation");
            stage = 8; client.stop();
        }
    }
    private static boolean allPlotChunks(Minecraft client) {
        for (int x = plot.chunkX() - 1; x <= plot.chunkX() + 3; x++)
            for (int z = plot.chunkZ() - 1; z <= plot.chunkZ() + 3; z++)
                if (!client.level.getChunkSource().hasChunk(x, z)) return false;
        return true;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
