package dev.szx.dimensionworks.transfer.compat;

import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import net.minecraft.world.entity.player.Player;

/** A managed scoreboard tag follows CMP state on both logical sides. */
public final class PackedPlayerTransport {
    public static final String TAG = "dimensionworks_transfer.packed_player";
    private PackedPlayerTransport() {}

    public static void synchronize(Player player) {
        if (PlayerParcel.isPacked(player)) player.addTag(TAG);
        else player.removeTag(TAG);
    }

    public static boolean ignoresBlocks(Player player) {
        synchronize(player);
        return player.isAlive() && player.getTags().contains(TAG)
            && (player.level().isClientSide
                ? dev.szx.dimensionworks.transfer.client.PackedPlayerTransportClient.riding(player)
                : ServerChainConveyorHandler.hangingPlayers.containsKey(player.getUUID()));
    }
}
