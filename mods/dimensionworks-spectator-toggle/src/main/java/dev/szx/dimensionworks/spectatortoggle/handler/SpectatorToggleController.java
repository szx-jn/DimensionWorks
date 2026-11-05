package dev.szx.dimensionworks.spectatortoggle.handler;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Server-authoritative spectator toggle and per-player return point storage. */
public final class SpectatorToggleController {
    private static final String RETURN_POINT_KEY = "dimensionworks_spectator_toggle";

    private static UUID serverSession;

    public static void startServerSession() {
        serverSession = UUID.randomUUID();
    }

    public static void restoreAfterRestart(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(RETURN_POINT_KEY)) {
            return;
        }

        CompoundTag savedPoint = data.getCompound(RETURN_POINT_KEY);
        if (serverSession != null && savedPoint.hasUUID("session")
                && serverSession.equals(savedPoint.getUUID("session"))) {
            return;
        }

        if (savedPoint.getBoolean("active")) {
            player.setGameMode(previousGameMode(savedPoint));
        }
        data.remove(RETURN_POINT_KEY);
    }

    private SpectatorToggleController() {
    }

    public static void toggle(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag returnPoint = persistentData.getCompound(RETURN_POINT_KEY);

        if (returnPoint.getBoolean("active")) {
            returnToSavedPoint(player, persistentData, returnPoint);
            return;
        }

        GameType previousGameMode = player.gameMode.getGameModeForPlayer();
        if (previousGameMode == GameType.SPECTATOR) {
            player.displayClientMessage(
                    Component.translatable("message.dimensionworks_spectator_toggle.already_spectator"), true);
            return;
        }

        CompoundTag savedPoint = new CompoundTag();
        savedPoint.putBoolean("active", true);
        savedPoint.putUUID("session", serverSession);
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

    private static GameType previousGameMode(CompoundTag savedPoint) {
        GameType mode = GameType.byId(savedPoint.getInt("game_mode"));
        return mode == GameType.SPECTATOR ? GameType.SURVIVAL : mode;
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

        GameType previousGameMode = previousGameMode(returnPoint);

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
