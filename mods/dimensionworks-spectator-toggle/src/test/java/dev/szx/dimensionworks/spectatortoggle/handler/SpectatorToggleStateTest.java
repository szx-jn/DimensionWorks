package dev.szx.dimensionworks.spectatortoggle.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SpectatorToggleStateTest {
    @ParameterizedTest
    @EnumSource(value = GameType.class, names = {"SURVIVAL", "CREATIVE", "ADVENTURE"})
    void shutdownClearsBothTheReturnPointAndSpectatorMode(GameType savedMode) {
        CompoundTag persistentData = savedState(savedMode, UUID.randomUUID());
        persistentData.putString("other_mod", "kept");
        AtomicReference<GameType> mode = new AtomicReference<>(GameType.SPECTATOR);

        assertTrue(SpectatorToggleState.reset(persistentData, mode.get(), GameType.SURVIVAL, restored -> {
            mode.set(restored);
            return true;
        }));

        assertEquals(savedMode, mode.get());
        assertFalse(persistentData.contains(SpectatorToggleState.RETURN_POINT_KEY));
        assertEquals("kept", persistentData.getString("other_mod"));
    }

    @Test
    void aRejectedModeChangeKeepsTheCompleteReturnPointForRetry() {
        CompoundTag persistentData = savedState(GameType.CREATIVE, UUID.randomUUID());
        CompoundTag original = persistentData.copy();

        assertFalse(SpectatorToggleState.reset(
                persistentData, GameType.SPECTATOR, GameType.SURVIVAL, restored -> false));
        assertEquals(original, persistentData);

        assertTrue(SpectatorToggleState.reset(
                persistentData, GameType.SPECTATOR, GameType.SURVIVAL, restored -> true));
        assertFalse(persistentData.contains(SpectatorToggleState.RETURN_POINT_KEY));
    }

    @Test
    void offlineStateExpiresAfterRestartButSurvivesReconnectDuringTheSameSession() {
        UUID oldSession = UUID.randomUUID();
        CompoundTag reloadedData = savedState(GameType.ADVENTURE, oldSession).copy();

        assertFalse(SpectatorToggleState.isExpired(reloadedData, oldSession));
        assertTrue(SpectatorToggleState.isExpired(reloadedData, UUID.randomUUID()));
    }

    @Test
    void legacyRecordsWithoutASessionAreExpired() {
        CompoundTag persistentData = savedState(GameType.SURVIVAL, UUID.randomUUID());
        persistentData.getCompound(SpectatorToggleState.RETURN_POINT_KEY).remove("session");

        assertTrue(SpectatorToggleState.isExpired(persistentData, UUID.randomUUID()));
        assertFalse(SpectatorToggleState.isExpired(new CompoundTag(), UUID.randomUUID()));
    }

    @Test
    void aLostReturnPointCanExitSpectatorUsingTheVanillaPreviousMode() {
        CompoundTag persistentData = new CompoundTag();
        AtomicReference<GameType> mode = new AtomicReference<>(GameType.SPECTATOR);

        assertTrue(SpectatorToggleState.reset(persistentData, mode.get(), GameType.CREATIVE, restored -> {
            mode.set(restored);
            return true;
        }));

        assertEquals(GameType.CREATIVE, mode.get());
        assertFalse(persistentData.contains(SpectatorToggleState.RETURN_POINT_KEY));
    }

    @Test
    void missingOrSpectatorPreviousModeFallsBackToSurvival() {
        for (GameType previousMode : new GameType[] {null, GameType.SPECTATOR}) {
            AtomicReference<GameType> mode = new AtomicReference<>(GameType.SPECTATOR);
            assertTrue(SpectatorToggleState.reset(new CompoundTag(), mode.get(), previousMode, restored -> {
                mode.set(restored);
                return true;
            }));
            assertEquals(GameType.SURVIVAL, mode.get());
        }
    }

    @Test
    void missingDimensionAndCoordinatesDoNotPreventCleanup() {
        CompoundTag persistentData = savedState(GameType.ADVENTURE, UUID.randomUUID());
        CompoundTag returnPoint = persistentData.getCompound(SpectatorToggleState.RETURN_POINT_KEY);
        returnPoint.remove("dimension");
        returnPoint.remove("x");
        AtomicReference<GameType> mode = new AtomicReference<>(GameType.SPECTATOR);

        assertTrue(SpectatorToggleState.reset(persistentData, mode.get(), GameType.SURVIVAL, restored -> {
            mode.set(restored);
            return true;
        }));
        assertEquals(GameType.ADVENTURE, mode.get());
        assertFalse(persistentData.contains(SpectatorToggleState.RETURN_POINT_KEY));
    }

    @Test
    void anExternalGameModeChangeIsPreservedWhenClearingF4State() {
        CompoundTag persistentData = savedState(GameType.SURVIVAL, UUID.randomUUID());
        assertTrue(SpectatorToggleState.reset(persistentData, GameType.CREATIVE, GameType.SPECTATOR, restored -> {
            throw new AssertionError("A non-spectator player's game mode must not be changed");
        }));
        assertFalse(persistentData.contains(SpectatorToggleState.RETURN_POINT_KEY));
    }

    private static CompoundTag savedState(GameType gameMode, UUID session) {
        CompoundTag returnPoint = new CompoundTag();
        returnPoint.putBoolean("active", true);
        returnPoint.putUUID("session", session);
        returnPoint.putInt("game_mode", gameMode.getId());
        returnPoint.putString("dimension", "minecraft:overworld");
        returnPoint.putDouble("x", 100);
        CompoundTag persistentData = new CompoundTag();
        persistentData.put(SpectatorToggleState.RETURN_POINT_KEY, returnPoint);
        return persistentData;
    }
}
