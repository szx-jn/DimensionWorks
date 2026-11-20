package dev.szx.dimensionworks.cavefactory.client;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.registry.CFMenus;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

@EventBusSubscriber(
    modid = DimensionWorksCaveFactory.MOD_ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class CaveFactoryClientEvents {
    static final Map<String, Integer> MOLTEN_COLORS = Map.ofEntries(
        Map.entry("iron", 0xD77A42), Map.entry("copper", 0xE07845), Map.entry("gold", 0xFFD34D),
        Map.entry("netherite", 0x51454A), Map.entry("zinc", 0xB7C6C7), Map.entry("brass", 0xE5BE48),
        Map.entry("tin", 0xC7D8DE), Map.entry("lead", 0x777F91), Map.entry("osmium", 0xA6D9D1),
        Map.entry("uranium", 0x8DC65A), Map.entry("bronze", 0xB97943), Map.entry("steel", 0xAAB3BF),
        Map.entry("refined_obsidian", 0x51446D), Map.entry("refined_glowstone", 0xF6D66B),
        Map.entry("scarlet_neodymium", 0xC94B55), Map.entry("azure_neodymium", 0x4B83D1),
        Map.entry("totemic_gold", 0xE9C85A), Map.entry("poseidite", 0x4C9CC4),
        Map.entry("shulkerate", 0xA47CC2), Map.entry("sculkium", 0x397B78),
        Map.entry("eternium", 0xE6E1D0)
    );

    private CaveFactoryClientEvents() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new CaveFactoryPonderPlugin()));
    }    @SubscribeEvent
    public static void registerFluidExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        for (var machine : dev.szx.dimensionworks.cavefactory.logic.MachineType.values()) {
            event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return ResourceLocation.withDefaultNamespace("block/water_still"); }
                @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.withDefaultNamespace("block/water_flow"); }
                @Override public int getTintColor() { return dev.szx.dimensionworks.cavefactory.registry.CFFluids.colorFor(machine); }
            }, dev.szx.dimensionworks.cavefactory.registry.CFFluids.get(machine).type().get());
        }
    }
    @SubscribeEvent public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(CFMenus.factoryMachine(), FactoryMachineScreen::new);
    }

}
