package dev.szx.dimensionworks.rpmlimit.diagnostics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
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
import net.minecraft.world.level.block.entity.BlockEntity;
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
        new ResourceLocation(DimensionWorksRpmLimit.MOD_ID, "topology"), () -> "3", "3"::equals, "3"::equals);
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrenches"));
    private static final Map<ServerPlayer, Long> LAST_REQUEST = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerPlayer, Long> LAST_ACTION = Collections.synchronizedMap(new WeakHashMap<>());
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
        CHANNEL.registerMessage(2, Action.class, Action::encode, Action::decode, Action::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
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
        if (event.getLevel().isClientSide) request(event.getPos(), -1);
        else if (event.getEntity() instanceof ServerPlayer player) send(player, event.getPos(), -1);
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
        // The entry remains the reach-check anchor; focus may be its virtual member or input shaft.
        NetworkSnapshot data = NetworkSnapshot.capture(kinetic, page, pos);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), data);
    }
    public static void request(BlockPos pos, int page) { CHANNEL.sendToServer(new Request(pos, page)); }
    public static void dismantle(BlockPos origin, BlockPos target, long networkId) {
        CHANNEL.sendToServer(new Action(origin, target, networkId, Action.DISMANTLE));
    }
    public static void teleport(BlockPos origin, BlockPos target, long networkId) {
        CHANNEL.sendToServer(new Action(origin, target, networkId, Action.TELEPORT));
    }

    private static void handleAction(ServerPlayer player, Action action) {
        if (player == null || !player.level().hasChunkAt(action.origin())
            || player.distanceToSqr(action.origin().getX() + .5, action.origin().getY() + .5,
                action.origin().getZ() + .5) > 64) return;
        long tick = player.server.getTickCount();
        Long last = LAST_ACTION.get(player);
        if (last != null && tick - last < 10) return;
        LAST_ACTION.put(player, tick);

        BlockEntity originEntity = player.level().getBlockEntity(action.origin());
        KineticBlockEntity origin = resolve(originEntity);
        if (origin == null || !origin.hasNetwork()) return;
        KineticNetwork network = origin.getOrCreateNetwork();
        if (network.id == null || network.id.longValue() != action.networkId()) return;
        if (!player.level().hasChunkAt(action.target())) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("目标区块尚未加载，无法操作"), true);
            return;
        }
        BlockEntity targetEntity = player.level().getBlockEntity(action.target());
        KineticBlockEntity target = resolve(targetEntity);
        if (!supported(targetEntity) || target == null
            || !network.members.containsKey(target) && !network.sources.containsKey(target)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("目标部件已不属于此网络"), true);
            return;
        }

        if (action.operation() == Action.DISMANTLE) {
            if (!(targetEntity instanceof KineticBlockEntity)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("虚拟部件不能直接拆除"), true);
                return;
            }
            if (!wrench(player.getMainHandItem())) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("拆除操作需要手持扳手"), true);
                return;
            }
            if (player.distanceToSqr(action.target().getX() + .5, action.target().getY() + .5,
                action.target().getZ() + .5) > 64) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("请先传送到目标附近再拆除"), true);
                return;
            }
            if (!player.gameMode.destroyBlock(action.target()))
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("当前模式无法拆除该部件"), true);
            return;
        }
        if (action.operation() == Action.TELEPORT) {
            for (int dy = 1; dy <= 8 && action.target().getY() + dy < player.level().getMaxBuildHeight(); dy++) {
                double x = action.target().getX() + .5;
                double y = action.target().getY() + dy;
                double z = action.target().getZ() + .5;
                var destination = player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
                if (player.level().noCollision(player, destination)) {
                    player.teleportTo(x, y, z);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("已传送到目标部件附近"), true);
                    return;
                }
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("目标上方没有安全的落脚位置"), true);
        }
    }

    private record Action(BlockPos origin, BlockPos target, long networkId, int operation) {
        private static final int DISMANTLE = 0;
        private static final int TELEPORT = 1;
        void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(origin); buffer.writeBlockPos(target); buffer.writeLong(networkId);
            buffer.writeVarInt(operation);
        }
        static Action decode(FriendlyByteBuf buffer) {
            return new Action(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readLong(), buffer.readVarInt());
        }
        void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                if (operation == DISMANTLE || operation == TELEPORT)
                    handleAction(context.getSender(), this);
            });
            context.setPacketHandled(true);
        }
    }
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
