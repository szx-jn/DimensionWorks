package dev.szx.dimensionworks.mekstress.card.client;

import dev.szx.dimensionworks.mekstress.card.StressOutputCardMenu;
import dev.szx.dimensionworks.mekstress.card.StressOutputCardNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public final class StressOutputCardScreen extends AbstractContainerScreen<StressOutputCardMenu> {

    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 100;
    private static final int FIELD_WIDTH = 150;
    private static final int FIELD_HEIGHT = 18;

    private EditBox rpmField;
    private EditBox stressField;

    public StressOutputCardScreen(StressOutputCardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.inventoryLabelY = -10_000;
    }

    @Override
    protected void init() {
        super.init();
        rpmField = new EditBox(
            font,
            leftPos + 13,
            topPos + 28,
            FIELD_WIDTH,
            FIELD_HEIGHT,
            Component.translatable("gui.dimensionworks_mek_stress.rpm")
        );
        rpmField.setMaxLength(10);
        rpmField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,10}"));
        addRenderableWidget(rpmField);

        stressField = new EditBox(
            font,
            leftPos + 13,
            topPos + 54,
            FIELD_WIDTH,
            FIELD_HEIGHT,
            Component.translatable("gui.dimensionworks_mek_stress.stress")
        );
        stressField.setMaxLength(12);
        stressField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,12}"));
        addRenderableWidget(stressField);

        addRenderableWidget(Button.builder(
                Component.translatable("gui.dimensionworks_mek_stress.apply"),
                button -> applySettings())
            .bounds(leftPos + 48, topPos + 78, 80, 16)
            .build());

        syncFields();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        syncFields();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
            && (rpmField.isFocused() || stressField.isFocused())) {
            applySettings();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202020);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, 0xFF808080);
        graphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, 0xFF808080);
        graphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, 0xFF808080);
        graphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF808080);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.rpm"), 13, 17, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.stress"), 13, 43, 0xFFFFFFFF, false);
    }

    private void syncFields() {
        if (rpmField != null && !rpmField.isFocused()) {
            rpmField.setValue(Integer.toString(menu.rpm()));
        }
        if (stressField != null && !stressField.isFocused()) {
            stressField.setValue(Long.toString(menu.stressPerTick()));
        }
    }

    private void applySettings() {
        final int rpm;
        final long stress;
        try {
            rpm = Integer.parseInt(rpmField.getValue());
            stress = Long.parseLong(stressField.getValue());
        } catch (NumberFormatException ignored) {
            return;
        }
        if (rpm < 0 || stress < 0L) {
            return;
        }
        StressOutputCardNetwork.sendSettings(menu.containerId, rpm, stress);
    }
}
