package dev.szx.dimensionworks.magicegg.client;

import dev.szx.dimensionworks.magicegg.MagicEggNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.ForgeRegistries;

/** 完全使用文字、矩形与代码绘制的勾选框，不使用物品槽或新增 GUI 贴图。 */
public final class MagicEggScreen extends Screen {
    private static final int ROW_HEIGHT = 32;
    private static final int TEXT = 0xFFE9EEF5;
    private static final int MUTED = 0xFFA8B4C7;
    private final UUID token;
    private final List<Entry> catalogue;
    private final List<Entry> previous;
    private final Set<String> selected = new HashSet<>();
    private final List<Button> effectButtons = new ArrayList<>();
    private final List<FlatButton> filters = new ArrayList<>();
    private boolean reusePrompt;
    private boolean submitted;
    private boolean selectedOnly;
    private String query = "";
    private int category;
    private int scrollRow;
    private int left, top, panelWidth, panelHeight, listTop, listBottom, visibleRows, columns;
    private List<Entry> filtered = List.of();
    private EditBox search;
    private FlatButton selectedFilter;

    private record Entry(String id, MobEffect effect) {
        Component name() { return Component.translatable(effect.getDescriptionId()); }
        int category() {
            return effect.getCategory() == MobEffectCategory.BENEFICIAL ? 1
                    : effect.getCategory() == MobEffectCategory.NEUTRAL ? 2 : 3;
        }
        int color() { return category() == 1 ? 0xFF75D9AC : category() == 2 ? 0xFFE4C477 : 0xFFFF9298; }
    }

    public static void open(UUID token, List<String> catalogue, boolean hasPrevious, List<String> previous) {
        Minecraft.getInstance().setScreen(new MagicEggScreen(token, catalogue, hasPrevious, previous));
    }

