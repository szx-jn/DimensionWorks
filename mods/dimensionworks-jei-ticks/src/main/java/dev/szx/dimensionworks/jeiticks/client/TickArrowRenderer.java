package dev.szx.dimensionworks.jeiticks.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class TickArrowRenderer {
    public enum Direction { RIGHT, LEFT, DOWN }

    private TickArrowRenderer() {}

    public static boolean draw(GuiGraphics graphics, int x, int y, int width, int height, Direction direction) {
        long ticks = RecipeDrawContext.ticks();
        if (ticks <= 0) {
            return false;
        }
        Font font = Minecraft.getInstance().font;
        String text = ticks + "t";
        int textWidth = font.width(text);
        boolean vertical = direction == Direction.DOWN;
        int arrowWidth = vertical ? Math.max(width, textWidth + 8) : width;
        int arrowHeight = Math.max(height, font.lineHeight + 6);
        int left = x + (width - arrowWidth) / 2;
        int top = y + (height - arrowHeight) / 2;
        // Keep neighboring slots free. Large values shrink inside the original horizontal width.
        int availableWidth = vertical ? arrowWidth - 6 : arrowWidth - 8;
        float scale = Math.min(1.0F, availableWidth / (float) Math.max(1, textWidth));
        // This is only a visual cycle. The label always comes from the recipe or machine contract.
        long cycle = Math.max(50L, Math.min(ticks, 1200L) * 50L);
        double progress = (Util.getMillis() % cycle) / (double) cycle;
        graphics.pose().pushPose();
        try {
            shape(graphics, left, top, arrowWidth, arrowHeight, direction, 0xFF555555);
            // Fill progress along the arrow's axis without changing its outline.
            int extent = vertical ? arrowHeight : arrowWidth;
            int filled = (int) (progress * extent);
            for (int i = 0; i < filled; i++) {
                int axis = direction == Direction.LEFT ? extent - 1 - i : i;
                slice(graphics, left, top, arrowWidth, arrowHeight, direction, axis, 0xFFE2B746);
            }
            graphics.pose().translate(left + arrowWidth / 2.0F, top + arrowHeight / 2.0F, 1);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, text, -textWidth / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
        } finally {
            graphics.pose().popPose();
        }
        return true;
    }

    private static void shape(GuiGraphics graphics, int x, int y, int width, int height,
                              Direction direction, int color) {
        int extent = direction == Direction.DOWN ? height : width;
        for (int i = 0; i < extent; i++) {
            slice(graphics, x, y, width, height, direction, i, color);
        }
    }

    private static void slice(GuiGraphics graphics, int x, int y, int width, int height,
                              Direction direction, int axis, int color) {
        if (direction == Direction.DOWN) {
            int inset = axis < height - 4 ? 2 : (axis - (height - 4)) * (width / 2) / 4;
            graphics.fill(x + inset, y + axis, x + width - inset, y + axis + 1, color);
        } else {
            int fromTail = direction == Direction.LEFT ? width - 1 - axis : axis;
            int inset = fromTail < width - 4 ? 2 : (fromTail - (width - 4)) * (height / 2) / 4;
            graphics.fill(x + axis, y + inset, x + axis + 1, y + height - inset, color);
        }
    }
}
