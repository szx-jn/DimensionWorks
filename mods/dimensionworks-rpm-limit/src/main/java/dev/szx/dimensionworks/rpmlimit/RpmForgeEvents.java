package dev.szx.dimensionworks.rpmlimit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RpmForgeEvents {
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        RpmLimitManager.init(e.getEntity());
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer p)
            RpmLimitManager.refresh(p.server.getPlayerList().getPlayers(), p.server.getTickCount());
    }

    @SubscribeEvent
    public static void place(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            BlockEntity be = e.getLevel().getBlockEntity(e.getPos());
            if (be instanceof KineticBlockEntity k)
                RpmLimitManager.setOwner(k, p.getUUID());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("dw")
            .requires(s -> s.hasPermission(2))
            .then(Commands.literal("rpm")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1, 10240))
                        .executes(c -> {
                            ServerPlayer target = EntityArgument.getPlayer(c, "player");
                            int limit = IntegerArgumentType.getInteger(c, "limit");
                            RpmLimitManager.setLimit(target, limit);
                            c.getSource().sendSuccess(
                                () -> Component.literal("Set " + target.getGameProfile().getName() + " RPM limit to " + limit + "."),
                                true
                            );
                            return 1;
                        })))));
    }
}
