package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/** Test-only stand-in for the existing KubeJS item; never included in production jars. */
@Mod.EventBusSubscriber(modid=DimensionWorksRpmLimit.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public class GearHeartFixtureItems {
    @SubscribeEvent public static void items(RegisterEvent event) {
        event.register(Registries.ITEM, h -> {
            h.register(new ResourceLocation("kubejs", "gear_heart"), new Item(new Item.Properties()));
            h.register(new ResourceLocation("kubejs", "gear_heart_enchanted"), new Item(new Item.Properties()));
        });
    }
}
