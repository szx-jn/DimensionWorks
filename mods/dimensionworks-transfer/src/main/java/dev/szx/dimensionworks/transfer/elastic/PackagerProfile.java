package dev.szx.dimensionworks.transfer.elastic;

import de.theidler.create_mobile_packages.items.mobile_packager.ability.PackagerCapability;
import dev.szx.dimensionworks.transfer.elastic.ability.PackagerAbility;
import java.util.EnumSet;
import java.util.Set;

public record PackagerProfile(Set<PackagerCapability> capabilities) {
    public PackagerProfile {
        capabilities = Set.copyOf(capabilities);
    }

    public static PackagerProfile of(PackagerAbility... abilities) {
        EnumSet<PackagerCapability> capabilities = EnumSet.noneOf(PackagerCapability.class);
        for (PackagerAbility ability : abilities) capabilities.add(ability.capability());
        return new PackagerProfile(capabilities);
    }

    public PackagerProfile with(PackagerAbility... abilities) {
        EnumSet<PackagerCapability> combined = EnumSet.copyOf(capabilities);
        for (PackagerAbility ability : abilities) combined.add(ability.capability());
        return new PackagerProfile(combined);
    }
}
