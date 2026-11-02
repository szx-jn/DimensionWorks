package dev.szx.dimensionworks.jeiticks.client;

import com.mojang.datafixers.util.Either;
import dev.szx.dimensionworks.jeiticks.DimensionWorksJeiTicks;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = DimensionWorksJeiTicks.MOD_ID, value = Dist.CLIENT)
public final class FuelTooltipEvents {
    private static ResourceLocation hovered;
    private static long lastSeen;
    private static int page;
    private static int pages;
    private static double mouseX;
    private static double mouseY;

    private FuelTooltipEvents() {}

    @SubscribeEvent
    public static void gather(RenderTooltipEvent.GatherComponents event) {
        var entries = FuelCatalog.entries(event.getItemStack());
        if (entries.isEmpty()) {
            hovered = null;
            return;
        }
        var id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (!id.equals(hovered)) {
            page = 0;
        }
        hovered = id;
        lastSeen = Util.getMillis();
        var mouse = Minecraft.getInstance().mouseHandler;
        mouseX = mouse.xpos();
        mouseY = mouse.ypos();
        int pageSize = Math.max(1, Math.min(4, (event.getScreenHeight() - 100) / 85));
        pages = (entries.size() + pageSize - 1) / pageSize;
        page = Math.floorMod(page, pages);
        int from = page * pageSize;
        var visible = entries.subList(from, Math.min(entries.size(), from + pageSize));
        int width = Math.max(60, Math.min(260, event.getScreenWidth() - 32));
        event.getTooltipElements().add(Either.right(new FuelTooltipData(visible, page, pages, width)));
    }

    @SubscribeEvent
    public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        var mouse = Minecraft.getInstance().mouseHandler;
        if (Screen.hasShiftDown() && hovered != null && pages > 1 && Util.getMillis() - lastSeen < 250
                && Math.abs(mouse.xpos() - mouseX) < 2 && Math.abs(mouse.ypos() - mouseY) < 2) {
            page = Math.floorMod(page + (event.getScrollDelta() < 0 ? 1 : -1), pages);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void recipes(RecipesUpdatedEvent event) {
        FuelCatalog.invalidate();
    }

    @SubscribeEvent
    public static void tags(TagsUpdatedEvent event) {
        FuelCatalog.invalidate();
    }

    @Mod.EventBusSubscriber(modid = DimensionWorksJeiTicks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void factories(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(FuelTooltipData.class, FuelTooltipComponent::new);
        }

        @SubscribeEvent
        public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> FuelCatalog.invalidate());
        }
    }
}
