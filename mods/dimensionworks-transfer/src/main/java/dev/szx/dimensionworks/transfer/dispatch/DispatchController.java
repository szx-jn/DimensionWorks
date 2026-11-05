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
import java.lang.reflect.Constructor;
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
                case "twilight" -> toTwilight(player);
                default -> false;
            };
        } catch (ReflectiveOperationException | RuntimeException exception) {
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

        var portal = destination.getPortalForcer().findPortalAround(
            desired, true, destination.getWorldBorder());
        if (portal.isEmpty()) {
            portal = destination.getPortalForcer().createPortal(desired, Direction.Axis.X);
        }
        if (portal.isEmpty()) {
            return false;
        }

        BlockPos safe = portal.get().minCorner;
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

    private static boolean toTwilight(ServerPlayer player) throws ReflectiveOperationException {
        ResourceKey<Level> key = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation("twilightforest", "twilight_forest"));
        ServerLevel destination = player.server.getLevel(key);
        if (destination == null) {
            return false;
        }

        Class<?> teleporterClass = Class.forName("twilightforest.world.TFTeleporter");
        Constructor<?> constructor = teleporterClass.getConstructor(boolean.class);
        ITeleporter teleporter = (ITeleporter) constructor.newInstance(false);
        AUTHORIZED_TWILIGHT_TRAVEL.add(player.getUUID());
        try {
            return player.changeDimension(destination, teleporter) != null
                && player.serverLevel() == destination;
        } finally {
            AUTHORIZED_TWILIGHT_TRAVEL.remove(player.getUUID());
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
