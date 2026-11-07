package dev.szx.dimensionworks.transfer.client;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class PackedPlayerTransportClient {
    private PackedPlayerTransportClient() {}
    public static boolean riding(Player player) {
        return player == Minecraft.getInstance().player && ChainConveyorRidingHandler.ridingChainConveyor != null;
    }
}
