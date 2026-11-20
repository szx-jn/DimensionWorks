package dev.szx.dimensionworks.mekstress.client;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber(modid = DimensionWorksMekStress.MOD_ID, value = Dist.CLIENT)
public final class MemoryDriveClientEvents {
    private MemoryDriveClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(RegisterMenuScreensEvent event) {
        event.register(DimensionWorksMekStress.CLOUD_DISK_MENU.get(), CloudDiskScreen::new);
        event.register(DimensionWorksMekStress.MEMORY_DRIVE_MENU.get(), MemoryDriveScreen::new);
        event.register(DimensionWorksMekStress.CROSS_DIMENSION_TRANSPORT_MENU.get(), CrossDimensionTransportScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            DimensionWorksMekStress.MEMORY_DRIVE_BLOCK_ENTITY.get(), MemoryDriveLedRenderer::new);
    }
}
