package dev.szx.dimensionworks.cavefactory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.szx.dimensionworks.cavefactory.menu.FactoryMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FactoryMachineScreen extends AbstractContainerScreen<FactoryMachineMenu> {
    private static final int BACKGROUND = 0xFF20252B;
    private static final int PANEL = 0xFF303740;
    private static final int SLOT = 0xFF111417;
    private static final int LINE = 0xFF56616D;

    public FactoryMachineScreen(FactoryMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 214;
        imageHeight = 186;
        inventoryLabelY = 92;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, BACKGROUND);
        graphics.fill(left + 4, top + 4, left + 172, top + 94, PANEL);
        graphics.fill(left + 4, top + 98, left + 172, top + 182, PANEL);
        graphics.fill(left + 172, top + 4, left + 210, top + 94, PANEL);
        graphics.fill(left + 172, top + 98, left + 210, top + 182, PANEL);

        for (int index = 0; index < 13; index++) {
            int x = left + switch (index) {
                case 0, 1, 2, 3, 4, 5, 6, 7 -> 8 + (index % 4) * 20;
                case 8, 9, 10 -> 8 + (index - 8) * 30;
                default -> 150;
            };
            int y = top + switch (index) {
                case 0, 1, 2, 3 -> 20;
                case 4, 5, 6, 7 -> 48;
                case 8, 9, 10 -> 76;
                default -> 20 + (index - 11) * 28;
            };
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT);
            graphics.renderOutline(x - 2, y - 2, 20, 20, LINE);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int x = left + 8 + column * 18;
                int y = top + 104 + row * 18;
                graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT);
            }
        }
        for (int column = 0; column < 9; column++) {
            int x = left + 8 + column * 18;
            int y = top + 162;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xE6EDF3, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC6D0DA, false);
        int phase = menu.data().get(0);
        int progress = menu.data().get(1);
        int batches = Math.max(1, menu.data().get(2));
        int rpm = menu.data().get(3);
        int structure = menu.data().get(4);
        int locked = menu.data().get(5);

        graphics.drawString(font, Component.translatable(
            "gui.dimensionworks_cave_factory.structure." + (structure == 1 ? "valid" : "invalid")
        ), 88, 20, structure == 1 ? 0x7DD87D : 0xE06C75, false);
        graphics.drawString(font, Component.translatable(
            "gui.dimensionworks_cave_factory.phase_short",
            phase == 1 ? "B" : "A",
            progress,
            batches
        ), 88, 34, 0xD7DEE8, false);
        graphics.drawString(font, Component.translatable(
            "gui.dimensionworks_cave_factory.rpm",
            rpm
        ), 88, 48, 0x8AB4F8, false);
        if (locked == 1) {
            graphics.drawString(font, Component.translatable(
                "gui.dimensionworks_cave_factory.redstone_locked_short"
            ), 88, 62, 0xF0A35E, false);
        }
    }
}
