package dev.szx.dimensionworks.transfer.elastic.ability;

import de.theidler.create_mobile_packages.items.mobile_packager.ability.PackagerCapability;

public enum EntityPackingAbility implements PackagerAbility {
    INSTANCE;

    @Override public PackagerCapability capability() { return PackagerCapability.ENTITY_PACKING; }
}
