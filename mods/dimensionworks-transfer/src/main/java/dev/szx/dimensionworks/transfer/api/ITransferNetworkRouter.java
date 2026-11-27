package dev.szx.dimensionworks.transfer.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Application-layer server packet handlers wired into {@code TransferNetwork}. */
public interface ITransferNetworkRouter {
    void onToggleRequested(ServerPlayer player);

    void onDispatchChoice(ServerPlayer player, BlockPos pos, String route);

    void onDispatchClosed(ServerPlayer player);

    boolean isAlexCavesUnlocked(ServerPlayer player);
}
