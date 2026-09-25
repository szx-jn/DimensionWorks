package dev.szx.dimensionworks.transfer.access;

import java.util.Collection;
import net.minecraft.server.level.ServerPlayer;

/** Shared access-tag checks for the transfer dimension. */
public final class TransferAccess {
    public static final String UNLOCK_TAG = "dimensionworks_transfer.unlocked";

    private TransferAccess() {
    }

    public static boolean hasUnlockTag(Collection<String> tags) {
        return tags.contains(UNLOCK_TAG);
    }

    public static boolean hasUnlockTag(ServerPlayer player) {
        return hasUnlockTag(player.getTags());
    }
}
