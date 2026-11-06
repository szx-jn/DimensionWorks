package dev.szx.dimensionworks.transfer.territory;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AeBlockNamespacesTest {
    @Test
    void includesPackAddonsWithOptionalAeMetadata() {
        var resolved = AeBlockNamespaces.resolve(Map.of());
        assertTrue(resolved.containsAll(Set.of("ae2", "appmek", "neoecoae", "megacells", "appliedcreate", "expatternprovider")));
        assertFalse(resolved.contains("mekanism"));
        assertFalse(resolved.contains("create"));
    }

    @Test
    void followsRequiredAddonDependenciesTransitively() {
        var resolved = AeBlockNamespaces.resolve(Map.of("addon", Set.of("ae2"),
            "addon_expansion", Set.of("addon"), "general_mod", Set.of("forge", "minecraft")));
        assertTrue(resolved.containsAll(Set.of("addon", "addon_expansion")));
        assertFalse(resolved.contains("general_mod"));
        assertFalse(resolved.contains("forge"));
    }

    @Test
    void excludesPackIntegrationModulesAndTheirDependents() {
        var resolved = AeBlockNamespaces.resolve(Map.of("dimensionworks_rpm_limit", Set.of("ae2"),
            "dimensionworks_mek_stress", Set.of("appliedcreate"), "dimensionworks_cave_factory", Set.of("dimensionworks_rpm_limit"),
            "kubejs", Set.of("ae2")));
        assertFalse(resolved.contains("dimensionworks_rpm_limit"));
        assertFalse(resolved.contains("dimensionworks_mek_stress"));
        assertFalse(resolved.contains("dimensionworks_cave_factory"));
        assertFalse(resolved.contains("kubejs"));
    }
}
