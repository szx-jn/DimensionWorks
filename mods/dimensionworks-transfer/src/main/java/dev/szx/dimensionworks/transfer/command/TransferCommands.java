package dev.szx.dimensionworks.transfer.command;

import dev.szx.dimensionworks.transfer.WDTransfer;
import dev.szx.dimensionworks.transfer.handler.TransferController;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Registers administrative commands for player transfer. */
@EventBusSubscriber(modid = WDTransfer.MOD_ID)
public final class TransferCommands {
    private TransferCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dwtransfer")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                return TransferController.useAdmin(player) ? 1 : 0;
            }));
    }
}
