package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/** Stable public API for KubeJS and other server-side integrations. */
public final class RpmLimitApi {
    private RpmLimitApi() {}

    public static int getLimit(ServerPlayer player) {
        return RpmLimitManager.getLimit(player.getUUID());
    }

    public static int getLimit(UUID playerId) {
        return RpmLimitManager.getLimit(playerId);
    }

    public static int getLimit(String playerId) {
        return getLimit(UUID.fromString(playerId));
    }

    public static void setLimit(ServerPlayer player, int rpm) {
        RpmLimitManager.setLimit(player, rpm);
    }


    public static int getDefaultLimit() {
        return RpmLimitConfig.DEFAULT_RPM.get();
    }

    public static int getMaxLimit() {
        return RpmLimitConfig.MAX_RPM.get();
    }
}