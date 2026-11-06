package dev.szx.dimensionworks.transfer.dispatch;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPoint;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPointSavedData;
import dev.szx.dimensionworks.transfer.territory.Territory;
import dev.szx.dimensionworks.transfer.territory.TerritoryManager;
import dev.szx.dimensionworks.transfer.tutorial.TutorialManager;
import dev.szx.dimensionworks.transfer.territory.SafeLanding;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.joml.Vector3f;

/** Validates parcel dispatch requests and performs the selected dimension transfer. */
public final class DispatchController {
    private static final Map<UUID, Long> OPEN = new HashMap<>();
    private static final Set<UUID> AUTHORIZED_TWILIGHT_TRAVEL = new HashSet<>();

    private DispatchController() {}

    public static BlockPos terminal(Territory territory) {
        return BlockPos.containing(territory.centerX(), Territory.FLOOR_Y + 1, territory.centerZ());
    }

    public static void ensure(ServerLevel level, Territory territory) {
        BlockPos pos = terminal(territory);
        if (!level.getBlockState(pos).is(DispatchBlocks.TERMINAL.get())) {
            level.setBlockAndUpdate(pos, DispatchBlocks.TERMINAL.get().defaultBlockState());
        }
    }

    public static boolean authorized(ServerPlayer player) {
        return AUTHORIZED_TWILIGHT_TRAVEL.contains(player.getUUID());
    }

    public static boolean eligible(ServerPlayer player, BlockPos pos) {
        Territory territory = TerritoryManager.getTerritory(player);
        return DispatchPolicy.decide(
            player.level().dimension().equals(WDKeys.TRANSFER_LEVEL),
            territory != null && terminal(territory).equals(pos),
            player.distanceToSqr(Vec3.atCenterOf(pos)) < 9,
            PlayerParcel.isPacked(player),
            TutorialManager.locked(player),
            true) == DispatchPolicy.Decision.ALLOWED;
    }

