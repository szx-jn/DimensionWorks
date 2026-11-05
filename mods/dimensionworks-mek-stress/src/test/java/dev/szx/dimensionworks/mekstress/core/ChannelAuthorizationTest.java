package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChannelAuthorizationTest {
    @Test void ownerAndCurrentTeammatesMayManage() {
        UUID owner = UUID.randomUUID(), member = UUID.randomUUID();
        assertTrue(ChannelAuthorization.mayManage(owner, owner, (a, b) -> false));
        assertTrue(ChannelAuthorization.mayManage(owner, member, (a, b) -> true));
        assertFalse(ChannelAuthorization.mayManage(owner, member, (a, b) -> false));
        assertTrue(ChannelAuthorization.validChannel(1));
        assertTrue(ChannelAuthorization.validChannel(1024));
        assertFalse(ChannelAuthorization.validChannel(0));
        assertFalse(ChannelAuthorization.validChannel(1025));
    }
}
