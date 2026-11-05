package dev.szx.dimensionworks.mekstress.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Opt-in real client model bake check, including every placed block state. */
@Mod.EventBusSubscriber(modid = "dimensionworks_mek_stress", value = Dist.CLIENT)
public final class TransportModelCheck {
    private static boolean checked;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (checked || !Boolean.getBoolean("dimensionworks.verifyTransportModels")
            || event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        if (!(client.screen instanceof TitleScreen) || client.getOverlay() != null) return;
        checked = true;
        int states = 0;
        for (var entry : ForgeRegistries.BLOCKS.getEntries()) {
            var id = entry.getKey().location();
            if (!id.getNamespace().equals("dimensionworks_mek_stress")
                || !(id.getPath().startsWith("cross_dimension_transmitter_") || id.getPath().equals("matter_ball_unpacker"))) continue;
            for (var state : entry.getValue().getStateDefinition().getPossibleStates()) {
                var model = client.getBlockRenderer().getBlockModel(state);
                if (model == client.getModelManager().getMissingModel()
                    || model.getParticleIcon().contents().name().getPath().equals("missingno")) {
                    System.err.println("TRANSPORT MODEL CHECK FAILED: " + state);
                    System.exit(1);
                }
                states++;
            }
        }
        if (states != 7) throw new AssertionError("Expected seven transport block states, got " + states);
        System.out.println("TRANSPORT MODEL CHECK PASSED: all seven placed models baked with textures");
        client.stop();
    }
}
