package dev.szx.dimensionworks.transfer.territory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Pack addons, including AppMek whose AE2 dependency is optional in its metadata. */
public final class AeBlockNamespaces {
    public static final Set<String> PACK_ADDONS = Set.of("ae2", "ae2ct", "ae2omnicells", "ae2wtlib",
        "appliedcreate", "appmek", "expatternprovider", "megacells", "meplacementtool", "neoecoae");

    private AeBlockNamespaces() {}

    public static Set<String> resolve(Map<String, Set<String>> requiredDependencies) {
        Set<String> result = new HashSet<>(PACK_ADDONS);
        boolean changed;
        do {
            changed = false;
            for (var entry : requiredDependencies.entrySet()) {
                String id = entry.getKey();
                // Pack integration modules depend on AE2 without being AE2 addons.
                if (id.equals("dimensionworks") || id.startsWith("dimensionworks_") || id.equals("kubejs")) continue;
                if (entry.getValue().stream().anyMatch(result::contains)) changed |= result.add(id);
            }
        } while (changed);
        return Set.copyOf(result);
    }
}
