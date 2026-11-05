package dev.szx.dimensionworks.mekstress.memory;

import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraftforge.fml.ModList;

/** FTB Teams is a pack dependency; reflection keeps the standalone mod's API linkage optional. */
public final class TeamAccess {
    private TeamAccess() {}

    public static boolean sameCurrentTeam(UUID first, UUID second) {
        if (first.equals(second)) return true;
        if (!ModList.get().isLoaded("ftbteams")) return false;
        try {
            Class<?> api = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object instance = api.getMethod("api").invoke(null);
            Object manager = instance.getClass().getMethod("getManager").invoke(instance);
            Method sameTeam = manager.getClass().getMethod("arePlayersInSameTeam", UUID.class, UUID.class);
            return Boolean.TRUE.equals(sameTeam.invoke(manager, first, second));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }
}
