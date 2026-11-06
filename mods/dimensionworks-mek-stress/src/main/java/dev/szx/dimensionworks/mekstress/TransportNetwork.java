package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Menu-scoped settings: the server checks distance, identity and channel ownership. */
public final class TransportNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(DimensionWorksMekStress.MOD_ID, "transport"), () -> "2", "2"::equals, "2"::equals);
    private record Coefficient(int menuId, float value) {}
    private record Channel(int menuId, int value) {}
    private record OpenCloudDisk() {}
    private TransportNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(OpenCloudDisk.class, 2, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> {})
            .decoder(buffer -> new OpenCloudDisk())
            .consumerMainThread((message, context) -> {
                var player = context.get().getSender();
                if (player != null && player.isAlive() && !player.isSpectator()
                    && player.containerMenu == player.inventoryMenu) {
                    net.minecraftforge.network.NetworkHooks.openScreen(player,
                        new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, owner) -> new dev.szx.dimensionworks.mekstress.memory.CloudDiskMenu(id, inventory),
                            net.minecraft.network.chat.Component.translatable("gui.dimensionworks_mek_stress.cloud_title")));
                }
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Coefficient.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> { buffer.writeInt(message.menuId()); buffer.writeFloat(message.value()); })
            .decoder(buffer -> new Coefficient(buffer.readInt(), buffer.readFloat()))
            .consumerMainThread((message, context) -> {
                var player = context.get().getSender();
                if (player != null && player.containerMenu instanceof CrossDimensionTransportMenu menu
                    && menu.containerId == message.menuId()) menu.setCoefficient(player, message.value());
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Channel.class, 1, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> { buffer.writeInt(message.menuId()); buffer.writeInt(message.value()); })
            .decoder(buffer -> new Channel(buffer.readInt(), buffer.readInt()))
            .consumerMainThread((message, context) -> {
                var player = context.get().getSender();
                if (player != null && player.containerMenu instanceof CrossDimensionTransportMenu menu
                    && menu.containerId == message.menuId()) menu.setChannel(player, message.value());
                context.get().setPacketHandled(true);
            }).add();
    }

    public static void setChannel(int menuId, int value) {
        CHANNEL.sendToServer(new Channel(menuId, value));
    }

    public static void setCoefficient(int menuId, float value) {
        CHANNEL.sendToServer(new Coefficient(menuId, value));
    }

    public static void openCloudDisk() { CHANNEL.sendToServer(new OpenCloudDisk()); }
}
