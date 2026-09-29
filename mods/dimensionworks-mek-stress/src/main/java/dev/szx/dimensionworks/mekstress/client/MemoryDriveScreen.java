package dev.szx.dimensionworks.mekstress.client;

import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import dev.szx.dimensionworks.mekstress.memory.MemoryDriveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MemoryDriveScreen extends AbstractContainerScreen<MemoryDriveMenu> {
    private int localPage;

    public MemoryDriveScreen(MemoryDriveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 196;
        imageHeight = 168;
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
            .bounds(leftPos + 16, topPos + 5, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
            .bounds(leftPos + 160, topPos + 5, 20, 16).build());
    }

    private void changePage(int delta) {
        int page = Math.max(0, Math.min(MemoryDriveMenu.MAX_PAGE, localPage + delta));
        if (page == localPage) {
            return;
        }
        localPage = page;
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0202020);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xE0383838);
        for (int slot = 0; slot < MemoryDriveMenu.SLOTS_PER_PAGE; slot++) {
            int x = leftPos + 16 + (slot % 5) * 22;
            int y = topPos + 26 + (slot / 5) * 22;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF101010);
        }
        graphics.fill(leftPos + 8, topPos + 75, leftPos + imageWidth - 8, topPos + 76, 0xFF707070);
        graphics.fill(leftPos + 8, topPos + 133, leftPos + imageWidth - 8, topPos + 134, 0xFF707070);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.page", localPage + 1, 7),
            62, 10, 0xCCCCCC, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.drives", menu.driveCount()),
            8, 52, 0xAAAAAA, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.stored",
                menu.networkStoredSu(), menu.networkCapacitySu()), 8, 62, 0xFFD166, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.bandwidth",
                menu.networkBandwidthRpm()), 8, 72, 0x7EC8E3, false);
        if (menu.status() != MemoryNetworkStatus.OK) {
            graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.status."
                    + menu.status().name().toLowerCase()), 8, 145, 0xFF5555, false);
        }
    }
}