    public static void touch(ServerPlayer player, BlockPos pos) {
        if (!eligible(player, pos)) {
            return;
        }

        boolean firstVisit = !player.getPersistentData()
            .getCompound(Player.PERSISTED_NBT_TAG)
            .getBoolean("dw_terminal_seen");
        if (firstVisit) {
            player.displayClientMessage(
                Component.translatable("message.dimensionworks_transfer.terminal_hint")
                    .withStyle(ChatFormatting.WHITE),
                true);
        }
        net.minecraft.nbt.CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean("dw_terminal_seen", true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        if (OPEN.containsKey(player.getUUID())) {
            return;
        }
        OPEN.put(player.getUUID(), player.server.overworld().getGameTime());
        TransferNetwork.open(player, pos);
    }

    public static void close(ServerPlayer player) {
        OPEN.remove(player.getUUID());
    }

    public static void forceClose(ServerPlayer player) {
        close(player);
        TransferNetwork.closeScreen(player);
    }

    public static void forget(ServerPlayer player) {
        close(player);
    }

    public static void tick(ServerPlayer player) {
        if (!player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }

        Territory territory = TerritoryManager.getTerritory(player);
        long now = player.server.overworld().getGameTime();
        Long openedAt = OPEN.get(player.getUUID());
        if (openedAt != null && (territory == null
            || !PlayerParcel.isPacked(player)
            || TutorialManager.locked(player)
            || player.distanceToSqr(Vec3.atCenterOf(terminal(territory))) > 144
            || now - openedAt > 600)) {
            forceClose(player);
        }

        if (player.tickCount % 40 != 0 || territory == null) {
            return;
        }
        boolean hasSeen = player.getPersistentData()
            .getCompound(Player.PERSISTED_NBT_TAG)
            .getBoolean("dw_terminal_seen");
        if (!hasSeen && player.distanceToSqr(Vec3.atCenterOf(terminal(territory))) < 64) {
            player.displayClientMessage(
                Component.translatable("message.dimensionworks_transfer.terminal_hint")
                    .withStyle(ChatFormatting.WHITE),
                true);
        }
    }

    /**
     * Consumes the current terminal session, then dispatches the packed player. A malformed,
     * stale, out-of-range or locked request never reaches the destination transfer code.
     */
    public static boolean send(ServerPlayer player, BlockPos terminal, String route) {
        if (!eligible(player, terminal) || !OPEN.containsKey(player.getUUID())) {
            return false;
        }

        forceClose(player);
        if (route.equals("alexscaves") || route.equals("aether")) {
            player.displayClientMessage(
                Component.translatable("message.dimensionworks_transfer.destination_locked"), true);
            return false;
        }

        ReturnPoint source = ReturnPointSavedData.get(player.server.overworld())
            .get(player.getUUID())
            .orElse(null);
        boolean arrived;
        try {
            arrived = switch (route) {
                case "overworld" -> toOverworld(player);
                case "nether" -> toNether(player, source);
                case "end" -> toEnd(player);
                case "twilight" -> toTwilight(player, source);
                default -> false;
            };
        } catch (ReflectiveOperationException | IOException | RuntimeException exception) {
            WDTransfer.LOGGER.error("Could not dispatch player to route {}", route, exception);
            arrived = false;
        }

        if (!arrived) {
            player.displayClientMessage(
                Component.translatable("message.dimensionworks_transfer.destination_unavailable"), true);
            return false;
        }
        if (!PlayerParcel.unpack(player)) {
            TerritoryManager.sendToTransfer(player);
            player.displayClientMessage(
                Component.translatable("message.dimensionworks_transfer.unpack_failed"), false);
            return false;
        }
        ReturnPointSavedData.get(player.server.overworld()).remove(player.getUUID());
        player.setPortalCooldown();
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.inventoryMenu.sendAllDataToRemote();
        burst(player);
        return true;
    }

    private static boolean toOverworld(ServerPlayer player) {
        ResourceKey<Level> respawnDimension = player.getRespawnDimension();
        ServerLevel destination = player.server.getLevel(respawnDimension);
        Vec3 target = null;
        float yaw = player.getRespawnAngle();
        if (Level.OVERWORLD.equals(respawnDimension)
            && destination != null
            && player.getRespawnPosition() != null) {
            target = Player.findRespawnPositionAndUseSpawnBlock(
                destination,
                player.getRespawnPosition(),
                yaw,
                player.isRespawnForced(),
                true).orElse(null);
        }

        if (target == null) {
            destination = player.server.overworld();
            BlockPos spawn = destination.getSharedSpawnPos();
            BlockPos safe = destination.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                spawn);
            target = Vec3.atBottomCenterOf(safe);
            yaw = 0;
        }

        player.teleportTo(destination, target.x, target.y, target.z, yaw, 0);
        return player.serverLevel() == destination;
    }

    private static boolean toNether(ServerPlayer player, ReturnPoint source) {
        ServerLevel destination = player.server.getLevel(Level.NETHER);
        if (destination == null) {
            return false;
        }

        ServerLevel origin = source == null
            ? player.server.overworld()
            : player.server.getLevel(ResourceKey.create(
                Registries.DIMENSION,
                source.dimension()));
        double scale = origin == null
            ? 1
            : net.minecraft.world.level.dimension.DimensionType.getTeleportationScale(
                origin.dimensionType(),
                destination.dimensionType());
        double originX = source == null ? player.getX() : source.x();
        double originY = source == null ? 64 : source.y();
        double originZ = source == null ? player.getZ() : source.z();
        BlockPos desired = destination.getWorldBorder().clampToBounds(
            (int) (originX * scale),
            net.minecraft.util.Mth.clamp((int) originY,
                destination.getMinBuildHeight() + 8, destination.getMaxBuildHeight() - 8),
            (int) (originZ * scale));

        // Nether roof/bedrock are excluded. No portal search or construction is involved.
        var landing = SafeLanding.around(destination, desired, 24,
            destination.getMinBuildHeight() + 1, Math.min(125, destination.getMaxBuildHeight() - 3), pos -> true);
        if (landing.isEmpty()) return false;
        BlockPos safe = landing.get();
        player.setPortalCooldown();
        player.teleportTo(destination, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5,
            player.getYRot(), 0);
        return player.serverLevel() == destination;
    }

