package dev.szx.dimensionworks.jeiticks.client;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Fuel UI scope is deliberately independent of JEI's recipe-duration coverage. */
public final class FuelScope {
    private FuelScope() {}

    public static boolean includes(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        String namespace = id.getNamespace();
        if (namespace.equals("create") || namespace.equals("draconicevolution")) {
            return true;
        }
        // Require an actual Create dependency, rather than matching arbitrary item names.
        return ModList.get().getModContainerById(namespace)
            .map(container -> container.getModInfo().getDependencies().stream()
                .anyMatch(dependency -> dependency.getModId().equals("create")))
            .orElse(false);
    }
}
