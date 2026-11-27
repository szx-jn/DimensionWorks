package dev.szx.dimensionworks.transfer.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Immutable application services assembled once by the mod composition root.
 */
public final class TransferServices {
    public record Services(
        ITutorialLockPolicy tutorialLockPolicy,
        ITerritoryDispatchHook territoryDispatchHook,
        ITransferNetworkRouter networkRouter
    ) {
        public Services {
            Objects.requireNonNull(tutorialLockPolicy, "tutorialLockPolicy");
            Objects.requireNonNull(territoryDispatchHook, "territoryDispatchHook");
            Objects.requireNonNull(networkRouter, "networkRouter");
        }
    }

    private static final AtomicReference<Services> INSTALLED = new AtomicReference<>();

    private TransferServices() {
    }

    public static void install(Services services) {
        Objects.requireNonNull(services, "services");
        if (!INSTALLED.compareAndSet(null, services)) {
            throw new IllegalStateException("Transfer application services are already installed");
        }
    }

    public static boolean isTutorialLocked(Player player) {
        return services().tutorialLockPolicy().isLocked(player);
    }

    public static void ensureTerritoryTerminal(ServerLevel level, BlockPos terminal) {
        services().territoryDispatchHook().ensureDispatchTerminal(level, terminal);
    }

    public static ITransferNetworkRouter networkRouter() {
        return services().networkRouter();
    }

    private static Services services() {
        Services services = INSTALLED.get();
        if (services == null) {
            throw new IllegalStateException("Transfer application services were used before initialization");
        }
        return services;
    }
}
