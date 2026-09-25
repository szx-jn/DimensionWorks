package dev.szx.dimensionworks.transfer.access;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TransferAccessTest {
    @Test
    void unlockTagIsRequired() {
        assertFalse(TransferAccess.hasUnlockTag(Set.of()));
        assertTrue(TransferAccess.hasUnlockTag(Set.of(TransferAccess.UNLOCK_TAG)));
    }
}
