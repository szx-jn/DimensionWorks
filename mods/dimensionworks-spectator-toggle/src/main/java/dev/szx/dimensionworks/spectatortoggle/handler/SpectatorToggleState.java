package dev.szx.dimensionworks.spectatortoggle.handler;

import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.GameType;

/** Resets the saved return point and spectator mode as one successful operation. */
final class SpectatorToggleState {
    static final String RETURN_POINT_KEY = "dimensionworks_spectator_toggle";

    private SpectatorToggleState() {
    }

    static boolean isExpired(CompoundTag persistentData, UUID sessionId) {
        if (!persistentData.contains(RETURN_POINT_KEY)) {
            return false;
        }
        CompoundTag returnPoint = persistentData.getCompound(RETURN_POINT_KEY);
        return !returnPoint.hasUUID("session") || !sessionId.equals(returnPoint.getUUID("session"));
    }

    static boolean reset(
            CompoundTag persistentData,
            GameType currentGameMode,
            GameType previousGameMode,
            Predicate<GameType> setGameMode) {
        if (currentGameMode == GameType.SPECTATOR) {
            CompoundTag returnPoint = persistentData.getCompound(RETURN_POINT_KEY);
            GameType restoreMode = returnPoint.contains("game_mode", Tag.TAG_INT)
                    ? GameType.byId(returnPoint.getInt("game_mode")) : previousGameMode;
            if (restoreMode == null || restoreMode == GameType.SPECTATOR) {
                restoreMode = previousGameMode;
            }
            if (restoreMode == null || restoreMode == GameType.SPECTATOR) {
                restoreMode = GameType.SURVIVAL;
            }
            if (!setGameMode.test(restoreMode)) {
                return false;
            }
        }
        persistentData.remove(RETURN_POINT_KEY);
        return true;
    }
}
