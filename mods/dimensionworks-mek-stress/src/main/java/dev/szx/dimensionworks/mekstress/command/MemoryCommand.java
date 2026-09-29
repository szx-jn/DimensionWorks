package dev.szx.dimensionworks.mekstress.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkSnapshot;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import java.util.Comparator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public final class MemoryCommand {
    private MemoryCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dw")
            .then(Commands.literal("memory")
                .executes(MemoryCommand::status)));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        MemoryDriveBlockEntity nearest = null;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz);
                if (chunk == null) {
                    continue;
                }
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof MemoryDriveBlockEntity drive
                        && (nearest == null || drive.getBlockPos().distSqr(origin) < nearest.getBlockPos().distSqr(origin))) {
                        nearest = drive;
                    }
                }
            }
        }
        if (nearest == null || nearest.getMainNode().getGrid() == null) {
            source.sendFailure(Component.translatable("command.dimensionworks_mek_stress.memory.no_grid"));
            return 0;
        }
        IMemoryGridService service = nearest.getMainNode().getGrid().getService(IMemoryGridService.class);
        if (service == null) {
            source.sendFailure(Component.translatable("command.dimensionworks_mek_stress.memory.no_service"));
            return 0;
        }
        MemoryNetworkSnapshot snapshot = service.snapshot(level.getGameTime());
        source.sendSuccess(() -> Component.translatable("command.dimensionworks_mek_stress.memory.status",
            snapshot.status().name(),
            snapshot.driveCount(),
            snapshot.storedSu(),
            snapshot.capacitySu(),
            snapshot.bandwidthRpm(),
            snapshot.demandRpm(),
            snapshot.demandSuPerTick(),
            String.format("%.4f", snapshot.finalQ())), false);
        return 1;
    }
}
