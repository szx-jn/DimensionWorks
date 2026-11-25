package dev.szx.dimensionworks.transfer.elastic;

import dev.szx.dimensionworks.transfer.elastic.ability.BlockPackingAbility;
import dev.szx.dimensionworks.transfer.elastic.ability.ElasticParcelAbility;
import dev.szx.dimensionworks.transfer.elastic.ability.EntityPackingAbility;
import dev.szx.dimensionworks.transfer.elastic.ability.PlayerParcelAbility;
import dev.szx.dimensionworks.transfer.elastic.ability.SableShipPackingAbility;
import dev.szx.dimensionworks.transfer.elastic.ability.SpecialBlockCompatibilityAbility;

public final class PackagerProfiles {
    public static final PackagerProfile WHITE = PackagerProfile.of(PlayerParcelAbility.INSTANCE);
    public static final PackagerProfile GREEN = WHITE.with(ElasticParcelAbility.INSTANCE, EntityPackingAbility.INSTANCE);
    public static final PackagerProfile BLUE = GREEN.with(BlockPackingAbility.INSTANCE);
    public static final PackagerProfile PURPLE = BLUE.with(
        SableShipPackingAbility.INSTANCE, SpecialBlockCompatibilityAbility.INSTANCE);

    private PackagerProfiles() {}
}
