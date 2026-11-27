package dev.szx.dimensionworks.transfer.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

/**
 * Application-level service registry breaking cyclic package dependencies across
 * {@code territory}, {@code dispatch}, {@code tutorial}, {@code elastic}, and {@code network}.
 */
public final class TransferServices {
    private static volatile ITutorialLockPolicy tutorialLockPolicy = player -> false;
    private static volatile ITerritoryDispatchHook territoryDispatchHook = (level, territory) -> {};
    private static volatile ITransferNetworkRouter networkRouter = new ITransferNetworkRouter() {
        @Override public void onToggleRequested(ServerPlayer player) {}
        @Override public void onDispatchChoice(ServerPlayer player, BlockPos pos, String route) {}
        @Override public void onDispatchClosed(ServerPlayer player) {}
        @Override public boolean isAlexCavesUnlocked(ServerPlayer player) { return false; }
    };

    private TransferServices() {
    }

    public static void registerTutorialLockPolicy(ITutorialLockPolicy policy) {
        tutorialLockPolicy = Objects.requireNonNull(policy);
    }

    public static void registerTerritoryDispatchHook(ITerritoryDispatchHook hook) {
        territoryDispatchHook = Objects.requireNonNull(hook);
    }

    public static void registerNetworkRouter(ITransferNetworkRouter router) {
        networkRouter = Objects.requireNonNull(router);
    }

    public static boolean isTutorialLocked(Player player) {
        return tutorialLockPolicy.isLocked(player);
    }

    public static void ensureTerritoryTerminal(ServerLevel level, BlockPos terminal) {
        territoryDispatchHook.ensureDispatchTerminal(level, terminal);
    }

    public static ITransferNetworkRouter networkRouter() {
        return networkRouter;
    }
}
