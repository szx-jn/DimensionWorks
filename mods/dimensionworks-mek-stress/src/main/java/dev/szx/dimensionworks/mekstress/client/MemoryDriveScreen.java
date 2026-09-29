package dev.szx.dimensionworks.mekstress.client;

import dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus;
import appeng.client.gui.Icon;
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
        new ResourceLocation("ae2", "textures/guis/drive.png");
    private static final int STORAGE_X = 8;
    private static final int STORAGE_Y = 14;
    private static final int LEGACY_SLOT_MASK_X = 70;
    private static final int LEGACY_SLOT_MASK_Y = 13;
    private static final int LEGACY_SLOT_MASK_WIDTH = 37;
    private static final int LEGACY_SLOT_MASK_HEIGHT = 93;

    private int localPage;
    private Button previousButton;
    private Button nextButton;

    public MemoryDriveScreen(MemoryDriveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 199;
        inventoryLabelX = 8;
        inventoryLabelY = 106;
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
        graphics.fill(leftPos + LEGACY_SLOT_MASK_X, topPos + LEGACY_SLOT_MASK_Y,
            leftPos + LEGACY_SLOT_MASK_X + LEGACY_SLOT_MASK_WIDTH,
            topPos + LEGACY_SLOT_MASK_Y + LEGACY_SLOT_MASK_HEIGHT, 0xFFC6C6C6);
        for (int slot = 0; slot < MemoryDrivePaging.SLOTS_PER_PAGE; slot++) {
            int x = leftPos + STORAGE_X + MemoryDrivePaging.slotX(slot);
            int y = topPos + STORAGE_Y + MemoryDrivePaging.slotY(slot);
            Icon.SLOT_BACKGROUND.getBlitter()
                .dest(x - 1, y - 1)
                .opacity(menu.isPageSlotActive(slot) ? 1.0F : 0.4F)
                .blit(graphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSmallString(graphics, title, 8, 5, 0xFFFFFF);
        drawSmallString(graphics, Component.translatable("gui.dimensionworks_mek_stress.page",
            localPage + 1, menu.pageCount()), 108, 8, 0xCCCCCC);
        drawSmallString(graphics, Component.translatable("gui.dimensionworks_mek_stress.drives",
            menu.driveCount()), 90, 24, 0xAAAAAA);
        drawSmallString(graphics, Component.translatable("gui.dimensionworks_mek_stress.stored",
            menu.networkStoredSu(), menu.networkCapacitySu()), 90, 38, 0xFFD166);
        drawSmallString(graphics, Component.translatable("gui.dimensionworks_mek_stress.bandwidth",
            menu.networkBandwidthRpm()), 90, 52, 0x7EC8E3);
        drawSmallString(graphics, Component.translatable("gui.dimensionworks_mek_stress.cards",
            menu.cardCount()), 90, 66, 0xAAAAAA);
        if (menu.status() != MemoryNetworkStatus.OK) {
            drawSmallString(graphics, Component.translatable("gui.dimensionworks_mek_stress.status."
                + menu.status().name().toLowerCase()), 90, 84, 0xFF5555);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void drawSmallString(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.75F, 0.75F, 1.0F);
        graphics.drawString(font, text, Math.round(x / 0.75F), Math.round(y / 0.75F), color, false);
        graphics.pose().popPose();
    }
}
