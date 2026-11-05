package dev.szx.dimensionworks.mekstress.client;

import dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CrossDimensionTransportScreen extends AbstractContainerScreen<CrossDimensionTransportMenu> {
    private EditBox channelInput;
    private Button applyChannel;
    private EditBox coefficientInput;
    private boolean coefficientInitialized;

    public CrossDimensionTransportScreen(CrossDimensionTransportMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 238;
        imageHeight = 194;
        inventoryLabelY = 500;
    }

    @Override protected void init() {
        super.init();
        channelInput = new EditBox(font, leftPos + 14, topPos + 39, 72, 18, Component.translatable("gui.dimensionworks_mek_stress.transport.channel"));
        channelInput.setMaxLength(4);
        channelInput.setValue(Integer.toString(Math.max(1, menu.channel())));
        channelInput.setFilter(value -> value.isEmpty() || value.matches("\\d{1,4}"));
        addRenderableWidget(channelInput);
        applyChannel = addRenderableWidget(Button.builder(Component.translatable("gui.dimensionworks_mek_stress.transport.bind"), button -> {
            int target;
            try { target = Integer.parseInt(channelInput.getValue()); }
            catch (NumberFormatException exception) { return; }
            if (target >= 1 && target <= 1024 && minecraft != null && minecraft.gameMode != null)
                dev.szx.dimensionworks.mekstress.TransportNetwork.setChannel(menu.containerId, target);
        }).bounds(leftPos + 92, topPos + 39, 74, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.dimensionworks_mek_stress.transport.power_mode"), button -> send(0))
            .bounds(leftPos + 14, topPos + 73, 100, 18).build());
        coefficientInput = new EditBox(font, leftPos + 122, topPos + 73, 62, 18,
            Component.translatable("gui.dimensionworks_mek_stress.transport.coefficient"));
        coefficientInput.setMaxLength(10);
        coefficientInput.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("gui.dimensionworks_mek_stress.transport.coefficient")));
        coefficientInput.setFilter(value -> value.isEmpty() || value.matches("\\d{0,7}(\\.\\d{0,2})?"));
        coefficientInput.setValue(Float.toString(menu.coefficient()));
        coefficientInitialized = false;
        addRenderableWidget(coefficientInput);
        addRenderableWidget(Button.builder(Component.translatable("gui.dimensionworks_mek_stress.transport.apply"), button -> applyCoefficient())
            .bounds(leftPos + 188, topPos + 73, 36, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.dimensionworks_mek_stress.transport.item_mode"), button -> send(1))
            .bounds(leftPos + 14, topPos + 98, 100, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.dimensionworks_mek_stress.transport.fluid_mode"), button -> send(2))
            .bounds(leftPos + 122, topPos + 98, 100, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.dimensionworks_mek_stress.transport.priority"), button -> send(5))
            .bounds(leftPos + 14, topPos + 123, 100, 18).build());
        channelInput.setResponder(value -> {
            try {
                int parsed = Integer.parseInt(value);
                applyChannel.active = parsed >= 1 && parsed <= 1024;
            } catch (NumberFormatException exception) { applyChannel.active = false; }
        });
    }

    private void applyCoefficient() {
        try {
            float value = Float.parseFloat(coefficientInput.getValue());
            if (Float.isFinite(value) && value >= 0.01F && value <= 1_000_000.0F)
                dev.szx.dimensionworks.mekstress.TransportNetwork.setCoefficient(menu.containerId, value);
        } catch (NumberFormatException ignored) { }
    }

    @Override protected void containerTick() {
        super.containerTick();
        channelInput.tick();
        coefficientInput.tick();
        if (!coefficientInitialized && menu.tier() > 0 && !coefficientInput.isFocused()) {
            coefficientInput.setValue(Float.toString(menu.coefficient()));
            if (!channelInput.isFocused()) channelInput.setValue(Integer.toString(Math.max(1, menu.channel())));
            coefficientInitialized = true;
        }
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (coefficientInput.isFocused() && (key == 257 || key == 335)) {
            applyCoefficient();
            return true;
        }
        if ((coefficientInput.isFocused() || channelInput.isFocused()) && key != 256)
            return coefficientInput.isFocused() ? coefficientInput.keyPressed(key, scanCode, modifiers)
                : channelInput.keyPressed(key, scanCode, modifiers);
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF01B232A);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF303B43);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 10, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.transport.bound",
            menu.channel() == 0 ? "—" : menu.channel(), menu.tier()), 14, 25, 0xD8E7EC, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.transport.su_status",
            mode(menu.powerMode()), String.format(java.util.Locale.ROOT, "%.2f", menu.coefficient()),
            priority(menu.priority())), 14, 59, 0xFFD166, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.transport.io_status",
            mode(menu.itemMode()), mode(menu.fluidMode())),
            14, 148, 0xD8E7EC, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.transport.rates",
            itemRate(menu.tier()), fluidRate(menu.tier())), 14, 163, 0xFFD166, false);
        graphics.drawString(font, Component.translatable("gui.dimensionworks_mek_stress.transport.output_status",
            menu.outputRpm(), menu.outputCapacity(), menu.itemKinds(), menu.fluidAmount(), menu.upgradesUsed()),
            14, 178, 0xA9C7D2, false);
    }

    private static int itemRate(int tier) { return switch (tier) { case 1 -> 50; case 2 -> 150; default -> 300; }; }
    private static int fluidRate(int tier) { return itemRate(tier); }

    private static Component mode(int mode) { return Component.translatable(switch (mode) {
        case 1 -> "gui.dimensionworks_mek_stress.transport.mode.input";
        case 2 -> "gui.dimensionworks_mek_stress.transport.mode.output";
        default -> "gui.dimensionworks_mek_stress.transport.mode.closed";
    }); }
    private static Component priority(int priority) { return Component.translatable(switch (priority) {
        case 0 -> "gui.dimensionworks_mek_stress.transport.priority.high";
        case 2 -> "gui.dimensionworks_mek_stress.transport.priority.low";
        default -> "gui.dimensionworks_mek_stress.transport.priority.normal";
    }); }
}
