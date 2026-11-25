package dev.szx.dimensionworks.transfer.elastic;

import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager;
import de.theidler.create_mobile_packages.items.mobile_packager.PackagerCapabilityProvider;
import de.theidler.create_mobile_packages.items.mobile_packager.ability.PackagerCapability;
import java.util.Set;

public final class TieredPackager extends MobilePackager implements PackagerCapabilityProvider {
    private final PackagerProfile profile;

    public TieredPackager(Properties properties, PackagerProfile profile) {
        super(properties);
        this.profile = profile;
    }

    @Override
    public Set<PackagerCapability> getPackagerCapabilities() {
        return profile.capabilities();
    }
}
