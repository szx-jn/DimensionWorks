package dev.szx.dimensionworks.mekstress.memory;

import dev.szx.dimensionworks.mekstress.core.SuConversion;
import java.util.HashMap;
import java.util.Map;

/** Sums each eligible machine's FE request for one server tick before converting it to SU. */
public final class ProcessingEnergyRegistry {
    private static final Map<Object, Request> REQUESTS = new HashMap<>();

    private ProcessingEnergyRegistry() {
    }

    public static synchronized <K> void record(K machine, long gameTick, long fe) {
        if (machine == null || fe <= 0L) {
            return;
        }
        Request current = REQUESTS.get(machine);
        long total = current != null && current.gameTick == gameTick
            ? saturatedAdd(current.fe, fe)
            : fe;
        REQUESTS.put(machine, new Request(gameTick, total));
    }

    public static synchronized <K> long convertedSu(K machine, long gameTick, double fePerSu) {
        Request request = REQUESTS.get(machine);
        if (request == null || request.gameTick != gameTick) {
            return 0L;
        }
        return SuConversion.ceilFromFe(request.fe, fePerSu);
    }

    public static synchronized <K> void clear(K machine) {
        REQUESTS.remove(machine);
    }

    private static long saturatedAdd(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    private record Request(long gameTick, long fe) {
    }
}
