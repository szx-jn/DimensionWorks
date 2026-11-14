package dev.szx.dimensionworks.magicegg;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class SelectionSessionTest {
    private static final List<String> CATALOGUE = List.of("minecraft:speed", "minecraft:levitation", "minecraft:poison");

    @Test
    void confirmationValidatesIdsAndCannotBeReplayed() {
        SelectionSession session = new SelectionSession(CATALOGUE, List.of(), false, new CompoundTag());
        assertNull(session.consume(UUID.randomUUID(), false, List.of("minecraft:speed")));
        assertNull(session.consume(session.token(), false, List.of("missing:effect")));
        assertEquals(List.of("minecraft:speed", "minecraft:poison"),
                session.consume(session.token(), false, List.of("minecraft:poison", "minecraft:speed", "minecraft:speed")));
        assertNull(session.consume(session.token(), false, List.of("minecraft:levitation")));
    }

    @Test
    void reuseUsesServerHistoryAndIgnoresClientSuppliedSelection() {
        SelectionSession session = new SelectionSession(CATALOGUE, List.of("minecraft:poison", "removed:effect"), true, new CompoundTag());
        assertEquals(List.of("minecraft:poison"), session.previous());
        assertEquals(List.of("minecraft:poison"), session.consume(session.token(), true, List.of("minecraft:speed")));
    }

    @Test
    void choosingAgainStartsEmptyAndOnlyNewConfirmationReplacesHistory() {
        SelectionSession session = new SelectionSession(CATALOGUE, List.of("minecraft:poison"), true, new CompoundTag());
        assertEquals(List.of("minecraft:speed"), session.consume(session.token(), false, List.of("minecraft:speed")));
    }

    @Test
    void emptyConfirmationIsValidAndCanBeReused() {
        SelectionSession fresh = new SelectionSession(CATALOGUE, List.of(), false, new CompoundTag());
        assertNull(fresh.consume(fresh.token(), true, List.of()));
        assertEquals(List.of(), fresh.consume(fresh.token(), false, List.of()));
        SelectionSession returning = new SelectionSession(CATALOGUE, List.of(), true, new CompoundTag());
        assertEquals(List.of(), returning.consume(returning.token(), true, List.of()));
    }

    @Test
    void playersAndNewMealsDoNotShareAuthorization() {
        SelectionSession first = new SelectionSession(CATALOGUE, List.of(), false, new CompoundTag());
        SelectionSession second = new SelectionSession(CATALOGUE, List.of(), false, new CompoundTag());
        assertNull(second.consume(first.token(), false, List.of("minecraft:speed")));
        assertEquals(List.of("minecraft:levitation"), second.consume(second.token(), false, List.of("minecraft:levitation")));
        assertEquals(List.of("minecraft:speed"), first.consume(first.token(), false, List.of("minecraft:speed")));
    }
}
