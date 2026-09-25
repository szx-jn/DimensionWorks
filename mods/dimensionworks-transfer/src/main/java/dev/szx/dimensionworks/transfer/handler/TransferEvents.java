package dev.szx.dimensionworks.transfer.handler;

import com.mojang.brigadier.CommandDispatcher;
import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.territory.Territory;
import dev.szx.dimensionworks.transfer.territory.TerritoryBuilder;
import dev.szx.dimensionworks.transfer.territory.TerritoryManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WDTransfer.MOD_ID)
public final class TransferEvents {
    private TransferEvents() {
    }

    /** Claims and builds a plot the moment a player arrives in {@code dw:transfer}. */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getTo().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TerritoryManager.enter(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        TerritoryBuilder.tick();
    }

    /** Hard confinement: the plot boundary wins over any movement, piston or teleport. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }

        Territory territory = TerritoryManager.getTerritory(player);
        if (territory == null) {
            return;
        }
        confine(player, territory);
    }

    private static void confine(ServerPlayer player, Territory territory) {
        double minX = territory.interiorMinX() + 0.3D;
        double maxX = territory.interiorMaxX() + 0.7D;
        double minZ = territory.interiorMinZ() + 0.3D;
        double maxZ = territory.interiorMaxZ() + 0.7D;

        double x = Mth.clamp(player.getX(), minX, maxX);
        double z = Mth.clamp(player.getZ(), minZ, maxZ);

        if (x != player.getX() || z != player.getZ()) {
            player.setPos(x, player.getY(), z);
            player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
        }
    }

    /**
     * Permanent peace: no hostile mob may exist in {@code dw:transfer}. Natural spawning is
     * already impossible because the biome declares no spawners; this catches everything else.
     * Other dimensions are untouched because the dimension key is checked first.
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!event.getLevel().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER) {
            event.setCanceled(true);
        }
    }

    /** Keeps the barrier ring indestructible and forbids edits outside the owner's plot. */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }

        if (event.getState().is(Blocks.BARRIER)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        Territory territory = TerritoryManager.getTerritory(player);
        if (territory != null && !territory.contains(event.getPos().getX(), event.getPos().getZ())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Territory territory = TerritoryManager.getTerritory(player);
        if (territory != null && !territory.contains(event.getPos().getX(), event.getPos().getZ())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("dwtransfer").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            TerritoryManager.sendToTransfer(player);
            return 1;
        }));
    }
}
