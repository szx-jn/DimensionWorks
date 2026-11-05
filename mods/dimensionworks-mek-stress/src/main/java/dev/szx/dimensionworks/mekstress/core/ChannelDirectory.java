package dev.szx.dimensionworks.mekstress.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

/** Channel claims and endpoint membership. Persistence and teardown belong to the server integration. */
public final class ChannelDirectory {
    public static final int MAX_ENDPOINTS_PER_CHANNEL = 2;
    public record Endpoint(String dimension, long position) {
        public Endpoint {
            Objects.requireNonNull(dimension);
            if (dimension.isBlank()) throw new IllegalArgumentException();
        }
    }
    public record Channel(UUID owner, Set<Endpoint> endpoints) {
        public Channel { Objects.requireNonNull(owner); endpoints = Set.copyOf(endpoints); }
    }
    private static final class MutableChannel {
        final UUID owner;
        final Set<Endpoint> endpoints = new LinkedHashSet<>();
        MutableChannel(UUID owner) { this.owner = owner; }
    }
    private final Map<Integer, MutableChannel> channels = new HashMap<>();
    private final Map<Endpoint, Integer> membership = new HashMap<>();

    public boolean join(int number, UUID player, Endpoint endpoint, BiPredicate<UUID, UUID> sameTeam) {
        if (!ChannelAuthorization.validChannel(number)) throw new IllegalArgumentException("Invalid channel");
        Objects.requireNonNull(player);
        Objects.requireNonNull(endpoint);
        Objects.requireNonNull(sameTeam);
        Integer current = membership.get(endpoint);
        if (current != null && current != number) return false;
        MutableChannel channel = channels.get(number);
        if (channel != null) {
            // Keep the claim closed while last-endpoint teardown is packaging its stock.
            // The exact owner can still recover an interrupted teardown after a restart.
            if (channel.endpoints.isEmpty() && !channel.owner.equals(player)) return false;
            if (channel.endpoints.size() >= MAX_ENDPOINTS_PER_CHANNEL && !channel.endpoints.contains(endpoint)) return false;
            if (!ChannelAuthorization.mayManage(channel.owner, player, sameTeam)) return false;
        }
        if (channel == null) {
            channel = new MutableChannel(player);
            channels.put(number, channel);
        }
        channel.endpoints.add(endpoint);
        membership.put(endpoint, number);
        return true;
    }

    public Channel get(int number) {
        MutableChannel channel = channels.get(number);
        return channel == null ? null : new Channel(channel.owner, channel.endpoints);
    }

    public Map<Integer, Channel> snapshot() {
        Map<Integer, Channel> result = new HashMap<>();
        channels.forEach((number, channel) -> result.put(number, new Channel(channel.owner, channel.endpoints)));
        return Map.copyOf(result);
    }

    /** Restore all claims atomically; malformed or duplicate endpoints fail closed. */
    public static ChannelDirectory restore(Map<Integer, Channel> saved) {
        Objects.requireNonNull(saved);
        ChannelDirectory result = new ChannelDirectory();
        saved.forEach((number, channel) -> {
            if (number == null || !ChannelAuthorization.validChannel(number) || channel == null)
                throw new IllegalArgumentException("Invalid saved channel");
            MutableChannel restored = new MutableChannel(channel.owner());
            for (Endpoint endpoint : channel.endpoints()) {
                if (result.membership.putIfAbsent(endpoint, number) != null)
                    throw new IllegalArgumentException("Endpoint belongs to multiple channels");
                restored.endpoints.add(endpoint);
            }
            result.channels.put(number, restored);
        });
        return result;
    }

    /** Returns true when this removal leaves a channel needing transactional teardown. */
    public boolean removeEndpoint(Endpoint endpoint) {
        Integer number = membership.remove(endpoint);
        if (number == null) return false;
        MutableChannel channel = channels.get(number);
        channel.endpoints.remove(endpoint);
        return channel.endpoints.isEmpty();
    }

    /** Only release an empty claim after its inventory and upgrades were handled successfully. */
    public void releaseEmpty(int number) {
        MutableChannel channel = channels.get(number);
        if (channel == null || !channel.endpoints.isEmpty()) throw new IllegalStateException();
        channels.remove(number);
    }
}
