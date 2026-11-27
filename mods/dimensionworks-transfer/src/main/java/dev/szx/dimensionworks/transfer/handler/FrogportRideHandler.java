package dev.szx.dimensionworks.transfer.handler;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget.ChainConveyorFrogportTarget;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Owns Frogport-to-chain-conveyor ride state and cooldowns for packed players. */
@EventBusSubscriber(modid = WDTransfer.MOD_ID)
final class FrogportRideHandler {
    private static final Map<UUID, FrogRide> FROG_RIDES = new HashMap<>();
    private static final Map<UUID, Long> FROG_COOLDOWNS = new HashMap<>();
    private static final int FROG_COOLDOWN_TICKS = 100;

    private record FrogRide(ResourceKey<Level> dimension, BlockPos frog, BlockPos chain,
                            float position, BlockPos connection, int ticks) {
    }

    private FrogportRideHandler() {
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearRide(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        clearRide(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearAll(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clearRide(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clearServer();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tickFrogport(player);
        }
    }

    static void clearRide(UUID playerId) {
        FROG_RIDES.remove(playerId);
    }

    static void clearAll(UUID playerId) {
        FROG_RIDES.remove(playerId);
        FROG_COOLDOWNS.remove(playerId);
    }

    static void clearServer() {
        FROG_RIDES.clear();
        FROG_COOLDOWNS.clear();
    }

    static void tickFrogport(ServerPlayer player) {
        long now = player.server.overworld().getGameTime();
        Long cooldown = FROG_COOLDOWNS.get(player.getUUID());
        if (cooldown != null && now >= cooldown) {
            FROG_COOLDOWNS.remove(player.getUUID());
            cooldown = null;
        }
        if (!player.isAlive() || !PlayerParcel.isPacked(player)
            || player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            cancelFrogRide(player, now);
            return;
        }
        FrogRide active = FROG_RIDES.get(player.getUUID());
        if (active != null) {
            if (!active.dimension().equals(player.level().dimension())
                || player.distanceToSqr(Vec3.atCenterOf(active.frog())) > 16
                || !player.level().hasChunkAt(active.frog()) || !player.level().hasChunkAt(active.chain())
                || !(player.level().getBlockEntity(active.frog()) instanceof FrogportBlockEntity frogport)
                || !(frogport.target instanceof ChainConveyorFrogportTarget target)
                || !active.chain().equals(active.frog().offset(target.relativePos))
                || Float.compare(active.position(), target.chainPos) != 0
                || !Objects.equals(active.connection(), target.connection)
                || !(player.level().getBlockEntity(active.chain()) instanceof ChainConveyorBlockEntity conveyor)
                || Math.abs(conveyor.getSpeed()) <= 0.001f
                || active.connection() != null && !conveyor.connections.contains(active.connection())) {
                cancelFrogRide(player, now);
                return;
            }
            BlockPos frog = active.frog();
            if (active.ticks() == 0) {
                player.level().playSound(null, frog, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, .8f, 1.2f);
            }
            player.serverLevel().sendParticles(ParticleTypes.BUBBLE, frog.getX() + .5, frog.getY() + .8, frog.getZ() + .5, 4, .28, .3, .28, .03);
            Vec3 mouth = Vec3.atCenterOf(frog).add(0, 0.4, 0);
            player.connection.teleport(player.getX() + (mouth.x - player.getX()) * .35, player.getY() + (mouth.y - player.getY()) * .35,
                player.getZ() + (mouth.z - player.getZ()) * .35, player.getYRot(), player.getXRot());
            if (active.ticks() >= 8) {
                player.serverLevel().sendParticles(ParticleTypes.POOF, frog.getX() + .5, frog.getY() + .8, frog.getZ() + .5, 10, .3, .3, .3, .04);
                player.level().playSound(null, frog, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, .8f, 1.4f);
                var be = player.level().getBlockEntity(active.chain());
                if (be instanceof ChainConveyorBlockEntity chain && Math.abs(chain.getSpeed()) > 0.001f) {
                    var pos = chain.getPackagePosition(active.position(), active.connection());
                    player.connection.teleport(pos.x, pos.y - player.getBbHeight() - .5, pos.z, player.getYRot(), player.getXRot());
                    ServerChainConveyorHandler.handleTTLPacket(player);
                    TransferNetwork.chainRide(player, active.chain(), active.position(), active.connection());
                }
                FROG_RIDES.remove(player.getUUID());
                FROG_COOLDOWNS.put(player.getUUID(), now + FROG_COOLDOWN_TICKS);
            } else {
                FROG_RIDES.put(player.getUUID(), new FrogRide(active.dimension(), active.frog(), active.chain(), active.position(), active.connection(), active.ticks() + 1));
            }
            return;
        }
        if (cooldown != null || ServerChainConveyorHandler.hangingPlayers.containsKey(player.getUUID())) {
            return;
        }
        if (player.tickCount % 5 != 0) {
            return;
        }
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
            if (!player.level().hasChunkAt(pos)) continue;
            if (!(player.level().getBlockEntity(pos) instanceof FrogportBlockEntity frog)) continue;
            if (!(frog.target instanceof ChainConveyorFrogportTarget target)) continue;
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 16) continue;
            BlockPos chainPos = pos.offset(target.relativePos).immutable();
            if (!player.level().hasChunkAt(chainPos)) continue;
            if (!(player.level().getBlockEntity(chainPos) instanceof ChainConveyorBlockEntity chain)
                || Math.abs(chain.getSpeed()) <= 0.001f
                || target.connection != null && !chain.connections.contains(target.connection)) continue;
            FROG_RIDES.put(player.getUUID(), new FrogRide(player.level().dimension(), pos.immutable(), chainPos, target.chainPos, target.connection, 0));
            break;
        }
    }

    private static void cancelFrogRide(ServerPlayer player, long now) {
        if (FROG_RIDES.remove(player.getUUID()) != null) {
            FROG_COOLDOWNS.put(player.getUUID(), now + FROG_COOLDOWN_TICKS);
        }
    }
}