    private MagicEggScreen(UUID token, List<String> catalogue, boolean hasPrevious, List<String> previous) {
        super(text("title"));
        this.token = token;
        this.catalogue = catalogue.stream().map(id -> new Entry(id,
                ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(id))))
                .sorted(Comparator.comparing(entry -> entry.name().getString())).toList();
        this.previous = this.catalogue.stream().filter(entry -> previous.contains(entry.id())).toList();
        this.reusePrompt = hasPrevious;
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("gui.dimensionworks_magic_egg." + key, arguments);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(620, width - 16);
        panelHeight = Math.min(430, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        listTop = top + 98;
        listBottom = top + panelHeight - 80;
        columns = panelWidth >= 460 ? 2 : 1;
        visibleRows = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        effectButtons.clear();
        filters.clear();
        addRenderableWidget(new FlatButton(left + panelWidth - 28, top + 10, 18, 18,
                Component.literal("×"), 0xFF364052, button -> onClose()));
        if (reusePrompt) {
            int buttonWidth = Math.min(150, (panelWidth - 44) / 2);
            int y = top + panelHeight - 32;
            addRenderableWidget(new FlatButton(left + 14, y, buttonWidth, 22,
                    text("reuse_no"), 0xFF364052, button -> {
                        reusePrompt = false;
                        scrollRow = 0;
                        rebuildWidgets();
                    }));
            addRenderableWidget(new FlatButton(left + panelWidth - 14 - buttonWidth, y, buttonWidth, 22,
                    text("reuse_yes"), 0xFF275F50, button -> finish(true)));
            return;
        }

        int searchWidth = panelWidth - 132;
        search = new EditBox(font, left + 18, top + 43, searchWidth - 8, 16, text("search"));
        search.setBordered(false);
        search.setTextColor(TEXT);
        search.setMaxLength(128);
        search.setHint(text("search"));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            scrollRow = 0;
            refreshEffects();
        });
        addRenderableWidget(search);
        selectedFilter = addRenderableWidget(new FlatButton(left + panelWidth - 110, top + 40, 96, 22,
                text("selected_only"), 0xFF364052, button -> {
                    selectedOnly = !selectedOnly;
                    scrollRow = 0;
                    refreshEffects();
                }));
        String[] categories = {"all", "beneficial", "neutral", "harmful"};
        int tabWidth = (panelWidth - 28 - 18) / 4;
        for (int index = 0; index < categories.length; index++) {
            int filter = index;
            filters.add(addRenderableWidget(new FlatButton(left + 14 + index * (tabWidth + 6), top + 70,
                    tabWidth, 20, text(categories[index]), 0xFF364052, button -> {
                        category = filter;
                        scrollRow = 0;
                        refreshEffects();
                    })));
        }
        int y = top + panelHeight - 30;
        addRenderableWidget(new FlatButton(left + 14, y, 60, 22, text("clear"), 0xFF364052, button -> {
            selected.clear();
            refreshEffects();
        }));
        addRenderableWidget(new FlatButton(left + 80, y, 60, 22, text("cancel"), 0xFF364052, button -> onClose()));
        int confirmWidth = Math.min(180, (panelWidth - 28) / 2);
        addRenderableWidget(new FlatButton(left + panelWidth - 14 - confirmWidth, y, confirmWidth, 22,
                text("confirm"), 0xFF275F50, button -> finish(false)));
        refreshEffects();
    }

    private void refreshEffects() {
        String focusedId = getFocused() instanceof EffectButton button ? button.entry.id() : null;
        if (focusedId != null) setFocused(null);
        effectButtons.forEach(this::removeWidget);
        effectButtons.clear();
        String needle = query.trim().toLowerCase(Locale.ROOT);
        filtered = catalogue.stream().filter(entry -> (category == 0 || category == entry.category())
                && (!selectedOnly || selected.contains(entry.id()))
                && (entry.name().getString().toLowerCase(Locale.ROOT).contains(needle)
                    || entry.id().contains(needle))).toList();
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll()));
        selectedFilter.fill = selectedOnly ? 0xFF275F50 : 0xFF364052;
        for (int index = 0; index < filters.size(); index++) {
            filters.get(index).fill = category == index ? 0xFF325876 : 0xFF364052;
        }
        int cellWidth = (panelWidth - 34 - (columns - 1) * 6) / columns;
        for (int index = 0; index < visibleRows * columns; index++) {
            int entryIndex = scrollRow * columns + index;
            if (entryIndex >= filtered.size()) break;
            Entry entry = filtered.get(entryIndex);
            EffectButton button = new EffectButton(left + 14 + (index % columns) * (cellWidth + 6),
                    listTop + (index / columns) * ROW_HEIGHT, cellWidth, entry);
            effectButtons.add(addRenderableWidget(button));
            if (entry.id().equals(focusedId)) setFocused(button);
        }
    }

    private int maxScroll() {
        return Math.max(0, (filtered.size() + columns - 1) / columns - visibleRows);
    }

    private void finish(boolean reuse) {
        if (submitted) return;
        submitted = true;
        MagicEggNetwork.submit(token, reuse, List.copyOf(selected));
        onClose();
    }

    @Override
    public void removed() {
        if (!submitted) MagicEggNetwork.cancel(token);
    }

    @Override
    public void tick() {
        if (search != null && !reusePrompt) search.tick();
        if (minecraft.player == null || !minecraft.player.isAlive()) onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int scrollBottom = reusePrompt ? top + panelHeight - 46 : listBottom;
        if (mouseX >= left && mouseX <= left + panelWidth && mouseY >= listTop && mouseY < scrollBottom) {
            if (reusePrompt) {
                int rows = Math.max(1, (top + panelHeight - 46 - listTop) / 18);
                scrollRow = Math.max(0, Math.min(Math.max(0, previous.size() - rows), scrollRow + (delta > 0 ? -1 : 1)));
            } else {
                scrollRow += delta > 0 ? -1 : 1;
                refreshEffects();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        // 搜索框有焦点时保留方向键与 Home/End 的文本编辑行为。
        if (!reusePrompt && !search.isFocused()) {
            int next = switch (key) {
                case 264 -> scrollRow + 1;
                case 265 -> scrollRow - 1;
                case 267 -> scrollRow + visibleRows;
                case 266 -> scrollRow - visibleRows;
                case 268 -> 0;
                case 269 -> maxScroll();
                default -> scrollRow;
            };
            if (next != scrollRow) {
                scrollRow = next;
                refreshEffects();
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0081019);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFA172130);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFF75D9AC);
        graphics.drawString(font, title, left + 14, top + 13, TEXT, false);
        graphics.drawString(font, fit(text("subtitle"), panelWidth - 28), left + 14, top + 27, MUTED, false);
        if (reusePrompt) {
            graphics.drawString(font, text("reuse_question"), left + 14, top + 48, TEXT, false);
            graphics.drawString(font, text("reuse_count", previous.size()), left + 14, top + 64, MUTED, false);
            graphics.drawString(font, text("reuse_warning"), left + 14, top + 80, 0xFFFF9298, false);
            int rows = Math.max(1, (top + panelHeight - 46 - listTop) / 18);
            scrollRow = Math.min(scrollRow, Math.max(0, previous.size() - rows));
            for (int row = 0; row < rows && scrollRow + row < previous.size(); row++) {
                Entry entry = previous.get(scrollRow + row);
                graphics.drawString(font, fit(entry.name(), panelWidth - 46),
                        left + 18, listTop + row * 18, entry.color(), false);
            }
            if (previous.isEmpty()) graphics.drawString(font, text("none"), left + 18, listTop, MUTED, false);
            drawScrollbar(graphics, previous.size(), rows, 18, top + panelHeight - 46);
        } else {
            graphics.fill(left + 14, top + 40, left + panelWidth - 118, top + 62, 0xFF0E1825);
            if (filtered.isEmpty()) graphics.drawCenteredString(font, text("empty"), width / 2, listTop + 10, MUTED);
            drawScrollbar(graphics, (filtered.size() + columns - 1) / columns, visibleRows, ROW_HEIGHT, listBottom);
            graphics.drawString(font, text("count", selected.size(), filtered.size()),
                    left + 14, top + panelHeight - 72, MUTED, false);
            graphics.drawString(font, text("disclaimer_1"), left + 14, top + panelHeight - 57, 0xFFE4C477, false);
            graphics.drawString(font, text("disclaimer_2"), left + 14, top + panelHeight - 45, 0xFFE4C477, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String fit(Component label, int maxWidth) {
        String value = label.getString();
        return font.width(value) <= maxWidth ? value
                : font.plainSubstrByWidth(value, maxWidth - font.width("…")) + "…";
    }

    private void drawScrollbar(GuiGraphics graphics, int totalRows, int rows, int rowHeight, int bottom) {
        if (totalRows <= rows) return;
        int trackHeight = Math.min(bottom - listTop, rows * rowHeight);
        int thumbHeight = Math.max(8, trackHeight * rows / totalRows);
        int y = listTop + (trackHeight - thumbHeight) * scrollRow / (totalRows - rows);
        int x = left + panelWidth - 10;
        graphics.fill(x, listTop, x + 3, listTop + trackHeight, 0xFF29364A);
        graphics.fill(x, y, x + 3, y + thumbHeight, 0xFF91AAC8);
    }

    private class FlatButton extends Button {
        private int fill;

        FlatButton(int x, int y, int width, int height, Component label, int fill, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
            this.fill = fill;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);
            if (isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFF91AAC8);
            graphics.drawCenteredString(font, fit(getMessage(), getWidth() - 6),
                    getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, TEXT);
        }
    }

    private final class EffectButton extends Button {
        private final Entry entry;

        EffectButton(int x, int y, int width, Entry entry) {
            super(x, y, width, ROW_HEIGHT - 4, entry.name(), button -> {
                if (!selected.add(entry.id())) selected.remove(entry.id());
                refreshEffects();
            }, DEFAULT_NARRATION);
            this.entry = entry;
            setTooltip(Tooltip.create(entry.name().copy().append(" V\n" + entry.id())
                    .append("\n").append(text(entry.effect().isInstantenous() ? "instant" : "duration"))));
            updateLabel();
        }

        private void updateLabel() {
            setMessage(text(selected.contains(entry.id()) ? "checked" : "unchecked", entry.name()));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean checked = selected.contains(entry.id());
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), checked ? 0xFF234638 : 0xFF233044);
            if (isHoveredOrFocused()) graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFF91AAC8);
            graphics.renderOutline(getX() + 7, getY() + 8, 12, 12, checked ? 0xFF75D9AC : MUTED);
            if (checked) {
                graphics.fill(getX() + 10, getY() + 13, getX() + 13, getY() + 17, 0xFF75D9AC);
                graphics.fill(getX() + 13, getY() + 11, getX() + 16, getY() + 17, 0xFF75D9AC);
            }
            graphics.drawString(font, fit(entry.name().copy().append(" V"), getWidth() - 30),
                    getX() + 25, getY() + 4, entry.color(), false);
            String detail = entry.id().substring(0, entry.id().indexOf(':')) + " · "
                    + text(entry.effect().isInstantenous() ? "instant" : "duration").getString();
            graphics.drawString(font, font.plainSubstrByWidth(detail, getWidth() - 30),
                    getX() + 25, getY() + 16, MUTED, false);
        }
    }
}
