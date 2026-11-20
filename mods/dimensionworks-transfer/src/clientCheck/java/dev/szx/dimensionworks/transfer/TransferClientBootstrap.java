package dev.szx.dimensionworks.transfer;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in fresh world, bounded resources, no interaction with player installations. */
@EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class TransferClientBootstrap {
    private static boolean started;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            client.options.onboardingAccessibilityFinished(); client.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
        }
        if (!started && client.screen instanceof net.minecraft.client.gui.screens.TitleScreen && client.getOverlay() == null) {
            started = true; client.options.renderDistance().set(2); client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(30); client.options.pauseOnLostFocus = false;
            var settings = new net.minecraft.world.level.LevelSettings("DW Transfer Check", net.minecraft.world.level.GameType.CREATIVE, false,
                net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
            client.createWorldOpenFlows().createFreshLevel("dw-transfer-check-" + System.currentTimeMillis(), settings,
                new net.minecraft.world.level.levelgen.WorldOptions(7, false, false),
                access -> access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                    .getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).createWorldDimensions(), client.screen);
        }
    }
}
