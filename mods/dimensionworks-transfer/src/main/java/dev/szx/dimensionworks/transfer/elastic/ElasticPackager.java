package dev.szx.dimensionworks.transfer.elastic;

import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.ForgeRegistries;

/** All block, mob, inventory and menu handling remains in CMP's existing tool. */
public final class ElasticPackager extends MobilePackager {
    public ElasticPackager(Properties properties) { super(properties); }

    /** KubeJS crafting callback: preserve the complete tool payload, including ForgeCaps. */
    public static ItemStack upgrade(ItemStack original) {
        CompoundTag saved = original.save(new CompoundTag());
        saved.putString("id", ForgeRegistries.ITEMS.getKey(ElasticItems.PACKAGER.get()).toString());
        saved.putByte("Count", (byte) 1);
        return ItemStack.of(saved);
    }

}
