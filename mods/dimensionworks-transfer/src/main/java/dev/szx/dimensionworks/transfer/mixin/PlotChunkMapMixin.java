package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.territory.PlotChunkLoading;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class PlotChunkMapMixin {
    @Inject(method = "skipPlayer", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$noRadiusTicket(ServerPlayer player, CallbackInfoReturnable<Boolean> ci) {
        if (player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) ci.setReturnValue(true);
    }

    @Inject(method = "updateChunkTracking", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$onlyPlotTracking(ServerPlayer player, ChunkPos chunk,
            MutableObject<ClientboundLevelChunkWithLightPacket> packet, boolean wasLoaded,
            boolean load, CallbackInfo ci) {
        if (!PlotChunkLoading.maySend(player, chunk)) ci.cancel();
    }

    @Inject(method = "playerLoadedChunk", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$onlyPlotPacket(ServerPlayer player,
            MutableObject<ClientboundLevelChunkWithLightPacket> packet, LevelChunk chunk, CallbackInfo ci) {
        if (!PlotChunkLoading.maySend(player, chunk.getPos())) ci.cancel();
    }

    @Inject(method = "getPlayers", at = @At("RETURN"), cancellable = true)
    private void dimensionworks$onlyPlotUpdates(ChunkPos chunk, boolean boundary,
            CallbackInfoReturnable<List<ServerPlayer>> ci) {
        List<ServerPlayer> players = ci.getReturnValue();
        if (players.stream().anyMatch(player -> !PlotChunkLoading.maySend(player, chunk)))
            ci.setReturnValue(players.stream().filter(player -> PlotChunkLoading.maySend(player, chunk)).toList());
    }
}
