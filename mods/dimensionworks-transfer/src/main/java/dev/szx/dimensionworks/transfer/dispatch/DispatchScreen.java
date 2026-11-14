package dev.szx.dimensionworks.transfer.dispatch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Destination picker. Route names and artwork are data-driven through the resource catalog. */
public final class DispatchScreen extends Screen {
    private static final int CELL_WIDTH = 96;
    private static final int CELL_HEIGHT = 82;
    private static final int COLUMNS = 3;
    private static final int GAP = 6;
    private static volatile boolean tutorialLocked;

    private final BlockPos terminal;
    private final boolean alexCavesUnlocked;
    private final boolean cavesPage;
    private final List<Entry> entries = new ArrayList<>();
    private ResourceLocation lockTexture;

    private record Entry(String route, Component name, ResourceLocation image, boolean locked) {}

    private DispatchScreen(BlockPos terminal, boolean alexCavesUnlocked, boolean cavesPage) {
        super(Component.translatable(cavesPage ? "screen.dimensionworks_transfer.alex_caves"
            : "screen.dimensionworks_transfer.dispatch"));
        this.terminal = terminal;
        this.alexCavesUnlocked = alexCavesUnlocked;
        this.cavesPage = cavesPage;
        loadCatalog();
    }

    private void loadCatalog() {
        ResourceLocation catalog = ResourceLocation.fromNamespaceAndPath(
            "dimensionworks_transfer", "dispatch/destinations.json");
        Minecraft.getInstance().getResourceManager().getResource(catalog).ifPresent(resource -> {
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                lockTexture = ResourceLocation.parse(root.get("lock_overlay").getAsString());
                for (var element : root.getAsJsonArray(cavesPage ? "alex_caves" : "destinations")) {
                    JsonObject destination = element.getAsJsonObject();
                    String route = destination.get("route").getAsString();
                    entries.add(new Entry(
                        route,
                        Component.translatable(destination.get("name").getAsString()),
                        ResourceLocation.parse(destination.get("image").getAsString()),
                        route.equals("alexscaves") ? !alexCavesUnlocked
                            : destination.has("locked") && destination.get("locked").getAsBoolean()));
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Invalid dispatch destination catalog", exception);
            }
        });
    }

    public static void open(BlockPos terminal, boolean alexCavesUnlocked) {
        Minecraft.getInstance().setScreen(new DispatchScreen(terminal, alexCavesUnlocked, false));
    }

    public static void tutorial(boolean locked) {
        tutorialLocked = locked;
    }

    @Override
    protected void init() {
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        int gridWidth = COLUMNS * CELL_WIDTH + (COLUMNS - 1) * GAP;
        int gridHeight = rows * CELL_HEIGHT + Math.max(0, rows - 1) * GAP;
        int startX = (width - gridWidth) / 2;
        int startY = gridY(gridHeight);

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int x = startX + (i % COLUMNS) * (CELL_WIDTH + GAP);
            int y = startY + (i / COLUMNS) * (CELL_HEIGHT + GAP);
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(entry.name(), button -> {
                if (entry.locked()) {
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("message.dimensionworks_transfer.destination_locked"), true);
                } else if (entry.route().equals("alexscaves")) {
                    Minecraft.getInstance().setScreen(new DispatchScreen(terminal, alexCavesUnlocked, true));
                } else {
                    TransferNetwork.sendChoice(terminal, entry.route());
                }
            }).bounds(x, y + 62, CELL_WIDTH, 20).build());
        }
        if (cavesPage) {
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 48, height - 24, 96, 20).build());
        }
    }

    private int gridY(int gridHeight) {
        return Math.max(30, (height - gridHeight - 24) / 2 + 12);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 13, 0xFFFFFFFF);

        int gridWidth = COLUMNS * CELL_WIDTH + (COLUMNS - 1) * GAP;
        int startX = (width - gridWidth) / 2;
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        int gridHeight = rows * CELL_HEIGHT + Math.max(0, rows - 1) * GAP;
        int startY = gridY(gridHeight);
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int x = startX + (i % COLUMNS) * (CELL_WIDTH + GAP);
            int y = startY + (i / COLUMNS) * (CELL_HEIGHT + GAP);
            graphics.blit(entry.image(), x, y, CELL_WIDTH, 62, 0, 0, 108, 82, 108, 82);
            if (entry.locked() && lockTexture != null) {
                graphics.fill(x, y, x + CELL_WIDTH, y + 62, 0x990B1420);
                graphics.blit(lockTexture, x + (CELL_WIDTH - 32) / 2, y + 15, 0, 0, 32, 32, 32, 32);
            }
        }
        if (!cavesPage) {
            graphics.drawCenteredString(font, Component.translatable(alexCavesUnlocked
                ? "message.dimensionworks_transfer.alex_caves_unlocked"
                : "message.dimensionworks_transfer.alex_caves_upgrade_hint"), width / 2, height - 14, 0xFFE1CFA6);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (tutorialLocked) {
            graphics.fill(0, 0, width, height, 0xAA000000);
            graphics.drawCenteredString(
                font,
                Component.translatable("message.dimensionworks_transfer.tutorial_title"),
                width / 2,
                height / 2,
                0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (cavesPage) {
            Minecraft.getInstance().setScreen(new DispatchScreen(terminal, alexCavesUnlocked, false));
            return;
        }
        TransferNetwork.sendClose();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
