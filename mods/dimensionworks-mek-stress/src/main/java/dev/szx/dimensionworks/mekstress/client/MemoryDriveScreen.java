package dev.szx.dimensionworks.mekstress.client;

import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import dev.szx.dimensionworks.mekstress.core.MemoryDrivePaging;
import dev.szx.dimensionworks.mekstress.memory.MemoryDriveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class MemoryDriveScreen extends AbstractContainerScreen<MemoryDriveMenu> {
    private static final ResourceLocation DRIVE_TEXTURE =
        new ResourceLocation("dimensionworks_mek_stress", "textures/gui/memory_drive.png");
    private static final int STORAGE_X = 8;
    private static final int STORAGE_Y = 26;
    private int localPage;
    private Button previousButton;
    private Button nextButton;

    public MemoryDriveScreen(MemoryDriveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 215;
        inventoryLabelX = 8;
        inventoryLabelY = 122;
    }

    @Override
    protected void init() {
        super.init();
        previousButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
            .bounds(leftPos + 140, topPos + 4, 16, 16).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
            .bounds(leftPos + 158, topPos + 4, 16, 16).build());
        updatePageButtons();
    }

    private void changePage(int delta) {
        int page = Math.max(0, Math.min(menu.pageCount() - 1, localPage + delta));
        if (page == localPage) {
            return;
        }
        localPage = page;
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
        }
        updatePageButtons();
    }

    private void updatePageButtons() {
        previousButton.active = localPage > 0;
        nextButton.active = localPage + 1 < menu.pageCount();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(DRIVE_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int slot = 0; slot < MemoryDrivePaging.SLOTS_PER_PAGE; slot++) {
            if (!menu.isPageSlotActive(slot)) {
                int x = leftPos + STORAGE_X + MemoryDrivePaging.slotX(slot);
                int y = topPos + STORAGE_Y + MemoryDrivePaging.slotY(slot);
                graphics.fill(x, y, x + 16, y + 16, 0x80606060);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSmallString(graphics, title, 8, 5, 0xFFFFFF);
        drawSmallString(graphics, Component.translatable("gui.dimensionworks_mek_stress.page",
            localPage + 1, menu.pageCount()), 116, 8, 0xCCCCCC);
        drawMetric(graphics, "metric.drives", Component.literal(menu.driveCount() + "/2"), 90, 28, 90, 38, 0xFFFFFF);
        drawMetric(graphics, "metric.cards", Component.literal(Integer.toString(menu.cardCount())),
            132, 28, 132, 38, 0xFFFFFF);
        drawMetric(graphics, "metric.stored", Component.literal(compact(menu.networkStoredSu()) + "/"
            + compact(menu.networkCapacitySu())), 90, 53, 90, 63, 0xFFD166);
        drawMetric(graphics, "metric.bandwidth", Component.literal(compact(menu.networkBandwidthRpm())),
            132, 53, 132, 63, 0x7EC8E3);
        drawMetric(graphics, "metric.tier", Component.literal("DDR" + menu.drive().tier().index()),
            90, 78, 90, 88, 0xFFFFFF);
        drawMetric(graphics, "metric.slots", Component.literal(Integer.toString(menu.drive().tier().slotsPerDrive())),
            132, 78, 132, 88, 0xFFFFFF);
        int statusColor = menu.status() == MemoryNetworkStatus.OK ? 0x286B39 : 0xAA3333;
        drawSmallString(graphics, Component.translatable("gui.dimensionworks_mek_stress.status_short."
            + menu.status().name().toLowerCase()), 91, 105, statusColor);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void drawMetric(GuiGraphics graphics, String key, Component value,
                            int labelX, int labelY, int valueX, int valueY, int color) {
        drawSmallString(graphics, Component.translatable("gui.dimensionworks_mek_stress." + key), labelX, labelY, 0x555555);
        drawSmallString(graphics, value, valueX, valueY, color);
    }

    private static String compact(long value) {
        if (value >= 1_000_000L) return Math.round(value / 1_000_000.0D) + "M";
        if (value >= 1_000L) return Math.round(value / 1_000.0D) + "k";
        return Long.toString(value);
    }

    private void drawSmallString(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.75F, 0.75F, 1.0F);
        graphics.drawString(font, text, Math.round(x / 0.75F), Math.round(y / 0.75F), color, false);
        graphics.pose().popPose();
    }
}
