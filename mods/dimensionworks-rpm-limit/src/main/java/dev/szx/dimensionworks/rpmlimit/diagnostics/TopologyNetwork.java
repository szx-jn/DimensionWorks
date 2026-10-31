package dev.szx.dimensionworks.rpmlimit.diagnostics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import dev.szx.dimensionworks.rpmlimit.DimensionWorksRpmLimit;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TopologyNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(DimensionWorksRpmLimit.MOD_ID, "topology"), () -> "1", "1"::equals, "1"::equals);
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrenches"));
    private static final Map<ServerPlayer, Long> LAST_REQUEST = Collections.synchronizedMap(new WeakHashMap<>());
    private static final java.util.List<Adapter> ADAPTERS = new java.util.ArrayList<>();
    private record Adapter(java.util.function.Predicate<net.minecraft.world.level.block.entity.BlockEntity> matches,
                           java.util.function.Function<net.minecraft.world.level.block.entity.BlockEntity, KineticBlockEntity> resolve) {}
    public static void registerAdapter(java.util.function.Predicate<net.minecraft.world.level.block.entity.BlockEntity> matches,
                                       java.util.function.Function<net.minecraft.world.level.block.entity.BlockEntity, KineticBlockEntity> resolve) {
        ADAPTERS.add(new Adapter(matches, resolve));
    }
    private static boolean supported(net.minecraft.world.level.block.entity.BlockEntity entity) {
        return entity instanceof KineticBlockEntity || entity != null && ADAPTERS.stream().anyMatch(a -> a.matches.test(entity));
    }
    private static KineticBlockEntity resolve(net.minecraft.world.level.block.entity.BlockEntity entity) {
        if (entity instanceof KineticBlockEntity kinetic) return kinetic;
        for (Adapter adapter : ADAPTERS) if (adapter.matches.test(entity)) return adapter.resolve.apply(entity);
        return null;
    }
    private TopologyNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, Request.class, Request::encode, Request::decode, Request::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, NetworkSnapshot.class, NetworkSnapshot::encode, NetworkSnapshot::decode,
            TopologyNetwork::receive, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    private static boolean wrench(ItemStack stack) { return stack.getItem() instanceof WrenchItem || stack.is(WRENCHES); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getEntity().isShiftKeyDown() || !wrench(event.getEntity().getMainHandItem())
            || !event.getLevel().hasChunkAt(event.getPos())
            || !supported(event.getLevel().getBlockEntity(event.getPos()))) return;
        // Cancel on both logical sides, including creative mode, before block damage starts.
        event.setCanceled(true);
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (event.getLevel().isClientSide) request(event.getPos(), 0);
        else if (event.getEntity() instanceof ServerPlayer player) send(player, event.getPos(), 0);
    }

    private static void send(ServerPlayer player, BlockPos pos, int page) {
        if (!wrench(player.getMainHandItem()) || !player.level().hasChunkAt(pos)
            || player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 64) return;
        net.minecraft.world.level.block.entity.BlockEntity target = player.level().getBlockEntity(pos);
        if (!supported(target)) return;
        long tick = player.server.getTickCount();
        Long last = LAST_REQUEST.get(player);
        if (last != null && tick - last < 10) return;
        LAST_REQUEST.put(player, tick);
        KineticBlockEntity kinetic = resolve(target);
        if (kinetic == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("该机械当前没有接入应力网络"), true);
            return;
        }
        NetworkSnapshot data = NetworkSnapshot.capture(kinetic, page);
        // Keep the actual clicked target for reach validation on later page/refresh requests.
        data = new NetworkSnapshot(pos, data.id(), data.time(), data.page(), data.total(), data.unloadedMembers(), data.rpmLimit(),
            data.capacity(), data.stress(), data.unloadedCapacity(), data.unloadedStress(), data.loadedCapacity(), data.loadedStress(), data.nodes());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), data);
    }
    public static void request(BlockPos pos, int page) { CHANNEL.sendToServer(new Request(pos, page)); }
    private record Request(BlockPos pos, int page) {
        void encode(FriendlyByteBuf b) { b.writeBlockPos(pos); b.writeVarInt(page); }
        static Request decode(FriendlyByteBuf b) { return new Request(b.readBlockPos(), b.readVarInt()); }
        void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> { if (context.getSender() != null) send(context.getSender(), pos, page); });
            context.setPacketHandled(true);
        }
    }
    private static void receive(NetworkSnapshot snapshot, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TopologyScreen.show(snapshot)));
        context.setPacketHandled(true);
    }
}
