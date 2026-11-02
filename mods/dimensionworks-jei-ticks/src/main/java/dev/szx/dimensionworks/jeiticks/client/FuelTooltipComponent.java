package dev.szx.dimensionworks.jeiticks.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class FuelTooltipComponent implements ClientTooltipComponent {
    private record Row(FuelEntry entry, List<FormattedCharSequence> name,
                       List<FormattedCharSequence> rate, List<FormattedCharSequence> condition, int height) {}
    private final List<Row> rows = new ArrayList<>();
    private final Component footer;
    private final int width;
    private final int height;

    public FuelTooltipComponent(FuelTooltipData data) {
        Font font = Minecraft.getInstance().font;
        width = data.width();
        int totalHeight = font.lineHeight + 4;
        for (FuelEntry entry : data.entries()) {
            var name = font.split(entry.icon().name(), Math.max(20, width - 20));
            var rate = font.split(entry.rate(), Math.max(20, width - 20));
            var condition = entry.condition().getString().isEmpty() ? List.<FormattedCharSequence>of()
                : font.split(entry.condition(), Math.max(20, width - 20));
            int rowHeight = Math.max(18, name.size() * font.lineHeight) +
                (rate.size() + condition.size()) * font.lineHeight + 4;
            rows.add(new Row(entry, name, rate, condition, rowHeight));
            totalHeight += rowHeight;
        }
        footer = data.pages() > 1 ? Component.translatable("tooltip.dimensionworks_jei_ticks.fuel_page",
            data.page() + 1, data.pages()) : Component.empty();
        height = totalHeight + (data.pages() > 1 ? font.split(footer, width).size() * font.lineHeight : 0);
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth(Font font) {
        return width;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("tooltip.dimensionworks_jei_ticks.fuel_title"), x, y, 0xFFE2B746, false);
        int lineY = y + font.lineHeight + 4;
        for (Row row : rows) {
            row.entry().icon().draw(graphics, x, lineY);
            int textY = lines(graphics, font, row.name(), x + 20, lineY, 0xFFFFFFFF);
            textY = Math.max(lineY + 18, textY);
            textY = lines(graphics, font, row.rate(), x + 20, textY, 0xFFFFD479);
            lines(graphics, font, row.condition(), x + 20, textY, 0xFFAAAAAA);
            lineY += row.height();
        }
        lines(graphics, font, font.split(footer, width), x, lineY, 0xFF888888);
    }

    private static int lines(GuiGraphics graphics, Font font, List<FormattedCharSequence> lines,
                             int x, int y, int color) {
        for (var line : lines) {
            graphics.drawString(font, line, x, y, color, false);
            y += font.lineHeight;
        }
        return y;
    }
}
