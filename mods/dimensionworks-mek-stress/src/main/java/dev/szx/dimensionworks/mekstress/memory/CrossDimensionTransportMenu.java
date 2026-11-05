package dev.szx.dimensionworks.mekstress.memory;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.blockentity.CrossDimensionTransportBlockEntity;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

/** Server-authoritative configuration and status dashboard for one loaded transport endpoint. */
public final class CrossDimensionTransportMenu extends AbstractContainerMenu {
    private final CrossDimensionTransportBlockEntity endpoint;
    private final Player player;
    private final BlockPos position;
    private final ContainerData data;

    public CrossDimensionTransportMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, blockEntity(inventory, buffer.readBlockPos()));
    }

    private static CrossDimensionTransportBlockEntity blockEntity(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof CrossDimensionTransportBlockEntity endpoint
            ? endpoint : null;
    }

    public CrossDimensionTransportMenu(int containerId, Inventory inventory,
                                       CrossDimensionTransportBlockEntity endpoint) {
        super(DimensionWorksMekStress.CROSS_DIMENSION_TRANSPORT_MENU.get(), containerId);
        this.endpoint = endpoint;
        this.player = inventory.player;
        this.position = endpoint == null ? BlockPos.ZERO : endpoint.getBlockPos().immutable();
        int[] values = new int[12];
        boolean clientSide = inventory.player.level().isClientSide;
        data = new ContainerData() {
            @Override public int get(int index) {
                if (clientSide || endpoint == null) return values[index];
                ChannelStockSavedData.StockSnapshot stock = endpoint.getLevel() instanceof net.minecraft.server.level.ServerLevel server
                    && endpoint.channel() > 0 ? ChannelStockSavedData.get(server).snapshot(endpoint.channel()) : null;
                return switch (index) {
                    case 0 -> endpoint.channel();
                    case 1 -> endpoint.itemMode();
                    case 2 -> endpoint.fluidMode();
                    case 3 -> endpoint.powerMode().ordinal();
                    case 4 -> Math.round(endpoint.stressPerRpm() * 100.0F);
                    case 5 -> endpoint.priority();
                    case 6 -> endpoint.tier();
                    case 7 -> Math.round(endpoint.getGeneratedSpeed());
                    case 8 -> (int) Math.min(Integer.MAX_VALUE, Math.round(endpoint.calculateAddedStressCapacity()));
                    case 9 -> stock == null ? 0 : stock.items().size();
                    case 10 -> stock == null ? 0 : stock.fluidAmount();
                    case 11 -> stock == null ? 0 : stock.itemUpgrades() + stock.fluidUpgrades();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { if (clientSide) values[index] = value; }
            @Override public int getCount() { return values.length; }
        };
        addDataSlots(data);
    }

    public int channel() { return data.get(0); }
    public int itemMode() { return data.get(1); }
    public int fluidMode() { return data.get(2); }
    public int powerMode() { return data.get(3); }
    public float coefficient() { return data.get(4) / 100.0F; }
    public int priority() { return data.get(5); }
    public int tier() { return data.get(6); }
    public int outputRpm() { return data.get(7); }
    public int outputCapacity() { return data.get(8); }
    public int itemKinds() { return data.get(9); }
    public int fluidAmount() { return data.get(10); }
    public int upgradesUsed() { return data.get(11); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (player != this.player || !(player instanceof ServerPlayer serverPlayer) || endpoint == null
            || !stillValid(player) || !endpoint.mayManage(player.getUUID())) return false;
        if (id >= 1001 && id <= 2024) {
            int target = id - 1000;
            boolean joined = endpoint.setChannel(serverPlayer, target);
            if (!joined) serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "无权加入此频道，或频道编号无效"), true);
            return joined;
        }
        switch (id) {
            case 0 -> endpoint.cyclePowerMode();
            case 1 -> endpoint.cycleItemMode();
            case 2 -> endpoint.cycleFluidMode();
            case 3 -> endpoint.setStressPerRpm(endpoint.stressPerRpm() - (endpoint.stressPerRpm() < 10 ? 0.25F : 1.0F));
            case 4 -> endpoint.setStressPerRpm(endpoint.stressPerRpm() + (endpoint.stressPerRpm() < 10 ? 0.25F : 1.0F));
            case 5 -> endpoint.cyclePriority();
            default -> { return false; }
        }
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return endpoint != null && !endpoint.isRemoved() && endpoint.getLevel() == player.level()
            && endpoint.getBlockPos().distToCenterSqr(player.position()) <= 64.0D;
    }
}
