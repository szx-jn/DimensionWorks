package dev.szx.dimensionworks.cavefactory.client;

import dev.szx.dimensionworks.cavefactory.twilight.TwilightWorkstationMenu;
import dev.szx.dimensionworks.cavefactory.twilight.TwilightWorkstationBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;

/** Drawn UI avoids an extra GUI texture; slots and visible reasons use normal Minecraft rendering. */
public final class TwilightWorkstationScreen extends AbstractContainerScreen<TwilightWorkstationMenu> {
    public TwilightWorkstationScreen(TwilightWorkstationMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176; imageHeight = 224;
        inventoryLabelY = 130;
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF283C35);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFFD6DEC8);
        for (var slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF677B68);
            g.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFFB8C5B0);
        }
        g.fill(leftPos + 85, topPos + 45, leftPos + 115, topPos + 54, 0xFF687969);
        int width = menu.duration() == 0 ? 0 : Math.min(30, 30 * menu.progress() / menu.duration());
        g.fill(leftPos + 85, topPos + 45, leftPos + 85 + width, topPos + 54, 0xFFAE8549);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        text(g, "leaves", 24, 29);
        text(g, menu.machine.biological() ? "feed" : "humus", 54, 29);
        g.drawString(font, Component.translatable("gui.dimensionworks_cave_factory.twilight.water", menu.value(1)), 8, 69, 0x263B32, false);
        g.drawString(font, Component.translatable(menu.machine.biological()
            ? "gui.dimensionworks_cave_factory.twilight.boars" : "gui.dimensionworks_cave_factory.twilight.extract",
            menu.value(menu.machine.biological() ? 7 : 2)), 8, 81, 0x263B32, false);
        g.drawString(font, Component.translatable("gui.dimensionworks_cave_factory.twilight.rpm", menu.value(8)), 8, 93, 0x263B32, false);
        int status = Math.max(0, Math.min(TwilightWorkstationBlockEntity.Status.values().length - 1, menu.value(0)));
        Component reason = Component.translatable("gui.dimensionworks_cave_factory.twilight.status."
            + TwilightWorkstationBlockEntity.Status.values()[status].name().toLowerCase(Locale.ROOT));
        g.drawWordWrap(font, reason, 8, 106, 160, 0x263B32);
    }
    private void text(GuiGraphics g, String key, int x, int y) {
        g.drawString(font, Component.translatable("gui.dimensionworks_cave_factory.twilight." + key), x, y, 0x263B32, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }
}
