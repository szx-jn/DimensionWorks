package dev.szx.dimensionworks.spectatortoggle.handler;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

/** Server-authoritative spectator toggle and per-player return point storage. */
public final class SpectatorToggleController {
    private static final String RETURN_POINT_KEY = SpectatorToggleState.RETURN_POINT_KEY;
    private static final Map<MinecraftServer, UUID> SESSION_IDS = new WeakHashMap<>();

    private SpectatorToggleController() {
    }

    public static void toggle(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        if (SpectatorToggleState.isExpired(persistentData, sessionId(player.getServer()))) {
            recover(player);
            return;
        }
        CompoundTag returnPoint = persistentData.getCompound(RETURN_POINT_KEY);

        if (returnPoint.getBoolean("active")) {
            returnToSavedPoint(player, persistentData, returnPoint);
            return;
        }

        GameType previousGameMode = player.gameMode.getGameModeForPlayer();
        if (previousGameMode == GameType.SPECTATOR) {
            recover(player);
            return;
        }

        CompoundTag savedPoint = new CompoundTag();
        savedPoint.putBoolean("active", true);
        savedPoint.putUUID("session", sessionId(player.getServer()));
        savedPoint.putString("dimension", player.level().dimension().location().toString());
        savedPoint.putDouble("x", player.getX());
        savedPoint.putDouble("y", player.getY());
        savedPoint.putDouble("z", player.getZ());
        savedPoint.putFloat("yaw", player.getYRot());
        savedPoint.putFloat("pitch", player.getXRot());
        savedPoint.putInt("game_mode", previousGameMode.getId());
        persistentData.put(RETURN_POINT_KEY, savedPoint);

        if (!player.setGameMode(GameType.SPECTATOR)) {
            persistentData.remove(RETURN_POINT_KEY);
            player.displayClientMessage(
                    Component.translatable("message.dimensionworks_spectator_toggle.enter_failed"), true);
            return;
        }

        player.displayClientMessage(
                Component.translatable("message.dimensionworks_spectator_toggle.entered"), true);
    }

    public static void resetExpiredState(ServerPlayer player) {
        if (SpectatorToggleState.isExpired(player.getPersistentData(), sessionId(player.getServer()))) {
            reset(player);
        }
    }

    public static void resetOnServerStopping(ServerPlayer player) {
        if (player.getPersistentData().contains(RETURN_POINT_KEY)) {
            reset(player);
        }
    }

    private static boolean reset(ServerPlayer player) {
        boolean reset = SpectatorToggleState.reset(
                player.getPersistentData(),
                player.gameMode.getGameModeForPlayer(),
                player.gameMode.getPreviousGameModeForPlayer(),
                player::setGameMode);
        if (reset) {
            player.fallDistance = 0;
        }
        return reset;
    }

    private static void recover(ServerPlayer player) {
        boolean recovered = reset(player);
        player.displayClientMessage(
                Component.translatable(recovered
                        ? "message.dimensionworks_spectator_toggle.recovered"
                        : "message.dimensionworks_spectator_toggle.return_failed"), true);
    }

    private static synchronized UUID sessionId(MinecraftServer server) {
        return SESSION_IDS.computeIfAbsent(server, ignored -> UUID.randomUUID());
    }

    private static void returnToSavedPoint(
            ServerPlayer player,
            CompoundTag persistentData,
            CompoundTag returnPoint) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(returnPoint.getString("dimension"));
        if (dimensionId == null || player.getServer() == null) {
            player.displayClientMessage(
                    Component.translatable("message.dimensionworks_spectator_toggle.return_failed"), true);
            return;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel destination = player.getServer().getLevel(dimensionKey);
        if (destination == null) {
            player.displayClientMessage(
                    Component.translatable("message.dimensionworks_spectator_toggle.dimension_unavailable"), true);
            return;
        }

        GameType previousGameMode = GameType.byId(returnPoint.getInt("game_mode"));
        if (previousGameMode == GameType.SPECTATOR) {
            previousGameMode = GameType.SURVIVAL;
        }

        player.teleportTo(
                destination,
                returnPoint.getDouble("x"),
                returnPoint.getDouble("y"),
                returnPoint.getDouble("z"),
                returnPoint.getFloat("yaw"),
                returnPoint.getFloat("pitch"));

        if (!player.setGameMode(previousGameMode)) {
            player.displayClientMessage(
                    Component.translatable("message.dimensionworks_spectator_toggle.return_failed"), true);
            return;
        }

        persistentData.remove(RETURN_POINT_KEY);
        player.displayClientMessage(
                Component.translatable("message.dimensionworks_spectator_toggle.returned"), true);
    }
}
