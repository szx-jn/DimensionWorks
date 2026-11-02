package dev.szx.dimensionworks.jeiticks.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;

@WailaPlugin
public final class JadeFuelPlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new Provider(), Block.class);
    }

    private static final class Provider implements IBlockComponentProvider {
        private static final ResourceLocation UID = new ResourceLocation("dimensionworks_jei_ticks", "fuel_consumption");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            var entries = FuelCatalog.entries(new ItemStack(accessor.getBlock()));
            if (entries.isEmpty()) {
                return;
            }
            int limit = Math.min(entries.size(), 2);
            tooltip.add(new FuelElement(entries.subList(0, limit)));
            if (entries.size() > limit) {
                tooltip.add(Component.translatable("tooltip.dimensionworks_jei_ticks.fuel_inventory", entries.size()));
            }
        }
    }

    private static final class FuelElement extends Element {
        private final FuelTooltipComponent content;

        private FuelElement(List<FuelEntry> entries) {
            content = new FuelTooltipComponent(new FuelTooltipData(entries, 0, 1, 240));
        }

        @Override
        public Vec2 getSize() {
            return new Vec2(content.getWidth(Minecraft.getInstance().font), content.getHeight());
        }

        @Override
        public void render(GuiGraphics graphics, float x, float y, float width, float height) {
            content.renderImage(Minecraft.getInstance().font, (int) x, (int) y, graphics);
        }
    }
}
