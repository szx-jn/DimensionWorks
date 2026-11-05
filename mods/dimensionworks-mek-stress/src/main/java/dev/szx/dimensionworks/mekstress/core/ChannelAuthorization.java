package dev.szx.dimensionworks.mekstress.core;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;

/** Authorization uses the creator's current team, not the team at channel creation. */
public final class ChannelAuthorization {
    public static final int MIN_CHANNEL = 1;
    public static final int MAX_CHANNEL = 1024;

    private ChannelAuthorization() {}

    public static boolean validChannel(int channel) {
        return channel >= MIN_CHANNEL && channel <= MAX_CHANNEL;
    }

    public static boolean mayManage(UUID owner, UUID player, BiPredicate<UUID, UUID> sameCurrentTeam) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(player);
        Objects.requireNonNull(sameCurrentTeam);
        return owner.equals(player) || sameCurrentTeam.test(owner, player);
    }
}
