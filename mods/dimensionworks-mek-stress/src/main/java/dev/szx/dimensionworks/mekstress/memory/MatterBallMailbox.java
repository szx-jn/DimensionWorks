package dev.szx.dimensionworks.mekstress.memory;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Volatile in-memory delivery queue, matching the pack's short-lived cloud-disk recovery rule. */
public final class MatterBallMailbox {
    private static final Map<UUID, List<ItemStack>> PENDING = new HashMap<>();
    private MatterBallMailbox() {}

    public static synchronized void deliver(MinecraftServer server, UUID owner, List<ItemStack> balls) {
        if (balls.isEmpty()) return;
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player != null) {
            List<ItemStack> remaining = new ArrayList<>();
            for (ItemStack ball : balls) {
                ItemStack remainder = ball.copy();
                player.getInventory().add(remainder);
                if (!remainder.isEmpty()) remaining.add(remainder);
            }
            if (!remaining.isEmpty()) PENDING.computeIfAbsent(owner, ignored -> new ArrayList<>()).addAll(remaining);
        } else {
            PENDING.computeIfAbsent(owner, ignored -> new ArrayList<>()).addAll(balls.stream().map(ItemStack::copy).toList());
        }
    }

    public static synchronized void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        List<ItemStack> pending = PENDING.get(player.getUUID());
        if (pending == null) return;
        Iterator<ItemStack> iterator = pending.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            player.getInventory().add(stack);
            if (stack.isEmpty()) iterator.remove();
        }
        if (pending.isEmpty()) PENDING.remove(player.getUUID());
    }

    public static synchronized void clearAtShutdown(net.minecraftforge.event.server.ServerStoppingEvent event) {
        PENDING.clear();
    }
}
