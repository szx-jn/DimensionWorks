package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.mixin.ChunkMapAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.UUID;

/** Applies updated player RPM limits across currently loaded chunks without holding world state. */
final class LoadedKineticScanner {
    private LoadedKineticScanner() {
    }

    static int applyLimitToOwned(ServerPlayer player, int sanitizedLimit) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }

        UUID playerId = player.getUUID();
        int changed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (ChunkHolder holder : ((ChunkMapAccessor) level.getChunkSource().chunkMap).dimensionworks$getChunks()) {
                LevelChunk chunk = holder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).orElse(null);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof KineticBlockEntity kinetic)) {
                        continue;
                    }
                    if (!playerId.equals(KineticClampService.directOwner(kinetic))) {
                        continue;
                    }
                    if (KineticClampService.setMachineLimit(kinetic, sanitizedLimit)) {
                        kinetic.setChanged();
                        kinetic.sendData();
                        changed++;
                    }
                }
            }
        }
        return changed;
    }
}
