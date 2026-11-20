package dev.szx.dimensionworks.transfer.mixin;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.territory.PlotChunkLoading;
import java.util.List;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class PlotChunkMapMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    private net.minecraft.server.level.ServerLevel level;
    @Inject(method = "skipPlayer", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$noRadiusTicket(ServerPlayer player, CallbackInfoReturnable<Boolean> ci) {
        if (player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) ci.setReturnValue(true);
    }

    @org.spongepowered.asm.mixin.gen.Invoker("applyChunkTrackingView")
    protected abstract void dimensionworks$applyView(ServerPlayer player, net.minecraft.server.level.ChunkTrackingView view);

    @Inject(method = "updateChunkTracking", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$fixedPlotTracking(ServerPlayer player, CallbackInfo ci) {
        if (!level.dimension().equals(WDKeys.TRANSFER_LEVEL)) return;
        var territory = dev.szx.dimensionworks.transfer.territory.TerritoryManager.getTerritory(player);
        net.minecraft.server.level.ChunkTrackingView view = territory == null
            ? net.minecraft.server.level.ChunkTrackingView.EMPTY
            : new dev.szx.dimensionworks.transfer.territory.PlotChunkView(territory.chunkX(), territory.chunkZ());
        if (!view.equals(player.getChunkTrackingView())) {
            if (territory != null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket(
                territory.chunkX() + 1, territory.chunkZ() + 1));
            dimensionworks$applyView(player, view);
        }
        ci.cancel();
    }

    @Inject(method = "getPlayers", at = @At("HEAD"), cancellable = true)
    private void dimensionworks$onlyPlotUpdates(ChunkPos chunk, boolean boundary,
            CallbackInfoReturnable<List<ServerPlayer>> ci) {
        if (level.dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            ci.setReturnValue(level.players().stream().filter(player -> PlotChunkLoading.maySend(player, chunk)).toList());
        }
    }
}
