package dev.szx.dimensionworks.transfer.handler;

import com.mojang.brigadier.CommandDispatcher;
import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.territory.Territory;
import dev.szx.dimensionworks.transfer.territory.TerritoryBuilder;
import dev.szx.dimensionworks.transfer.territory.TerritoryManager;
import dev.szx.dimensionworks.transfer.territory.TransferBlockPolicy;
import dev.szx.dimensionworks.transfer.tutorial.TutorialManager;
import dev.szx.dimensionworks.transfer.dispatch.DispatchController;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
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
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DispatchController.forceClose(player);
        FROG_RIDES.remove(player.getUUID());
        player.inventoryMenu.sendAllDataToRemote();
        if (!event.getTo().equals(WDKeys.TRANSFER_LEVEL)) {
            dev.szx.dimensionworks.transfer.territory.PlotChunkLoading.deactivate(player.getUUID());
        }
        if (!event.getTo().equals(WDKeys.TRANSFER_LEVEL)) return;
        TerritoryManager.enter(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DispatchController.forget(player);
            FROG_RIDES.remove(player.getUUID());
            dev.szx.dimensionworks.transfer.territory.PlotChunkLoading.deactivate(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(WDKeys.TRANSFER_LEVEL))
            TerritoryManager.enter(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(WDKeys.TRANSFER_LEVEL))
            TerritoryManager.enter(player);
    }

    @SubscribeEvent
    public static void onStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        TerritoryManager.clear();
        FROG_RIDES.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        TerritoryBuilder.tick();
    }

    /** Hard confinement: the plot boundary wins over any movement, piston or teleport. */
    private static final java.util.Map<java.util.UUID, FrogRide> FROG_RIDES = new java.util.HashMap<>();
    private record FrogRide(net.minecraft.core.BlockPos frog, net.minecraft.core.BlockPos chain, float position, net.minecraft.core.BlockPos connection, int ticks) {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        TutorialManager.check(player);
        TutorialManager.tick(player);
        DispatchController.tick(player);
        if (!player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            if (event.phase == TickEvent.Phase.END && dev.szx.dimensionworks.transfer.compat.PlayerParcel.isPacked(player)) {
                tickFrogport(player);
            }
            return;
        }

        Territory territory = TerritoryManager.getTerritory(player);
        if (territory == null) {
            return;
        }
        confine(player, territory);
    }

    @SubscribeEvent
    public static void tutorialItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (TutorialManager.locked(player)) {
            event.setCanceled(true);
            if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) TutorialManager.use(player);
            return;
        }
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) {
            event.setCanceled(true);
            TutorialManager.use(player);
        }
    }

    @SubscribeEvent
    public static void tutorialBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) {
            event.setCanceled(true);
            if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) TutorialManager.use(player);
        }
    }

    @SubscribeEvent
    public static void tutorialEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) {
            event.setCanceled(true);
            if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) TutorialManager.use(player);
        }
    }

    @SubscribeEvent
    public static void tutorialEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) {
            event.setCanceled(true);
            if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) TutorialManager.use(player);
        }
    }

    @SubscribeEvent
    public static void tutorialLeft(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tutorialAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tutorialDrop(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockOldTwilightPortal(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
            && event.getDimension().location().toString().equals("twilightforest:twilight_forest")
            && !DispatchController.authorized(player)) event.setCanceled(true);
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


    /** Frogs swallow a packed player briefly, then hand Create's chain rider to the client. */
    private static void tickFrogport(ServerPlayer player) {
        FrogRide active = FROG_RIDES.get(player.getUUID());
        if (active != null) {
            if (!dev.szx.dimensionworks.transfer.compat.PlayerParcel.isPacked(player)) { FROG_RIDES.remove(player.getUUID()); return; }
            net.minecraft.core.BlockPos frog = active.frog();
            if (active.ticks() == 0) player.level().playSound(null, frog, net.minecraft.sounds.SoundEvents.SLIME_BLOCK_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, .8f, 1.2f);
            player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE, frog.getX()+.5, frog.getY()+.8, frog.getZ()+.5, 4, .28, .3, .28, .03);
            net.minecraft.world.phys.Vec3 target = net.minecraft.world.phys.Vec3.atCenterOf(frog).add(0, 0.4, 0);
            player.connection.teleport(player.getX() + (target.x-player.getX()) * .35, player.getY() + (target.y-player.getY()) * .35,
                player.getZ() + (target.z-player.getZ()) * .35, player.getYRot(), player.getXRot());
            if (active.ticks() >= 8) {
                player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, frog.getX()+.5, frog.getY()+.8, frog.getZ()+.5, 10, .3, .3, .3, .04);
                player.level().playSound(null, frog, net.minecraft.sounds.SoundEvents.CHAIN_HIT, net.minecraft.sounds.SoundSource.BLOCKS, .8f, 1.4f);
                var be = player.level().getBlockEntity(active.chain());
                if (be instanceof com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity chain
                    && Math.abs(chain.getSpeed()) > 0.001f) {
                    var pos = chain.getPackagePosition(active.position(), active.connection());
                    player.connection.teleport(pos.x, pos.y - player.getBbHeight() - .5, pos.z, player.getYRot(), player.getXRot());
                    com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler.handleTTLPacket(player);
                    dev.szx.dimensionworks.transfer.network.TransferNetwork.chainRide(player, active.chain(), active.position(), active.connection());
                }
                FROG_RIDES.remove(player.getUUID());
            } else FROG_RIDES.put(player.getUUID(), new FrogRide(active.frog(), active.chain(), active.position(), active.connection(), active.ticks()+1));
            return;
        }
        if (player.tickCount % 5 != 0) return;
        net.minecraft.core.BlockPos origin = player.blockPosition();
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(origin.offset(-3,-2,-3),origin.offset(3,2,3))) {
            if (!player.level().hasChunkAt(pos)) continue;
            if (!(player.level().getBlockEntity(pos) instanceof com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity frog)) continue;
            if (!(frog.target instanceof com.simibubi.create.content.logistics.packagePort.PackagePortTarget.ChainConveyorFrogportTarget target)) continue;
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 16) continue;
            net.minecraft.core.BlockPos chainPos = pos.offset(target.relativePos).immutable();
            if (!(player.level().getBlockEntity(chainPos) instanceof com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity chain)
                || Math.abs(chain.getSpeed()) <= 0.001f) continue;
            FROG_RIDES.put(player.getUUID(), new FrogRide(pos.immutable(), chainPos, target.chainPos, target.connection, 0));
            break;
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
        if (event.getState().is(dev.szx.dimensionworks.transfer.dispatch.DispatchBlocks.TERMINAL.get())) {
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
        boolean forbidden = TransferBlockPolicy.forbidden(event.getPlacedBlock())
            || TransferBlockPolicy.forbidden(level.getBlockState(event.getPos()));
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            forbidden |= multi.getReplacedBlockSnapshots().stream()
                .anyMatch(snapshot -> TransferBlockPolicy.forbidden(snapshot.getCurrentBlock()));
        }
        if (forbidden) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) TransferBlockPolicy.notifyDenied(player);
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
        dispatcher.register(Commands.literal("dwtransfer")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return TransferController.useAdmin(player) ? 1 : 0;
                }));
    }
}
