package dev.szx.dimensionworks.mekstress.memory;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.GlobalPos;
import org.jetbrains.annotations.Nullable;

/** Extra Create stress contributed by machines overclocked from a direct kinetic source. */
public final class DirectStressRegistry {
    private static final Map<KineticBlockEntity, Map<GlobalPos, Float>> CONTRIBUTIONS = new WeakHashMap<>();
    private static final Map<GlobalPos, KineticBlockEntity> ASSIGNMENTS = new HashMap<>();

    private DirectStressRegistry() {
    }

    public static synchronized Change contribute(KineticBlockEntity source, GlobalPos machine, float stress) {
        return assign(machine, source, stress);
    }

    public static synchronized Change assign(GlobalPos machine, KineticBlockEntity source, float stress) {
        KineticBlockEntity previous = ASSIGNMENTS.get(machine);
        if (previous == source && stress > 0.0F) {
            Map<GlobalPos, Float> current = CONTRIBUTIONS.get(source);
            Float oldStress = current == null ? null : current.get(machine);
            if (oldStress != null && Float.compare(oldStress, stress) == 0) {
                return new Change(null, null, false);
            }
            if (current != null) {
                current.put(machine, stress);
                return new Change(source, source, true);
            }
        }

        previous = ASSIGNMENTS.remove(machine);
        boolean changed = removeContribution(previous, machine);
        if (stress <= 0.0F) {
            return new Change(previous, null, changed);
        }

        Map<GlobalPos, Float> values = CONTRIBUTIONS.computeIfAbsent(source, ignored -> new HashMap<>());
        Float oldStress = values.put(machine, stress);
        ASSIGNMENTS.put(machine, source);
        boolean stressChanged = oldStress == null || Float.compare(oldStress, stress) != 0;
        return new Change(previous, source, changed || stressChanged || previous != source);
    }

    public static synchronized float extraStress(KineticBlockEntity source) {
        Map<GlobalPos, Float> values = CONTRIBUTIONS.get(source);
        if (values == null || values.isEmpty()) {
            return 0.0F;
        }
        float total = 0.0F;
        for (float value : values.values()) {
            total += value;
        }
        return total;
    }

    public static synchronized Change clear(GlobalPos machine) {
        KineticBlockEntity source = ASSIGNMENTS.remove(machine);
        boolean changed = removeContribution(source, machine);
        return new Change(source, null, changed);
    }

    private static boolean removeContribution(@Nullable KineticBlockEntity source, GlobalPos machine) {
        if (source == null) {
            return false;
        }
        Map<GlobalPos, Float> values = CONTRIBUTIONS.get(source);
        if (values == null || values.remove(machine) == null) {
            return false;
        }
        if (values.isEmpty()) {
            CONTRIBUTIONS.remove(source);
        }
        return true;
    }

    public record Change(@Nullable KineticBlockEntity previous, @Nullable KineticBlockEntity current, boolean changed) {
    }
}
