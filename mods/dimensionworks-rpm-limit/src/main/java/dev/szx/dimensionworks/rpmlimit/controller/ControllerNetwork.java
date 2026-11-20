package dev.szx.dimensionworks.rpmlimit.controller;

import dev.szx.dimensionworks.rpmlimit.DimensionWorksRpmLimit;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ControllerNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(SetTarget.TYPE, SetTarget.CODEC,
            (packet, context) -> packet.apply((ServerPlayer) context.player()));
    }

    public static void submit(BlockPos pos, int value) {
        PacketDistributor.sendToServer(new SetTarget(pos, value));
    }

    private record SetTarget(BlockPos pos, int value) implements CustomPacketPayload {
        static final Type<SetTarget> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DimensionWorksRpmLimit.MOD_ID, "controller_target"));
        static final StreamCodec<RegistryFriendlyByteBuf, SetTarget> CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeBlockPos(packet.pos()); buffer.writeInt(packet.value()); },
            buffer -> new SetTarget(buffer.readBlockPos(), buffer.readInt()));
        @Override public Type<SetTarget> type() { return TYPE; }

        void apply(ServerPlayer player) {
            if (!player.mayBuild() || !(player.containerMenu instanceof AdvancedControllerMenu menu)
                || !menu.pos.equals(pos) || !player.level().hasChunkAt(pos) || !menu.stillValid(player)
                || value < -AdvancedControllerEntity.maximumTarget() || value > AdvancedControllerEntity.maximumTarget()) return;
            if (player.level().getBlockEntity(pos) instanceof AdvancedControllerEntity entity) {
                if (RpmLimitManager.owner(entity) == null) RpmLimitManager.setOwner(entity, player.getUUID());
                entity.targetSpeed.setValue(value);
            }
        }
    }
    private ControllerNetwork() {}
}