    private static boolean toEnd(ServerPlayer player) {
        ServerLevel destination = player.server.getLevel(Level.END);
        if (destination == null) {
            return false;
        }
        ServerLevel.makeObsidianPlatform(destination);
        BlockPos spawn = ServerLevel.END_SPAWN_POINT;
        player.teleportTo(destination, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 90, 0);
        return player.serverLevel() == destination;
    }

    private static boolean toTwilight(ServerPlayer player, ReturnPoint source)
            throws ReflectiveOperationException, IOException {
        ServerLevel destination = TwilightDestination.getOrLoad(player.server);
        if (destination == null) {
            return false;
        }

        ServerLevel origin = source == null ? player.server.overworld() : player.server.getLevel(
            ResourceKey.create(Registries.DIMENSION, source.dimension()));
        double scale = origin == null ? 1 : net.minecraft.world.level.dimension.DimensionType.getTeleportationScale(
            origin.dimensionType(), destination.dimensionType());
        BlockPos desired = destination.getWorldBorder().clampToBounds(
            (source == null ? 0 : source.x()) * scale, 64, (source == null ? 0 : source.z()) * scale);
        destination.getChunk(desired);
        desired = destination.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, desired);
        // This stable TF method performs its progression/biome rerouting without makePortal().
        var reroute = Class.forName("twilightforest.world.TFTeleporter").getDeclaredMethod(
            "moveToSafeCoords", ServerLevel.class, net.minecraft.world.entity.Entity.class, BlockPos.class);
        reroute.setAccessible(true);
        var info = (net.minecraft.world.level.portal.PortalInfo) reroute.invoke(null, destination, player, desired);
        var safeAround = Class.forName("twilightforest.world.TFTeleporter").getMethod(
            "isSafeAround", Level.class, BlockPos.class, net.minecraft.world.entity.Entity.class, boolean.class);
        boolean enforced = (boolean) Class.forName("twilightforest.util.LandmarkUtil")
            .getMethod("isProgressionEnforced", Level.class).invoke(null, destination);
        var landing = SafeLanding.around(destination, BlockPos.containing(info.pos), 24,
            destination.getMinBuildHeight() + 1, destination.getMaxBuildHeight() - 3, pos -> {
                try { return (boolean) safeAround.invoke(null, destination, pos, player, enforced); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            });
        if (landing.isEmpty()) return false;
        AUTHORIZED_TWILIGHT_TRAVEL.add(player.getUUID());
        boolean previous = player.getPersistentData().getBoolean(TwilightDestination.AUTHORIZATION);
        player.getPersistentData().putBoolean(TwilightDestination.AUTHORIZATION, true);
        try {
            BlockPos safe = landing.get();
            player.setPortalCooldown();
            player.teleportTo(destination, safe.getX() + .5, safe.getY(), safe.getZ() + .5, player.getYRot(), 0);
            return player.serverLevel() == destination;
        } finally {
            AUTHORIZED_TWILIGHT_TRAVEL.remove(player.getUUID());
            if (previous) player.getPersistentData().putBoolean(TwilightDestination.AUTHORIZATION, true);
            else player.getPersistentData().remove(TwilightDestination.AUTHORIZATION);
        }
    }

    private static void burst(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0, player.getBbHeight() / 2, 0);
        for (int i = 0; i < 100; i++) {
            int rgb = java.awt.Color.HSBtoRGB(i / 100f, 0.9f, 1);
            DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f),
                1.2f);
            double x = center.x + (level.random.nextDouble() - 0.5) * 3;
            double y = center.y + (level.random.nextDouble() - 0.5) * 3;
            double z = center.z + (level.random.nextDouble() - 0.5) * 3;
            level.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
            SoundSource.PLAYERS, 1, 1);
    }
}
