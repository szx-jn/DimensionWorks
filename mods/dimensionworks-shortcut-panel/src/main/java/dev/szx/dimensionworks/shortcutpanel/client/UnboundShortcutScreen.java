package dev.szx.dimensionworks.shortcutpanel.client;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UnboundShortcutScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_TOP = 24;
    private static final int LIST_TOP = PANEL_TOP + 54;

    private List<KeyMapping> unboundMappings = List.of();
    private int scrollOffset;

    public UnboundShortcutScreen() {
        super(Component.translatable("screen.dimensionworks_shortcut_panel.title"));
    }

    @Override
    protected void init() {
        this.unboundMappings = Arrays.stream(this.minecraft.options.keyMappings)
                .filter(KeyMapping::isUnbound)
                .filter(mapping -> mapping != ShortcutPanelKeyMappings.OPEN_PANEL)
                .sorted(Comparator
                        .comparing((KeyMapping mapping) -> Component.translatable(mapping.getCategory()).getString())
                        .thenComparing(mapping -> Component.translatable(mapping.getName()).getString()))
                .toList();
        this.scrollOffset = Math.min(this.scrollOffset, maxScroll());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);

        int panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        int bottom = Math.max(LIST_TOP + ROW_HEIGHT, this.height - 34);
        int right = left + panelWidth;
        int listBottom = bottom - 26;
        int visibleRows = visibleRows(listBottom);

        graphics.fill(left, PANEL_TOP, right, bottom, 0xE810131A);
        graphics.fill(left, PANEL_TOP, right, PANEL_TOP + 1, 0xFF6EA8FE);
        graphics.fill(left, PANEL_TOP + 1, right, LIST_TOP - 5, 0xB0202530);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, PANEL_TOP + 10, 0xFFFFFFFF);
        graphics.drawString(
                this.font,
                Component.translatable("screen.dimensionworks_shortcut_panel.count", this.unboundMappings.size()),
                left + 12,
                PANEL_TOP + 30,
                0xFFB8C2D1);

        if (this.unboundMappings.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.dimensionworks_shortcut_panel.empty"),
                    this.width / 2,
                    LIST_TOP + 16,
                    0xFFE0E6EF);
        } else {
            for (int visibleIndex = 0; visibleIndex < visibleRows; visibleIndex++) {
                int mappingIndex = this.scrollOffset + visibleIndex;
                if (mappingIndex >= this.unboundMappings.size()) {
                    break;
                }

                int rowTop = LIST_TOP + visibleIndex * ROW_HEIGHT;
                int rowBottom = rowTop + ROW_HEIGHT;
                boolean hovered = mouseX >= left + 8 && mouseX < right - 8
                        && mouseY >= rowTop && mouseY < rowBottom;
                if (hovered) {
                    graphics.fill(left + 8, rowTop, right - 8, rowBottom, 0xFF37465D);
                }

                KeyMapping mapping = this.unboundMappings.get(mappingIndex);
                String category = Component.translatable(mapping.getCategory()).getString();
                String name = Component.translatable(mapping.getName()).getString();
                String label = this.font.plainSubstrByWidth(category + " · " + name, panelWidth - 36);
                graphics.drawString(
                        this.font,
                        label,
                        left + 14,
                        rowTop + 6,
                        hovered ? 0xFFFFFFFF : 0xFFD8DEE8);
            }
        }

        String footer = Component.translatable("screen.dimensionworks_shortcut_panel.footer").getString();
        graphics.drawCenteredString(this.font, footer, this.width / 2, bottom - 16, 0xFFB8C2D1);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        int bottom = Math.max(LIST_TOP + ROW_HEIGHT, this.height - 34);
        int listBottom = bottom - 26;
        int visibleRows = visibleRows(listBottom);
        if (mouseX < left + 8 || mouseX >= left + panelWidth - 8
                || mouseY < LIST_TOP || mouseY >= LIST_TOP + visibleRows * ROW_HEIGHT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int index = this.scrollOffset + (int) ((mouseY - LIST_TOP) / ROW_HEIGHT);
        if (index < 0 || index >= this.unboundMappings.size()) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        KeyMapping selected = this.unboundMappings.get(index);
        minecraft.setScreen(null);
        if (!UnboundShortcutTrigger.triggerOnce(selected, minecraft.options.keyMappings)) {
            minecraft.setScreen(new UnboundShortcutScreen());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < 0 || mouseX >= this.width || mouseY < LIST_TOP || mouseY >= this.height - 34) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int direction = verticalAmount > 0 ? -1 : verticalAmount < 0 ? 1 : 0;
        this.scrollOffset = Math.max(0, Math.min(maxScroll(), this.scrollOffset + direction * 3));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int maxScroll() {
        int bottom = Math.max(LIST_TOP + ROW_HEIGHT, this.height - 60);
        return Math.max(0, this.unboundMappings.size() - visibleRows(bottom));
    }

    private int visibleRows(int listBottom) {
        return Math.max(1, (listBottom - LIST_TOP) / ROW_HEIGHT);
    }
}
