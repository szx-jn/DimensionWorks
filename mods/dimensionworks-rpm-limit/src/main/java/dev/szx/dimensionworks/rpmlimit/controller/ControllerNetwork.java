package dev.szx.dimensionworks.rpmlimit.controller;

import dev.szx.dimensionworks.rpmlimit.DimensionWorksRpmLimit;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;
import java.util.function.Supplier;

public final class ControllerNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(DimensionWorksRpmLimit.MOD_ID, "controller"), () -> "1", "1"::equals, "1"::equals);
    public static void register() {
        CHANNEL.registerMessage(0, SetTarget.class, SetTarget::encode, SetTarget::decode, SetTarget::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void submit(BlockPos pos, int value) { CHANNEL.sendToServer(new SetTarget(pos, value)); }
    private record SetTarget(BlockPos pos, int value) {
        void encode(FriendlyByteBuf buffer) { buffer.writeBlockPos(pos); buffer.writeInt(value); }
        static SetTarget decode(FriendlyByteBuf buffer) { return new SetTarget(buffer.readBlockPos(), buffer.readInt()); }
        void handle(Supplier<NetworkEvent.Context> context) {
            var ctx = context.get();
            ctx.enqueueWork(() -> {
                var player = ctx.getSender();
                if (player == null || !player.mayBuild()
                    || !(player.containerMenu instanceof AdvancedControllerMenu menu)
                    || !menu.pos.equals(pos) || !player.level().hasChunkAt(pos) || !menu.stillValid(player)
                    || value < -AdvancedControllerEntity.maximumTarget() || value > AdvancedControllerEntity.maximumTarget()) return;
                if (player.level().getBlockEntity(pos) instanceof AdvancedControllerEntity entity) {
                    if (RpmLimitManager.owner(entity) == null) RpmLimitManager.setOwner(entity, player.getUUID());
                    entity.targetSpeed.setValue(value);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
    private ControllerNetwork() {}
}
