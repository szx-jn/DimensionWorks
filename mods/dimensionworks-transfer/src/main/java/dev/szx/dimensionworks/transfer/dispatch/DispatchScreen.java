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
    private final List<Entry> entries = new ArrayList<>();
    private ResourceLocation lockTexture;

    private record Entry(String route, Component name, ResourceLocation image, boolean locked) {}

    private DispatchScreen(BlockPos terminal) {
        super(Component.translatable("screen.dimensionworks_transfer.dispatch"));
        this.terminal = terminal;
        loadCatalog();
    }

    private void loadCatalog() {
        ResourceLocation catalog = ResourceLocation.fromNamespaceAndPath(
            "dimensionworks_transfer", "dispatch/destinations.json");
        Minecraft.getInstance().getResourceManager().getResource(catalog).ifPresent(resource -> {
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                lockTexture = ResourceLocation.parse(root.get("lock_overlay").getAsString());
                for (var element : root.getAsJsonArray("destinations")) {
                    JsonObject destination = element.getAsJsonObject();
                    entries.add(new Entry(
                        destination.get("route").getAsString(),
                        Component.translatable(destination.get("name").getAsString()),
                        ResourceLocation.parse(destination.get("image").getAsString()),
                        destination.has("locked") && destination.get("locked").getAsBoolean()));
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Invalid dispatch destination catalog", exception);
            }
        });
    }

    public static void open(BlockPos terminal) {
        Minecraft.getInstance().setScreen(new DispatchScreen(terminal));
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
        int startY = (height - gridHeight) / 2 + 8;

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int x = startX + (i % COLUMNS) * (CELL_WIDTH + GAP);
            int y = startY + (i / COLUMNS) * (CELL_HEIGHT + GAP);
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(entry.name(), button -> {
                if (entry.locked()) {
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("message.dimensionworks_transfer.destination_locked"), true);
                } else {
                    TransferNetwork.sendChoice(terminal, entry.route());
                }
            }).bounds(x, y + 62, CELL_WIDTH, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 13, 0xFFFFFFFF);

        int gridWidth = COLUMNS * CELL_WIDTH + (COLUMNS - 1) * GAP;
        int startX = (width - gridWidth) / 2;
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        int gridHeight = rows * CELL_HEIGHT + Math.max(0, rows - 1) * GAP;
        int startY = (height - gridHeight) / 2 + 8;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int x = startX + (i % COLUMNS) * (CELL_WIDTH + GAP);
            int y = startY + (i / COLUMNS) * (CELL_HEIGHT + GAP);
            graphics.blit(entry.image(), x, y, 0, 0, CELL_WIDTH, 62, 108, 82);
            if (entry.locked() && lockTexture != null) {
                graphics.blit(lockTexture, x, y, 0, 0, CELL_WIDTH, 62, 32, 32);
            }
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
        TransferNetwork.sendClose();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
