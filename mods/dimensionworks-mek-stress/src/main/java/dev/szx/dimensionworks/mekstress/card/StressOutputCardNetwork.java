package dev.szx.dimensionworks.mekstress.card;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class StressOutputCardNetwork {

    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(DimensionWorksMekStress.MOD_ID, "stress_output_card"))
        .networkProtocolVersion(() -> VERSION)
        .clientAcceptedVersions(VERSION::equals)
        .serverAcceptedVersions(VERSION::equals)
        .simpleChannel();

    private StressOutputCardNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ApplySettings.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ApplySettings::encode)
            .decoder(ApplySettings::new)
            .consumerMainThread((message, context) -> message.handle(context.get()))
            .add();
    }

    public static void sendSettings(int containerId, int rpm, long stressPerTick) {
        CHANNEL.sendToServer(new ApplySettings(containerId, rpm, stressPerTick));
    }

    private record ApplySettings(int containerId, int rpm, long stressPerTick) {

        private ApplySettings(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarLong());
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(rpm);
            buffer.writeVarLong(stressPerTick);
        }

        private void handle(net.minecraftforge.network.NetworkEvent.Context context) {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu.containerId == containerId
                && player.containerMenu instanceof StressOutputCardMenu menu) {
                menu.applyFromPlayer(player, rpm, stressPerTick);
            }
            context.setPacketHandled(true);
        }
    }
}
