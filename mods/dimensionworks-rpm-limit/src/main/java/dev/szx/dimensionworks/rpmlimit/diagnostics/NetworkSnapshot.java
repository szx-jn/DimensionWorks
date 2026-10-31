package dev.szx.dimensionworks.rpmlimit.diagnostics;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import dev.szx.dimensionworks.rpmlimit.mixin.KineticNetworkAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record NetworkSnapshot(BlockPos origin, BlockPos focus, long id, long time, int page, int total,
                              int unloadedMembers, int rpmLimit, float capacity, float stress,
                              float unloadedCapacity, float unloadedStress,
                              float loadedCapacity, float loadedStress, List<Node> nodes) {
    public static final int PAGE_SIZE = 256;
    public record Node(BlockPos pos, BlockPos parent, String name, ItemStack icon, float rpm, float actualRpm,
                       int limit, float stress, float capacity, boolean overloaded, boolean virtual) {}

    public static NetworkSnapshot capture(KineticBlockEntity origin, int requestedPage) {
        return capture(origin, requestedPage, origin.getBlockPos());
    }

    public static NetworkSnapshot capture(KineticBlockEntity origin, int requestedPage, BlockPos entry) {
        KineticNetwork network = origin.hasNetwork() ? origin.getOrCreateNetwork() : null;
        List<KineticBlockEntity> members = network == null
            ? new ArrayList<>(List.of(origin)) : new ArrayList<>(network.members.keySet());
        // A source normally belongs to members as well. Keep inconsistent source-only entries visible.
        if (network != null) for (KineticBlockEntity source : network.sources.keySet())
            if (!network.members.containsKey(source)) members.add(source);
        if (!members.contains(origin)) members.add(origin);
        members.sort(Comparator.comparing(KineticBlockEntity::getBlockPos)
            .thenComparing(member -> member.getClass().getName()));
        int focusIndex = members.indexOf(origin);
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getBlockPos().equals(entry)) { focusIndex = i; break; }
        }
        BlockPos focus = members.get(focusIndex).getBlockPos();
        int desiredPage = requestedPage < 0 ? focusIndex / PAGE_SIZE : requestedPage;
        int page = Math.max(0, Math.min(desiredPage, Math.max(0, (members.size() - 1) / PAGE_SIZE)));
        List<Node> nodes = new ArrayList<>();
        float loadedStress = 0, loadedCapacity = 0;
        for (int i = 0; i < members.size(); i++) {
            KineticBlockEntity member = members.get(i);
            float stress = network != null && network.members.containsKey(member) ? network.getActualStressOf(member) : 0;
            float capacity = network != null && network.sources.containsKey(member) ? network.getActualCapacityOf(member) : 0;
            loadedStress += stress;
            loadedCapacity += capacity;
            if (i / PAGE_SIZE != page) continue;
            boolean virtual = member.getLevel() == null || !member.getLevel().hasChunkAt(member.getBlockPos())
                || member.getLevel().getBlockEntity(member.getBlockPos()) != member;
            String name = member.getBlockState().getBlock().getDescriptionId();
            nodes.add(new Node(member.getBlockPos(), member.source, name,
                new ItemStack(member.getBlockState().getBlock()), member.getTheoreticalSpeed(),
                member.getSpeed(), RpmLimitManager.individualLimit(member), stress, capacity, member.isOverStressed(), virtual));
        }
        KineticNetworkAccessor state = network == null ? null : (KineticNetworkAccessor) network;
        return new NetworkSnapshot(entry, focus, network == null || network.id == null ? 0 : network.id,
            origin.getLevel().getGameTime(), page, members.size(), state == null ? 0 : state.dimensionworks$unloadedMembers(),
            dev.szx.dimensionworks.rpmlimit.optimizer.NetworkRpmLimit.limit(origin),
            state == null ? 0 : state.dimensionworks$currentCapacity(), state == null ? 0 : state.dimensionworks$currentStress(),
            state == null ? 0 : state.dimensionworks$unloadedCapacity(), state == null ? 0 : state.dimensionworks$unloadedStress(),
            loadedCapacity, loadedStress, List.copyOf(nodes));
    }

    public void encode(FriendlyByteBuf b) {
        b.writeBlockPos(origin); b.writeBlockPos(focus); b.writeLong(id); b.writeLong(time); b.writeVarInt(page); b.writeVarInt(total);
        b.writeVarInt(unloadedMembers); b.writeVarInt(rpmLimit); b.writeFloat(capacity); b.writeFloat(stress);
        b.writeFloat(unloadedCapacity); b.writeFloat(unloadedStress); b.writeFloat(loadedCapacity); b.writeFloat(loadedStress);
        b.writeVarInt(nodes.size());
        for (Node n : nodes) {
            b.writeBlockPos(n.pos); b.writeBoolean(n.parent != null);
            if (n.parent != null) b.writeBlockPos(n.parent);
            b.writeUtf(n.name, 512); b.writeItem(n.icon); b.writeFloat(n.rpm); b.writeFloat(n.actualRpm); b.writeVarInt(n.limit);
            b.writeFloat(n.stress); b.writeFloat(n.capacity); b.writeBoolean(n.overloaded); b.writeBoolean(n.virtual);
        }
    }

    public static NetworkSnapshot decode(FriendlyByteBuf b) {
        BlockPos origin = b.readBlockPos(), focus = b.readBlockPos(); long id = b.readLong(), time = b.readLong();
        int page = b.readVarInt(), total = b.readVarInt(), unloaded = b.readVarInt(), limit = b.readVarInt();
        float capacity = b.readFloat(), stress = b.readFloat(), uc = b.readFloat(), us = b.readFloat();
        float lc = b.readFloat(), ls = b.readFloat(); int count = b.readVarInt();
        if (count < 0 || count > PAGE_SIZE || page < 0 || total < 0) throw new IllegalArgumentException("Invalid topology snapshot");
        List<Node> nodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockPos pos = b.readBlockPos(), parent = b.readBoolean() ? b.readBlockPos() : null;
            nodes.add(new Node(pos, parent, b.readUtf(512), b.readItem(), b.readFloat(), b.readFloat(), b.readVarInt(),
                b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean()));
        }
        return new NetworkSnapshot(origin, focus, id, time, page, total, unloaded, limit, capacity, stress, uc, us, lc, ls, List.copyOf(nodes));
    }
}
