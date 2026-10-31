package dev.szx.dimensionworks.rpmlimit.diagnostics;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** A paged source-propagation tree, never a guessed adjacency graph. */
public final class TopologyScreen extends Screen {
    private NetworkSnapshot snapshot;
    private final Map<Integer, double[]> positions = new HashMap<>();
    private double panX, panY, zoom = 1;
    private int selected = -1;
    private int graphRight;
    private int detailsScroll;

    private TopologyScreen(NetworkSnapshot snapshot) {
        super(Component.literal("应力网络拓扑 · 服务端快照"));
        this.snapshot = snapshot;
    }
    public static void show(NetworkSnapshot snapshot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TopologyScreen screen && screen.snapshot.origin().equals(snapshot.origin())) {
            boolean changedPage = screen.snapshot.page() != snapshot.page();
            BlockPos selectedPos = screen.selected >= 0 ? screen.snapshot.nodes().get(screen.selected).pos() : null;
            screen.snapshot = snapshot;
            screen.selected = -1;
            screen.layout();
            if (changedPage) screen.centerOnEntry();
            else if (selectedPos != null) screen.selected = screen.indexOf(selectedPos);
        } else mc.setScreen(new TopologyScreen(snapshot));
    }
    @Override protected void init() {
        graphRight = Math.max(100, width - 210);
        addRenderableWidget(Button.builder(Component.literal("上一页"), b -> refresh(snapshot.page() - 1)).bounds(10, 10, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("下一页"), b -> refresh(snapshot.page() + 1)).bounds(68, 10, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("刷新"), b -> refresh(snapshot.page())).bounds(126, 10, 46, 20).build());
        addRenderableWidget(Button.builder(Component.literal("居中"), b -> { zoom = 1; centerOnEntry(); }).bounds(176, 10, 46, 20).build());
        layout();
        centerOnEntry();
    }
    private int indexOf(BlockPos pos) {
        for (int i = 0; i < snapshot.nodes().size(); i++) if (snapshot.nodes().get(i).pos().equals(pos)) return i;
        return -1;
    }
    private void centerOnEntry() {
        if (snapshot.nodes().isEmpty()) { selected = -1; panX = 0; panY = 0; return; }
        selected = indexOf(snapshot.focus());
        if (selected < 0) selected = 0;
        double[] point = positions.get(selected);
        panX = (5 + graphRight) / 2.0 - point[0] * zoom;
        panY = (38 + height - 24) / 2.0 - 40 - (point[1] - 40) * zoom;
    }
    private int nodeHalfSize() { return Math.max(3, (int) Math.ceil(11 * zoom)); }
    private void refresh(int page) {
        int last = Math.max(0, (snapshot.total() - 1) / NetworkSnapshot.PAGE_SIZE);
        TopologyNetwork.request(snapshot.origin(), Math.max(0, Math.min(last, page)));
    }
    private void layout() {
        positions.clear();
        Map<BlockPos, Integer> indices = new HashMap<>();
        for (int i = 0; i < snapshot.nodes().size(); i++) if (!snapshot.nodes().get(i).virtual()) indices.put(snapshot.nodes().get(i).pos(), i);
        Map<Integer, Integer> rows = new HashMap<>();
        for (int i = 0; i < snapshot.nodes().size(); i++) {
            int depth = 0;
            java.util.Set<Integer> seen = new java.util.HashSet<>();
            Integer cursor = i;
            while (cursor != null && seen.add(cursor) && depth < 32) {
                cursor = indices.get(snapshot.nodes().get(cursor).parent());
                if (cursor != null) depth++;
            }
            int row = rows.getOrDefault(depth, 0);
            rows.put(depth, row + 1);
            positions.put(i, new double[]{35 + depth * 105, 65 + row * 45});
        }
    }
    private int sx(double x) { return (int) (x * zoom + panX); }
    private int sy(double y) { return (int) ((y - 40) * zoom + 40 + panY); }
    private static String number(float v) { return String.format(Locale.ROOT, "%.1f", v); }
    private static String pos(BlockPos p) { return p == null ? "无" : p.getX() + ", " + p.getY() + ", " + p.getZ(); }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.fill(5, 38, graphRight, height - 24, 0xee17242c);
        g.enableScissor(5, 38, graphRight, height - 24);
        Map<BlockPos, Integer> indices = new HashMap<>();
        for (int i = 0; i < snapshot.nodes().size(); i++) if (!snapshot.nodes().get(i).virtual()) indices.put(snapshot.nodes().get(i).pos(), i);
        for (int i = 0; i < snapshot.nodes().size(); i++) {
            Integer parent = indices.get(snapshot.nodes().get(i).parent());
            if (parent == null) continue;
            double[] a = positions.get(parent), b = positions.get(i);
            int x1 = sx(a[0]), y1 = sy(a[1]), x2 = sx(b[0]), y2 = sy(b[1]);
            // Orthogonal lines show direction from parent to child without custom render state.
            g.fill(Math.min(x1, x2), y1, Math.max(x1, x2) + 1, y1 + 1, 0xff718999);
            g.fill(x2, Math.min(y1, y2), x2 + 1, Math.max(y1, y2) + 1, 0xff718999);
            g.drawString(font, "v", x2 - 2, y2 - 15, 0xffa7c8d8, false);
        }
        for (int i = 0; i < snapshot.nodes().size(); i++) {
            NetworkSnapshot.Node n = snapshot.nodes().get(i);
            double[] p = positions.get(i); int x = sx(p[0]), y = sy(p[1]);
            int color = n.overloaded() ? 0xffe15b64 : n.capacity() > 0 ? 0xff62d39b : n.virtual() ? 0xffb998f0 : 0xff77bce8;
            int half = nodeHalfSize();
            if (selected == i) g.fill(x - half - 2, y - half - 2, x + half + 3, y + half + 3, 0xffffffff);
            g.fill(x - half, y - half, x + half + 1, y + half + 1, color);
            if (!n.icon().isEmpty()) {
                // Inventory rendering preserves each mod's model and resource-pack appearance.
                g.pose().pushPose();
                g.pose().translate(x - 8 * zoom, y - 8 * zoom, 0);
                g.pose().scale((float) zoom, (float) zoom, 1);
                g.renderItem(n.icon(), 0, 0);
                g.pose().popPose();
            }
            g.drawString(font, "#" + (snapshot.page() * NetworkSnapshot.PAGE_SIZE + i) + " " + number(n.actualRpm()), x + half + 4, y - 4, color, false);
        }
        g.disableScissor();
        g.enableScissor(graphRight + 4, 38, width, height - 24);
        int x = graphRight + 8, y = 42 - detailsScroll;
        String[] summary = {"网络 " + snapshot.id(), "采样 tick " + snapshot.time(),
            "页 " + (snapshot.page() + 1) + "/" + Math.max(1, (snapshot.total() + 255) / 256) + " · 节点 " + snapshot.total(),
            "网络上限 " + snapshot.rpmLimit() + " RPM",
            "总负载 " + number(snapshot.stress()) + " SU", "有效容量 " + number(snapshot.capacity()) + " SU",
            "成员负载 " + number(snapshot.loadedStress()), "来源自然容量 " + number(snapshot.loadedCapacity()),
            "未加载负载 " + number(snapshot.unloadedStress()), "未加载容量 " + number(snapshot.unloadedCapacity()),
            "未加载成员 " + snapshot.unloadedMembers()};
        for (String line : summary) { g.drawString(font, line, x, y, 0xffd8e6ed, false); y += 12; }
        if (selected >= 0 && selected < snapshot.nodes().size()) {
            NetworkSnapshot.Node n = snapshot.nodes().get(selected); y += 8;
            String[] detail = {Component.translatable(n.name()).getString(), "位置 " + pos(n.pos()), "上游 " + pos(n.parent()),
                "理论 RPM " + number(n.rpm()), "实际 RPM " + number(n.actualRpm()), "个人上限 " + n.limit(),
                "负载 " + number(n.stress()) + " SU", "自然容量 " + number(n.capacity()) + " SU",
                n.overloaded() ? "状态：过载" : "状态：未过载", n.virtual() ? "虚拟/非当前世界实体" : "世界动力方块"};
            for (String line : detail) { g.drawString(font, font.plainSubstrByWidth(line, 202), x, y, 0xfff1d49b, false); y += 12; }
        }
        g.disableScissor();
        g.drawString(font, "拖动平移 · 滚轮缩放 · 点击节点查看 · 绿色来源 · 紫色虚拟 · 右栏可滚动", 8, height - 19, 0xffc1d6df, false);
        super.render(g, mx, my, partial);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        if (button != 0 || x >= graphRight || y < 38 || y >= height - 24) return false;
        selected = -1;
        for (var entry : positions.entrySet()) if (Math.abs(x - sx(entry.getValue()[0])) <= nodeHalfSize() + 2 && Math.abs(y - sy(entry.getValue()[1])) <= nodeHalfSize() + 2) {
            selected = entry.getKey(); break;
        }
        return true;
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && x < graphRight && y > 38) { panX += dx; panY += dy; return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (x >= graphRight && y >= 38) {
            detailsScroll = Math.max(0, Math.min(280, detailsScroll - (int) (delta * 18)));
            return true;
        }
        if (y < 38) return super.mouseScrolled(x, y, delta);
        double next = Math.max(.2, Math.min(3, zoom * Math.pow(1.15, delta)));
        panX = x - (x - panX) * next / zoom;
        panY = y - 40 - (y - 40 - panY) * next / zoom;
        zoom = next; return true;
    }
    @Override public boolean isPauseScreen() { return false; }
}
