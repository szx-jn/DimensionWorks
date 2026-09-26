package dev.szx.dimensionworks.transfer.handler;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.access.TransferAccess;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPoint;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPointSavedData;
import dev.szx.dimensionworks.transfer.territory.TerritoryManager;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server-side entry points for the OP2 command and player key request. */
public final class TransferController {
    private TransferController() {
    }

    /** OP 2 administrative entry point. It deliberately bypasses the unlock tag. */
    public static boolean useAdmin(ServerPlayer player) {
        return toggle(player);
    }

    /** Player key entry point. The server-side connection supplies this player. */
    public static boolean useFromKey(ServerPlayer player) {
        if (!TransferAccess.hasUnlockTag(player)) {
            player.displayClientMessage(
                    Component.translatable("message.dimensionworks_transfer.locked"), true);
            return false;
        }
        return toggle(player);
    }

    private static boolean toggle(ServerPlayer player) {
        if (player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return returnToSavedPoint(player);
        }
        return enterTransfer(player);
    }

    private static boolean enterTransfer(ServerPlayer player) {
        ReturnPointSavedData data = ReturnPointSavedData.get(player.server.overworld());
        data.put(player.getUUID(), new ReturnPoint(
                player.level().dimension().location(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()));
        TerritoryManager.sendToTransfer(player);
        return true;
    }

    private static boolean returnToSavedPoint(ServerPlayer player) {
        ReturnPointSavedData data = ReturnPointSavedData.get(player.server.overworld());
        Optional<ReturnPoint> saved = data.get(player.getUUID());
        if (saved.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.dimensionworks_transfer.no_return"), true);
            return false;
        }

        ReturnPoint point = saved.get();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, point.dimension());
        ServerLevel target = player.server.getLevel(dimension);
        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("message.dimensionworks_transfer.dimension_missing"), true);
            return false;
        }

        player.teleportTo(target, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setOnGround(true);
        data.remove(player.getUUID());
        player.displayClientMessage(
                Component.translatable("message.dimensionworks_transfer.returned"), true);
        return true;
    }
}
