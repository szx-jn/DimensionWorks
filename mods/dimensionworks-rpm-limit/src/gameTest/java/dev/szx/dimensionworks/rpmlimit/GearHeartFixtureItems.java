package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Test-only stand-in for the existing KubeJS item; never included in production jars. */
@EventBusSubscriber(modid=DimensionWorksRpmLimit.MOD_ID)
public class GearHeartFixtureItems {
    @SubscribeEvent public static void items(RegisterEvent event) {
        event.register(Registries.ITEM, h -> {
            h.register(ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart"), new Item(new Item.Properties()));
            h.register(ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_enchanted"), new Item(new Item.Properties()));
        });
    }
}
