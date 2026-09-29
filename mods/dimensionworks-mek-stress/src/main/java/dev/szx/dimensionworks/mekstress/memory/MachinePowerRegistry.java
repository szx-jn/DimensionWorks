package dev.szx.dimensionworks.mekstress.memory;

import appeng.api.config.Actionable;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.core.MachineTier;
import java.util.HashMap;
import java.util.Map;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;

/** Latest AE route decision for each loaded machine. */
public final class MachinePowerRegistry {
    private static final long TIMEOUT = 3L;
    private static final Map<GlobalPos, AeRoute> ROUTES = new HashMap<>();

    private MachinePowerRegistry() {
    }

    public static AeRoute attach(TileEntityMekanism machine, IMemoryGridService service, BlockPos busPos,
                              int effectiveRpm, MachineTier tier, long gameTick) {
        if (machine.getLevel() == null) {
            return null;
        }
        GlobalPos key = key(machine);
        AeRoute current = ROUTES.get(key);
        if (current != null && busPos.equals(current.busPos)) {
            current = new AeRoute(service, current.busPos, effectiveRpm, tier, gameTick);
            ROUTES.put(key, current);
            return current;
        }
        if (current == null || current.lastSeenTick < gameTick - TIMEOUT || effectiveRpm < current.effectiveRpm) {
            current = new AeRoute(service, busPos.immutable(), effectiveRpm, tier, gameTick);
            ROUTES.put(key, current);
        }
        return current;
    }

    public static AeRoute get(TileEntityMekanism machine, long gameTick) {
        GlobalPos key = key(machine);
        AeRoute route = ROUTES.get(key);
        if (route != null && gameTick - route.lastSeenTick > TIMEOUT) {
            ROUTES.remove(key);
            return null;
        }
        return route;
    }

    public static long consume(TileEntityMekanism machine, long amount, Actionable mode, long gameTick) {
        AeRoute route = get(machine, gameTick);
        if (route == null || route.service == null || route.effectiveRpm <= 0 || amount <= 0L) {
            return 0L;
        }
        double q = route.service.snapshot(gameTick).finalQ();
        if (q <= 0.0D) {
            return 0L;
        }
        long request = Math.max(1L, Math.round(amount));
        if (mode == Actionable.SIMULATE) {
            return route.service.extractSu(request, Actionable.SIMULATE, gameTick);
        }
        long available = route.service.extractSu(request, Actionable.SIMULATE, gameTick);
        if (available < request) {
            return 0L;
        }
        long extracted = route.service.extractSu(request, Actionable.MODULATE, gameTick);
        return extracted == request ? extracted : 0L;
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        long tick = event.getServer().getTickCount();
        ROUTES.entrySet().removeIf(entry -> tick - entry.getValue().lastSeenTick > TIMEOUT * 2L);
    }

    public static void clear(GlobalPos key) {
        ROUTES.remove(key);
    }

    private static GlobalPos key(TileEntityMekanism machine) {
        return GlobalPos.of(machine.getLevel().dimension(), machine.getBlockPos());
    }

    public record AeRoute(IMemoryGridService service, BlockPos busPos, int effectiveRpm, MachineTier tier,
                          long lastSeenTick) {
        public double q(long gameTick) {
            return service == null ? 0.0D : service.snapshot(gameTick).finalQ();
        }
    }
}
