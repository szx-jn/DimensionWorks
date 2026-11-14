package dev.szx.dimensionworks.transfer.dispatch;

import dev.szx.dimensionworks.transfer.returnpoint.ReturnPoint;
import dev.szx.dimensionworks.transfer.territory.SafeLanding;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** The six pinned Dimensions of Alex's Caves destinations offered by the upgraded terminal. */
public enum AlexCavesDestination {
    MAGNETIC("magnetic_caves"),
    PRIMORDIAL("primordial_caves"),
    TOXIC("toxic_caves"),
    ABYSSAL("abyssal_chasm"),
    FORLORN("forlorn_hollows"),
    CANDY("candy_cavity");

    public static final String AUTHORIZATION = "dw_dispatch_alex_caves_authorized";
    private final String route;
    private final String path;

    AlexCavesDestination(String path) {
        route = "alex_" + path;
        this.path = path;
    }

    public String route() { return route; }
    public ResourceKey<Level> key() {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation("alex_caves_dimensions", path));
    }

    public static AlexCavesDestination fromRoute(String route) {
        for (AlexCavesDestination destination : values()) {
            if (destination.route.equals(route)) return destination;
        }
        return null;
    }

    public boolean travel(ServerPlayer player, ReturnPoint source) throws IOException {
        var destination = DispatchLevel.getOrLoad(player.server, key());
        if (destination == null) return false;
        // Use the source world's coordinates, never the isolated plot's grid coordinates.
        BlockPos desired = destination.getWorldBorder().clampToBounds(
            source == null ? 0 : source.x(), 64, source == null ? 0 : source.z());
        var landing = SafeLanding.around(destination, desired, 24,
            destination.getMinBuildHeight() + 1, destination.getMaxBuildHeight() - 3, pos -> true);
        if (landing.isEmpty()) return false;

        player.getPersistentData().putBoolean(AUTHORIZATION, true);
        try {
            BlockPos safe = landing.get();
            player.setPortalCooldown();
            player.teleportTo(destination, safe.getX() + .5, safe.getY(), safe.getZ() + .5, player.getYRot(), 0);
            return player.serverLevel() == destination;
        } finally {
            player.getPersistentData().remove(AUTHORIZATION);
        }
    }
}
