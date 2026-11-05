package dev.szx.dimensionworks.mekstress.blockentity;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import dev.szx.dimensionworks.mekstress.block.CrossDimensionTransportBlock;
import dev.szx.dimensionworks.mekstress.core.ChannelDirectory;
import dev.szx.dimensionworks.mekstress.core.StressTransferMath;
import dev.szx.dimensionworks.mekstress.memory.ChannelDirectorySavedData;
import dev.szx.dimensionworks.mekstress.memory.ChannelStockSavedData;
import dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportManager;
import dev.szx.dimensionworks.mekstress.memory.MatterBallMailbox;
import dev.szx.dimensionworks.mekstress.memory.MatterBallPacking;
import java.util.ArrayList;
import java.util.List;
import dev.szx.dimensionworks.mekstress.memory.TeamAccess;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Create stress input/source endpoint. The live power link is always recomputed from loaded devices. */
public final class CrossDimensionTransportBlockEntity extends GeneratingKineticBlockEntity {
    public enum PowerMode { DISABLED, INPUT, OUTPUT }

    private static final float MAX_COEFFICIENT = 1_000_000.0F;
    private final int tier;
    private int channel;
    private PowerMode powerMode = PowerMode.INPUT;
    private float stressPerRpm = 1.0F;
    private int priority = 1;
    private int itemMode;
    private int fluidMode;
    private float outputRpm;
    private float outputCapacity;
    private boolean registered;
    private final CrossDimensionItemHandler itemHandler = new CrossDimensionItemHandler(this);
    private final CrossDimensionFluidHandler fluidHandler = new CrossDimensionFluidHandler(this);
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> itemHandler);
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidHandler);

    public CrossDimensionTransportBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tier = state.getBlock() instanceof CrossDimensionTransportBlock block ? block.tier() : 1;
    }

    public int tier() { return tier; }
    public int channel() { return channel; }
    public PowerMode powerMode() { return powerMode; }
    public float stressPerRpm() { return stressPerRpm; }
    public int priority() { return priority; }
    public int itemMode() { return itemMode; }
    public int fluidMode() { return fluidMode; }
    public boolean isPowerInput() { return powerMode == PowerMode.INPUT; }
    public boolean isPowerOutput() { return powerMode == PowerMode.OUTPUT; }
    public boolean canImportItems() { return channelJoined() && itemMode == 1; }
    public boolean canExportItems() { return channelJoined() && itemMode == 2; }
    public boolean canImportFluids() { return channelJoined() && fluidMode == 1; }
    public boolean canExportFluids() { return channelJoined() && fluidMode == 2; }

    public long itemRate() { return switch (tier) { case 1 -> 50L; case 2 -> 150L; default -> 300L; }; }
    public long fluidRate() { return switch (tier) { case 1 -> 50_000L; case 2 -> 150_000L; default -> 300_000L; }; }

    public void serverTick(ServerLevel serverLevel) {
        if (!registered) {
            registered = true;
            CrossDimensionTransportManager.register(this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            registered = true;
            CrossDimensionTransportManager.register(this);
            if (level instanceof ServerLevel server && channel > 0)
                ChannelDirectorySavedData.get(server).refreshTier(endpointKey(), tier);
        }
    }

    @Override
    public void onChunkUnloaded() {
        if (registered) {
            registered = false;
            CrossDimensionTransportManager.unregister(this);
        }
        super.onChunkUnloaded();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        fluidCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> itemHandler);
        fluidCapability = LazyOptional.of(() -> fluidHandler);
    }

    @Override
    public void remove() {
        if (registered) {
            registered = false;
            CrossDimensionTransportManager.unregister(this);
        }
        if (level instanceof ServerLevel server && channel > 0) {
            ChannelDirectorySavedData data = ChannelDirectorySavedData.get(server);
            boolean last = data.removeEndpoint(endpointKey());
            if (last) returnChannelContents(server, data, channel);
        }
        super.remove();
    }

    @Override
    public float getGeneratedSpeed() {
        return isPowerOutput() ? outputRpm : 0.0F;
    }

    @Override
    public float calculateStressApplied() {
        return isPowerInput() ? stressPerRpm : 0.0F;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = isPowerOutput() ? outputCapacity : 0.0F;
        lastCapacityProvided = capacity;
        return capacity;
    }

    public void applyPowerOutput(float rpm, long su) {
        float newRpm = Float.isFinite(rpm) ? Math.max(0.0F, rpm) : 0.0F;
        float newCapacity = StressTransferMath.capacityFor(su, newRpm);
        boolean changedRpm = Float.compare(outputRpm, newRpm) != 0;
        boolean changedCapacity = Float.compare(outputCapacity, newCapacity) != 0;
        if (!changedRpm && !changedCapacity) return;
        outputRpm = newRpm;
        outputCapacity = newCapacity;
        if (changedRpm) updateGeneratedRotation();
        else if (changedCapacity) notifyStressCapacityChange(calculateAddedStressCapacity());
        setChanged();
    }

    public boolean isLive() {
        return level instanceof ServerLevel && !isRemoved() && level.hasChunkAt(worldPosition)
            && level.getBlockEntity(worldPosition) == this && (channel == 0 || channelJoined());
    }

    @Nullable public KineticNetwork powerNetwork() {
        return hasNetwork() ? getOrCreateNetwork() : null;
    }

    public float liveRpm() {
        return Math.abs(getSpeed());
    }

    public long inputSu(float rpm) {
        return StressTransferMath.paidSu(stressPerRpm, rpm);
    }

    public long outputSu(float rpm) {
        return inputSu(rpm);
    }

    public String dimensionId() {
        return level == null ? "" : level.dimension().location().toString();
    }

    public void claimInitialChannel(ServerPlayer player) {
        if (level == null || !(level instanceof ServerLevel server)) return;
        ChannelDirectorySavedData data = ChannelDirectorySavedData.get(server);
        if (channel > 0 && data.join(channel, player.getUUID(), endpointKey(), tier, TeamAccess::sameCurrentTeam)) {
            setChanged();
            sendData();
            return;
        }
        channel = 0;
        for (int candidate = 1; candidate <= 1024; candidate++) {
            if (data.getChannel(candidate) != null) continue;
            if (data.join(candidate, player.getUUID(), endpointKey(), tier, TeamAccess::sameCurrentTeam)) {
                channel = candidate;
                setChanged();
                sendData();
                player.displayClientMessage(Component.literal("跨维传输端已创建频道 " + candidate), true);
                return;
            }
        }
        player.displayClientMessage(Component.literal("跨维传输频道已满"), true);
    }

    public void openConfiguration(ServerPlayer player) {
        if (!mayManage(player.getUUID())) {
            player.displayClientMessage(Component.literal("只有频道所有者及其当前 FTB 队伍成员可以管理此端点"), true);
            return;
        }
        net.minecraftforge.network.NetworkHooks.openScreen(player, new net.minecraft.world.SimpleMenuProvider(
            (id, inventory, ignored) -> new dev.szx.dimensionworks.mekstress.memory.CrossDimensionTransportMenu(id, inventory, this),
            Component.translatable("gui.dimensionworks_mek_stress.transport.title")), buffer -> buffer.writeBlockPos(worldPosition));
    }

    public void installCapacityUpgrade(ServerPlayer player, net.minecraft.world.InteractionHand hand, boolean fluid) {
        if (level == null || !(level instanceof ServerLevel server) || !mayManage(player.getUUID()) || !channelJoined()) {
            player.displayClientMessage(Component.literal("无权管理该频道"), true);
            return;
        }
        ChannelStockSavedData stock = ChannelStockSavedData.get(server);
        if (!stock.applyUpgrade(channel, fluid)) {
            player.displayClientMessage(Component.literal("该频道的 20 个扩容槽已用完"), true);
            return;
        }
        if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
        player.displayClientMessage(Component.literal(fluid ? "流体容量 +32 B" : "物品容量 +27 格"), true);
        sendData();
    }

    public void setPowerMode(PowerMode mode) {
        if (mode == powerMode) return;
        powerMode = mode;
        if (hasNetwork()) {
            var network = getOrCreateNetwork();
            if (network != null) network.updateNetwork();
        }
        if (mode != PowerMode.OUTPUT) applyPowerOutput(0, 0);
        setChanged();
        sendData();
    }

    public boolean setChannel(ServerPlayer player, int target) {
        if (level == null || !(level instanceof ServerLevel server) || target < 1 || target > 1024) return false;
        ChannelDirectorySavedData data = ChannelDirectorySavedData.get(server);
        if (target == channel) return data.join(target, player.getUUID(), endpointKey(), tier, TeamAccess::sameCurrentTeam);
        ChannelDirectory.Endpoint ref = endpointKey();
        int previous = channel;
        ChannelDirectory.Channel previousClaim = previous > 0 ? data.getChannel(previous) : null;
        if (previous > 0 && (previousClaim == null || !previousClaim.endpoints().contains(ref))) return false;
        // Remove the live source immediately so a rebind cannot leave one free output tick.
        if (isPowerOutput()) applyPowerOutput(0, 0);
        boolean lastPrevious = previous > 0 && data.removeEndpoint(ref);
        boolean joined = data.join(target, player.getUUID(), ref, tier, TeamAccess::sameCurrentTeam);
        if (!joined) {
            if (previousClaim != null)
                data.join(previous, previousClaim.owner(), ref, tier, TeamAccess::sameCurrentTeam);
            return false;
        }
        if (lastPrevious) returnChannelContents(server, data, previous);
        channel = target;
        setChanged();
        sendData();
        return true;
    }

    public boolean mayManage(UUID player) {
        if (level == null || !(level instanceof ServerLevel server) || channel == 0) return false;
        var current = ChannelDirectorySavedData.get(server).getChannel(channel);
        return current != null && (current.owner().equals(player) || TeamAccess.sameCurrentTeam(current.owner(), player));
    }

    private boolean channelJoined() {
        if (level == null || !(level instanceof ServerLevel server) || channel == 0) return false;
        var current = ChannelDirectorySavedData.get(server).getChannel(channel);
        return current != null && current.endpoints().contains(endpointKey());
    }

    public void cyclePowerMode() { setPowerMode(PowerMode.values()[(powerMode.ordinal() + 1) % PowerMode.values().length]); }
    public void setStressPerRpm(float value) {
        float clamped = Float.isFinite(value) ? Math.max(0.01F, Math.min(MAX_COEFFICIENT, value)) : stressPerRpm;
        if (Float.compare(stressPerRpm, clamped) == 0) return;
        stressPerRpm = clamped;
        // A lower output coefficient must never leave the previous source capacity live.
        if (isPowerOutput()) applyPowerOutput(0, 0);
        if (hasNetwork()) getOrCreateNetwork().updateNetwork();
        setChanged();
        sendData();
    }
    public void cycleItemMode() { itemMode = (itemMode + 1) % 3; setChanged(); sendData(); }
    public void cycleFluidMode() { fluidMode = (fluidMode + 1) % 3; setChanged(); sendData(); }
    public void cyclePriority() { priority = (priority + 1) % 3; setChanged(); sendData(); }

    public ChannelDirectory.Endpoint endpointKey() {
        return new ChannelDirectory.Endpoint(dimensionId(), worldPosition.asLong());
    }

    private static void returnChannelContents(ServerLevel level, ChannelDirectorySavedData directory, int channelNumber) {
        var channel = directory.getChannel(channelNumber);
        if (channel == null || !channel.endpoints().isEmpty()) return;
        ChannelStockSavedData stock = ChannelStockSavedData.get(level);
        var payload = stock.snapshot(channelNumber);
        var balls = MatterBallPacking.pack(payload);
        if (balls == null) {
            dev.szx.dimensionworks.mekstress.DimensionWorksMekStress.LOGGER.error(
                "Unable to package channel {} within the configured Matter Ball NBT limit; its stock remains recoverable.",
                channelNumber);
            return;
        }
        var taken = stock.take(channelNumber);
        List<net.minecraft.world.item.ItemStack> rewards = new ArrayList<>(balls);
        if (taken.itemUpgrades() > 0)
            rewards.add(new net.minecraft.world.item.ItemStack(dev.szx.dimensionworks.mekstress.DimensionWorksMekStress.ITEM_CAPACITY_UPGRADE.get(),
                taken.itemUpgrades()));
        if (taken.fluidUpgrades() > 0)
            rewards.add(new net.minecraft.world.item.ItemStack(dev.szx.dimensionworks.mekstress.DimensionWorksMekStress.FLUID_CAPACITY_UPGRADE.get(),
                taken.fluidUpgrades()));
        directory.releaseEmpty(channelNumber);
        MatterBallMailbox.deliver(level.getServer(), channel.owner(), rewards);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("TransportChannel", channel);
        tag.putString("PowerMode", powerMode.name());
        tag.putFloat("StressPerRpm", stressPerRpm);
        tag.putInt("Priority", priority);
        tag.putInt("ItemMode", itemMode);
        tag.putInt("FluidMode", fluidMode);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        channel = Math.max(0, Math.min(1024, tag.getInt("TransportChannel")));
        try { powerMode = PowerMode.valueOf(tag.getString("PowerMode")); }
        catch (IllegalArgumentException exception) { powerMode = PowerMode.INPUT; }
        float savedCoefficient = tag.getFloat("StressPerRpm");
        stressPerRpm = Float.isFinite(savedCoefficient) ? Math.max(0.01F, Math.min(MAX_COEFFICIENT, savedCoefficient)) : 1.0F;
        priority = Math.max(0, Math.min(2, tag.getInt("Priority")));
        itemMode = Math.max(0, Math.min(2, tag.getInt("ItemMode")));
        fluidMode = Math.max(0, Math.min(2, tag.getInt("FluidMode")));
        outputRpm = 0;
        outputCapacity = 0;
    }
}
