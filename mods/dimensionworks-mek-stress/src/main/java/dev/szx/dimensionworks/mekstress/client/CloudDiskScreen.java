package dev.szx.dimensionworks.mekstress.client;

import dev.szx.dimensionworks.mekstress.memory.CloudDiskMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CloudDiskScreen extends AbstractContainerScreen<CloudDiskMenu> {
    private Button previous;
    private Button next;

    public CloudDiskScreen(CloudDiskMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 208;
        inventoryLabelY = 114;
    }

    @Override protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(0))
            .bounds(leftPos + 8, topPos + 94, 26, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(1))
            .bounds(leftPos + 142, topPos + 94, 26, 16).build());
    }

    private void turn(int direction) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, direction);
    }

    @Override protected void containerTick() {
        super.containerTick();
        previous.active = menu.page() > 0;
        next.active = menu.page() + 1 < menu.pages();
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF172A36);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFDAE3E8);
        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF526471);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFFB6C4CC);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.cloud_hint"), 8, 23, 0x33434D, false);
        graphics.drawCenteredString(font, Component.translatable("gui.dimensionworks_mek_stress.cloud_page",
            menu.page() + 1, menu.pages()), imageWidth / 2, 98, 0x33434D);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (CloudDiskKeyEvents.OPEN_CLOUD.matches(key, scanCode)) { onClose(); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
