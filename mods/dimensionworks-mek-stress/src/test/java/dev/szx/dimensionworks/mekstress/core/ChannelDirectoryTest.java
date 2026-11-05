package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChannelDirectoryTest {
    @Test void teamClaimsAndTeardownDoNotLeakToOtherPlayers() {
        var directory = new ChannelDirectory();
        var owner = UUID.randomUUID();
        var other = UUID.randomUUID();
        var first = new ChannelDirectory.Endpoint("minecraft:overworld", 1);
        var second = new ChannelDirectory.Endpoint("minecraft:the_nether", 1);
        assertTrue(directory.join(1, owner, first, (a, b) -> false));
        assertFalse(directory.join(1, other, second, (a, b) -> false));
        assertTrue(directory.join(1, other, second, (a, b) -> true));
        assertFalse(directory.join(2, owner, second, (a, b) -> false));
        assertFalse(directory.join(1, other, new ChannelDirectory.Endpoint("minecraft:the_end", 1), (a, b) -> true));
        assertFalse(directory.removeEndpoint(first));
        assertTrue(directory.removeEndpoint(second));
        assertFalse(directory.join(1, other, second, (a, b) -> true));
        directory.releaseEmpty(1);
        assertTrue(directory.join(1, other, second, (a, b) -> false));
        assertEquals(other, directory.get(1).owner());
    }

    @Test void channelAcceptsOnlyOnePair() {
        var directory = new ChannelDirectory();
        var owner = UUID.randomUUID();
        assertTrue(directory.join(5, owner, new ChannelDirectory.Endpoint("minecraft:overworld", 1), (a, b) -> false));
        assertTrue(directory.join(5, owner, new ChannelDirectory.Endpoint("minecraft:the_nether", 2), (a, b) -> false));
        assertFalse(directory.join(5, owner, new ChannelDirectory.Endpoint("minecraft:the_end", 3), (a, b) -> false));
    }

    @Test void restorePreservesClaimsAndRejectsDuplicateMembership() {
        var owner = UUID.randomUUID();
        var endpoint = new ChannelDirectory.Endpoint("minecraft:overworld", 10);
        var saved = Map.of(7, new ChannelDirectory.Channel(owner, Set.of(endpoint)));
        var restored = ChannelDirectory.restore(saved);
        assertEquals(saved, restored.snapshot());
        assertThrows(IllegalArgumentException.class, () -> ChannelDirectory.restore(Map.of(
            7, new ChannelDirectory.Channel(owner, Set.of(endpoint)),
            8, new ChannelDirectory.Channel(owner, Set.of(endpoint)))));
    }
}
