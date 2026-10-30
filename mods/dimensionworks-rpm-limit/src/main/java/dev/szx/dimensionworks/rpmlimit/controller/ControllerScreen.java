package dev.szx.dimensionworks.rpmlimit.controller;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public final class ControllerScreen extends AbstractContainerScreen<AdvancedControllerMenu> {
    private EditBox speed;
    private boolean submitted;
    public ControllerScreen(AdvancedControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 170;
    }
    @Override protected void init() {
        super.init();
        speed = new EditBox(font, leftPos + 36, topPos + 60, 150, 22,
            Component.translatable("gui.dimensionworks_rpm_limit.target_speed"));
        speed.setMaxLength(7);
        speed.setFilter(text -> text.matches("-?[0-9]*"));
        speed.setValue(Integer.toString(menu.target));
        addRenderableWidget(speed);
        setInitialFocus(speed);
    }
    private Integer parsed() {
        try {
            int value = Integer.parseInt(speed.getValue());
            return value >= -menu.maximum && value <= menu.maximum ? value : null;
        } catch (NumberFormatException ignored) { return null; }
    }
    private void submit() {
        if (!submitted && speed != null) {
            submitted = true;
            Integer value = parsed();
            if (value != null && value != menu.target) ControllerNetwork.submit(menu.pos, value);
        }
    }
    @Override public void onClose() {
        submit();
        super.onClose();
    }
    @Override public void removed() {
        submit();
        super.removed();
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { onClose(); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(key, scan, modifiers);
        if (speed.isFocused()) return speed.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
    @Override protected void containerTick() { speed.tick(); }
    @Override protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x-2, y-2, x+250, y+imageHeight+2, 0xff342d25);
        g.fill(x, y, x+248, y+imageHeight, 0xffb88a4c);
        g.fill(x+3, y+3, x+245, y+imageHeight-3, 0xffede0c6);
        g.fill(x+7, y+7, x+241, y+imageHeight-7, 0xff665f51);
        g.fill(x+9, y+9, x+239, y+imageHeight-9, 0xff9b9688);
        g.fill(x+9, y+9, x+239, y+31, 0xff484b43);
        g.fill(x+25, y+50, x+223, y+94, 0xff3e372b);
        g.fill(x+28, y+53, x+220, y+91, 0xff222c27);
        for (int dx : new int[]{4, 239}) for (int dy : new int[]{4, imageHeight-9}) {
            g.fill(x+dx, y+dy, x+dx+5, y+dy+5, 0xff4a4235);
            g.fill(x+dx+1, y+dy+1, x+dx+4, y+dy+2, 0xffd9c392);
        }
        g.drawString(font, "RPM", x+192, y+66, 0xffc5b67c, false);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawCenteredString(font, title, imageWidth/2, 16, 0xfff1dfb8);
        g.drawString(font, Component.translatable("gui.dimensionworks_rpm_limit.target_speed"), 26, 38, 0xff292c27, false);
        boolean valid = parsed() != null;
        g.drawCenteredString(font, Component.translatable("gui.dimensionworks_rpm_limit.range", menu.maximum, menu.maximum), 124, 102, valid ? 0xff303c30 : 0xff8f2420);
        g.drawCenteredString(font, Component.translatable(valid ? "gui.dimensionworks_rpm_limit.save_on_close" : "gui.dimensionworks_rpm_limit.invalid"), 124, 119, valid ? 0xff34372f : 0xff8f2420);
        g.drawCenteredString(font, Component.translatable("gui.dimensionworks_rpm_limit.effective_limit",
            menu.effectiveMaximum), 124, 135, 0xff34372f);
        if (menu.effectiveMaximum < menu.maximum)
            g.drawCenteredString(font, Component.translatable("gui.dimensionworks_rpm_limit.external_limit"),
                124, 148, 0xff7b331c);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
    }
}
