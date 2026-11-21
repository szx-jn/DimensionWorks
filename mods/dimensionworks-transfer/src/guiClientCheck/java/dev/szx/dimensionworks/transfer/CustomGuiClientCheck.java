package dev.szx.dimensionworks.transfer;

import com.mojang.blaze3d.platform.NativeImage;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryMachineBlockEntity;
import dev.szx.dimensionworks.cavefactory.client.FactoryMachineScreen;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.menu.FactoryMachineMenu;
import dev.szx.dimensionworks.cavefactory.registry.CFBlocks;
import dev.szx.dimensionworks.magicegg.client.MagicEggScreen;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import dev.szx.dimensionworks.mekstress.client.CloudDiskScreen;
import dev.szx.dimensionworks.mekstress.client.CrossDimensionTransportScreen;
import dev.szx.dimensionworks.mekstress.client.MemoryDriveScreen;
import dev.szx.dimensionworks.mekstress.memory.CloudDiskMenu;
import dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportMenu;
import dev.szx.dimensionworks.mekstress.memory.MemoryDriveMenu;
import dev.szx.dimensionworks.rpmlimit.controller.AdvancedControllerMenu;
import dev.szx.dimensionworks.rpmlimit.controller.ControllerScreen;
import dev.szx.dimensionworks.rpmlimit.diagnostics.NetworkSnapshot;
import dev.szx.dimensionworks.rpmlimit.diagnostics.TopologyScreen;
import dev.szx.dimensionworks.transfer.access.TransferAccess;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import dev.szx.dimensionworks.transfer.dispatch.DispatchController;
import dev.szx.dimensionworks.transfer.dispatch.DispatchLevel;
import dev.szx.dimensionworks.transfer.dispatch.DispatchScreen;
import dev.szx.dimensionworks.transfer.handler.TransferController;
import dev.szx.dimensionworks.transfer.returnpoint.ReturnPointSavedData;
import dev.szx.dimensionworks.transfer.territory.TerritorySavedData;
import dev.szx.dimensionworks.versiongate.PackVersion;
import dev.szx.dimensionworks.versiongate.VersionCheck;
import dev.szx.dimensionworks.versiongate.client.VersionMismatchScreen;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/** Opt-in real rendering and native dispatch payload regression, in a fresh test world only. */
@EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class CustomGuiClientCheck {
    private static final String[] SCREENS = {"dispatch", "caves", "topology", "controller", "memory-drive",
        "cloud-disk", "transport", "magic-egg", "version-gate", "factory"};
    private static int stage, ticks, backgrounds;
    private static volatile int refused;
    private static long started;
    private static volatile boolean refuseNext;
    private static CompletableFuture<Void> work;
    private static UUID traveller;
    private static BlockPos terminal;

    @SubscribeEvent public static void frame(RenderFrameEvent.Pre event) { backgrounds = 0; }

    @SubscribeEvent public static void background(ScreenEvent.BackgroundRendered event) {
        if (Boolean.getBoolean("dimensionworks.guiClientCheck") && stage < 20)
            require(++backgrounds == 1, "Screen background blurred the custom content twice: " + event.getScreen().getClass());
    }

    @SubscribeEvent public static void refuse(EntityTravelToDimensionEvent event) {
        if (refuseNext && event.getEntity().getUUID().equals(traveller) && event.getDimension().equals(Level.OVERWORLD)) {
            refuseNext = false;
            refused++;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("dimensionworks.guiClientCheck") || stage == 120) return;
        Minecraft mc = Minecraft.getInstance();
        if (started == 0) started = System.currentTimeMillis();
        require(System.currentTimeMillis() - started < 180_000, "Custom GUI check timed out at " + stage);
        if (mc.getOverlay() != null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (work != null) {
            if (!work.isDone()) return;
            work.join(); work = null;
        }
        ticks++;
        if (stage < SCREENS.length * 2) {
            int index = stage / 2;
            if (stage % 2 == 0) { open(mc, index); stage++; ticks = 0; }
            else if (ticks >= 12) {
                for (var child : mc.screen.children()) {
                    if (child instanceof AbstractWidget w && w.visible)
                        require(w.getX() >= 0 && w.getY() >= 0 && w.getX() + w.getWidth() <= mc.screen.width
                            && w.getY() + w.getHeight() <= mc.screen.height, "Offscreen widget in " + SCREENS[index]);
                }
                capture(mc, SCREENS[index]);
                WDTransfer.LOGGER.info("CUSTOM_GUI_RENDER_PASS: {}", SCREENS[index]);
                mc.setScreen(null); stage++; ticks = 0;
            }
            return;
        }
        if (stage == 20) {
            traveller = mc.player.getUUID(); stage = 100; ticks = 0;
            server(mc, player -> {
                try { DispatchLevel.getOrLoad(player.server, WDKeys.TRANSFER_LEVEL); }
                catch (java.io.IOException e) { throw new AssertionError(e); }
                player.addTag(TransferAccess.UNLOCK_TAG);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 7));
                require(TransferController.enterWithKey(player), "Could not enter the test territory");
                terminal = DispatchController.terminal(TerritorySavedData.get(player.server.overworld()).get(traveller));
            });
        } else if (stage == 100 && ticks > 40 && mc.level.dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            stage = 101; ticks = 0;
            server(mc, player -> { PackedPlayerState.setPacked(player, true, false); move(player, true); });
        } else if (stage == 101 && mc.screen instanceof DispatchScreen) {
            refuseNext = true;
            button(mc, "dimensionworks_transfer.destination.overworld").onPress(); stage = 102; ticks = 0;
        } else if (stage == 102 && ticks > 30) {
            require(refused == 1 && mc.screen == null, "Failed dispatch left/reopened a stale picker");
            require(mc.level.dimension().equals(WDKeys.TRANSFER_LEVEL), "Refused dispatch changed dimension");
            server(mc, player -> {
                require(PlayerParcel.isPacked(player) && ReturnPointSavedData.get(player.server.overworld()).get(traveller).isPresent(),
                    "Failed dispatch lost the parcel or source point");
                move(player, false);
            });
            stage = 103; ticks = 0;
        } else if (stage == 103 && ticks > 5) {
            server(mc, player -> move(player, true)); stage = 104; ticks = 0;
        } else if (stage == 104 && mc.screen instanceof DispatchScreen) {
            require(mc.screen.keyPressed(256, 0, 0), "Esc was not handled"); stage = 105; ticks = 0;
        } else if (stage == 105 && ticks > 30) {
            require(mc.screen == null, "Esc closed the picker but terminal contact reopened it");
            server(mc, player -> move(player, false)); stage = 106; ticks = 0;
        } else if (stage == 106 && ticks > 5) {
            server(mc, player -> move(player, true)); stage = 107; ticks = 0;
        } else if (stage == 107 && mc.screen instanceof DispatchScreen) {
            button(mc, "dimensionworks_transfer.destination.overworld").onPress(); stage = 108; ticks = 0;
        } else if (stage == 108 && ticks > 20 && mc.level.dimension().equals(Level.OVERWORLD)) {
            require(mc.screen == null, "Successful dispatch retained the picker");
            server(mc, player -> {
                require(!PlayerParcel.isPacked(player), "Successful retry did not unpack");
                require(ReturnPointSavedData.get(player.server.overworld()).get(traveller).isEmpty(), "Retry retained source point");
                require(player.getMainHandItem().is(Items.DIAMOND) && player.getMainHandItem().getCount() == 7, "Retry lost inventory");
            });
            stage = 109;
        } else if (stage == 109) {
            WDTransfer.LOGGER.info("CUSTOM_GUI_CLIENT_CHECK_PASS: ten screens; native refused dispatch, re-entry, Esc, successful retry, inventory/source preservation");
            stage = 120; mc.stop();
        }
    }

    private static void open(Minecraft mc, int index) {
        var inv = mc.player.getInventory();
        Component title = Component.literal("GUI regression");
        switch (index) {
            case 0 -> DispatchScreen.open(BlockPos.ZERO, true);
            case 1 -> { DispatchScreen.open(BlockPos.ZERO, true); button(mc, "dimensionworks_transfer.destination.alexscaves").onPress(); }
            case 2 -> TopologyScreen.show(new NetworkSnapshot(BlockPos.ZERO, BlockPos.ZERO, 1, 0, 0, 1, 0, 256,
                1024, 128, 0, 0, 1024, 128, List.of(new NetworkSnapshot.Node(BlockPos.ZERO, null,
                    "block.minecraft.iron_block", new ItemStack(Items.IRON_BLOCK), 32, 32, 256, 128, 1024, false, false))));
            case 3 -> mc.setScreen(new ControllerScreen(new AdvancedControllerMenu(0, inv, BlockPos.ZERO, 32, 256, 256), inv, title));
            case 4 -> {
                var block = (EntityBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse("dimensionworks_mek_stress:memory_drive_ddr5"));
                var state = ((net.minecraft.world.level.block.Block) block).defaultBlockState();
                var drive = (MemoryDriveBlockEntity) block.newBlockEntity(BlockPos.ZERO, state);
                drive.setLevel(mc.level);
                mc.setScreen(new MemoryDriveScreen(new MemoryDriveMenu(0, inv, drive), inv, title));
            }
            case 5 -> mc.setScreen(new CloudDiskScreen(new CloudDiskMenu(0, inv), inv, title));
            case 6 -> mc.setScreen(new CrossDimensionTransportScreen(new CrossDimensionTransportMenu(0, inv,
                (dev.szx.dimensionworks.mekstress.blockentity.CrossDimensionTransportBlockEntity) null), inv, title));
            case 7 -> MagicEggScreen.open(UUID.randomUUID(), List.of("minecraft:speed", "minecraft:poison", "minecraft:levitation"), false, List.of());
            case 8 -> mc.setScreen(new VersionMismatchScreen(new VersionCheck(VersionCheck.Status.TOO_OLD,
                PackVersion.parse("0.3.52"), PackVersion.parse("0.4.0"), "")));
            case 9 -> {
                var block = CFBlocks.controller(MachineType.MAGNETIC).get();
                var machine = (FactoryMachineBlockEntity) block.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                machine.setLevel(mc.level);
                mc.setScreen(new FactoryMachineScreen(new FactoryMachineMenu(0, inv, machine), inv, title));
            }
            default -> throw new AssertionError(index);
        }
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        work = CompletableFuture.runAsync(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(traveller)),
            mc.getSingleplayerServer());
    }

    private static void move(ServerPlayer player, boolean inside) {
        player.connection.teleport(terminal.getX() + (inside ? .5 : 3.5), terminal.getY(), terminal.getZ() + .5, 0, 0);
        player.setDeltaMovement(0, 0, 0);
    }

    private static Button button(Minecraft mc, String key) {
        return mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
            .filter(button -> button.getMessage().equals(Component.translatable(key))).findFirst().orElseThrow();
    }

    private static void capture(Minecraft mc, String name) {
        try {
            var path = mc.gameDirectory.toPath().resolve("screenshots/gui-" + name + ".png");
            Files.createDirectories(path.getParent());
            try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(path); }
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
