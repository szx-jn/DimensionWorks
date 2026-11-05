package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Menu-scoped settings: the server checks distance, identity and channel ownership. */
public final class TransportNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(DimensionWorksMekStress.MOD_ID, "transport"), () -> "1", "1"::equals, "1"::equals);
    private record Coefficient(int menuId, float value) {}
    private record Channel(int menuId, int value) {}
    private TransportNetwork() {}

    public static void register() {
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
}
