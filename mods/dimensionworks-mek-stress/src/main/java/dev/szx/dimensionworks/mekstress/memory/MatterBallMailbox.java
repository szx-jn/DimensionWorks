package dev.szx.dimensionworks.mekstress.memory;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Online and offline deliveries always go to the owner's persistent cloud disk. */
public final class MatterBallMailbox {
    private MatterBallMailbox() {}

    public static void deliver(MinecraftServer server, UUID owner, List<ItemStack> balls) {
        if (balls.isEmpty()) return;
        CloudDiskSavedData.get(server).deposit(owner, balls);
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player != null) notifyPending(player);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) notifyPending(player);
    }

    private static void notifyPending(ServerPlayer player) {
        int notices = CloudDiskSavedData.get(player.server).consumeNotices(player.getUUID());
        for (int i = 0; i < notices; i++) player.displayClientMessage(
            Component.translatable("message.dimensionworks_mek_stress.cloud_delivery").withStyle(ChatFormatting.RED), false);
    }
}
