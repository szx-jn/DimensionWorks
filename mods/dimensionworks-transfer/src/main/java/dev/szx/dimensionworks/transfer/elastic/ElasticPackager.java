package dev.szx.dimensionworks.transfer.elastic;

import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;

/** All block, mob, inventory and menu handling remains in CMP's existing tool. */
public class ElasticPackager extends MobilePackager {
    public ElasticPackager(Properties properties) { super(properties); }

    @Override public boolean canPackEntities() { return true; }

    /** KubeJS crafting callback: preserve the complete tool payload, including every native data component. */
    public static ItemStack upgrade(ItemStack original) {
        return original.transmuteCopy(PackagerItems.PACKAGER.get(), 1);
    }

}
