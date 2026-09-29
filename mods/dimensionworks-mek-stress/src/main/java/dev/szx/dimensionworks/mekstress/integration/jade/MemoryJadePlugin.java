package dev.szx.dimensionworks.mekstress.integration.jade;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.block.networking.CableBusBlock;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.parts.automation.ExportBusPart;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.api.IMemoryGridService;
import dev.szx.dimensionworks.mekstress.api.MemoryNetworkSnapshot;
import dev.szx.dimensionworks.mekstress.block.MemoryDriveBlock;
import dev.szx.dimensionworks.mekstress.blockentity.MemoryDriveBlockEntity;
import dev.szx.dimensionworks.mekstress.core.MachineTier;
import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import dev.szx.dimensionworks.mekstress.memory.MachinePowerRegistry;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import dev.szx.dimensionworks.rpmlimit.RpmLimitManager;
import java.util.UUID;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(DimensionWorksMekStress.MOD_ID)
public final class MemoryJadePlugin implements IWailaPlugin {
    private static final String DRIVE_KEY = "dw_memory_drive";
    private static final String BUS_KEY = "dw_stress_bus";
    private static final String MACHINE_KEY = "dw_mek_machine";
    private static final ResourceLocation DRIVE_UID = id("memory_drive");
    private static final ResourceLocation BUS_UID = id("stress_bus");
    private static final ResourceLocation MACHINE_UID = id("mek_machine");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DriveDataProvider.INSTANCE, MemoryDriveBlockEntity.class);
        registration.registerBlockDataProvider(BusDataProvider.INSTANCE, CableBusBlockEntity.class);
        registration.registerBlockDataProvider(MachineDataProvider.INSTANCE, TileEntityMekanism.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DriveComponent.INSTANCE, MemoryDriveBlock.class);
        registration.registerBlockComponent(BusComponent.INSTANCE, CableBusBlock.class);
        registration.registerBlockComponent(MachineComponent.INSTANCE, Block.class);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(DimensionWorksMekStress.MOD_ID, path);
    }

    private static final class DriveDataProvider implements IServerDataProvider<BlockAccessor> {
        private static final DriveDataProvider INSTANCE = new DriveDataProvider();

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MemoryDriveBlockEntity drive)
                || drive.getLevel() == null || drive.getMainNode().getGrid() == null) {
                return;
            }
            IMemoryGridService service = drive.getMainNode().getGrid().getService(IMemoryGridService.class);
            if (service == null) {
                return;
            }
            MemoryNetworkSnapshot snapshot = service.snapshot(drive.getLevel().getGameTime());
            data.putBoolean(DRIVE_KEY, true);
            data.putInt("driveCount", snapshot.driveCount());
            data.putLong("capacitySu", snapshot.capacitySu());
            data.putLong("storedSu", snapshot.storedSu());
            data.putLong("bandwidthRpm", snapshot.bandwidthRpm());
            data.putLong("demandRpm", snapshot.demandRpm());
            data.putLong("demandSu", snapshot.demandSuPerTick());
            data.putDouble("stockQ", snapshot.stockQ());
            data.putDouble("bandwidthQ", snapshot.bandwidthQ());
            data.putDouble("finalQ", snapshot.finalQ());
            data.putString("status", snapshot.status().name());
            data.putInt("cards", drive.cardCount());
        }

        @Override
        public ResourceLocation getUid() {
            return DRIVE_UID;
        }
    }

    private static final class BusDataProvider implements IServerDataProvider<BlockAccessor> {
        private static final BusDataProvider INSTANCE = new BusDataProvider();

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof CableBusBlockEntity cableBus)
                || !(cableBus.getPart(accessor.getSide()) instanceof ExportBusPart bus)
                || !StressRules.hasStressOutputCard(bus)) {
                return;
            }
            data.putBoolean(BUS_KEY, true);
            IGridNode node = bus.getGridNode();
            if (node == null || node.getGrid() == null) {
                data.putString("status", "NO_GRID");
                return;
            }
            IGrid grid = node.getGrid();
            IMemoryGridService service = grid.getService(IMemoryGridService.class);
            if (service == null) {
                data.putString("status", "NO_SERVICE");
                return;
            }
            MemoryNetworkSnapshot snapshot = service.snapshot(cableBus.getLevel().getGameTime());
            data.putString("status", snapshot.status().name());
            data.putDouble("q", snapshot.finalQ());
            data.putLong("demandRpm", snapshot.demandRpm());
            data.putLong("bandwidthRpm", snapshot.bandwidthRpm());

            BlockPos targetPos = cableBus.getBlockPos().relative(accessor.getSide());
            BlockEntity target = cableBus.getLevel().getBlockEntity(targetPos);
            if (target instanceof TileEntityMekanism machine && StressRules.isEligible(machine)) {
                MachineTier tier = StressRules.machineTier(machine);
                data.putString("machineTier", tier.name());
                data.putInt("effectiveRpm", effectiveRpm(grid, tier));
            } else {
                data.putString("machineTier", "INVALID");
            }
        }

        @Override
        public ResourceLocation getUid() {
            return BUS_UID;
        }

        private static int effectiveRpm(IGrid grid, MachineTier tier) {
            int tierRpm = MekStressConfig.machineRpm(tier);
            UUID owner = grid.getPivot() == null ? null : grid.getPivot().getOwningPlayerProfileId();
            int ownerLimit = owner == null ? tierRpm : RpmLimitManager.getLimit(owner);
            return Math.max(0, Math.min(tierRpm, ownerLimit));
        }
    }

    private static final class MachineDataProvider implements IServerDataProvider<BlockAccessor> {
        private static final MachineDataProvider INSTANCE = new MachineDataProvider();

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof TileEntityMekanism machine)
                || !StressRules.isEligible(machine) || machine.getLevel() == null) {
                return;
            }
            long tick = machine.getLevel().getGameTime();
            data.putBoolean(MACHINE_KEY, true);
            data.putString("tier", StressRules.machineTier(machine).name());
            MachinePowerRegistry.AeRoute route = MachinePowerRegistry.get(machine, tick);
            if (route != null) {
                MemoryNetworkSnapshot snapshot = route.service() == null
                    ? MemoryNetworkSnapshot.empty(dev.szx.dimensionworks.mekstress.api.MemoryNetworkStatus.NO_DRIVE)
                    : route.service().snapshot(tick);
                data.putString("route", "AE");
                data.putString("status", route.service() == null ? "NO_SERVICE" : snapshot.status().name());
                data.putInt("effectiveRpm", route.effectiveRpm());
                data.putDouble("q", route.service() == null ? 0.0D : snapshot.finalQ());
            } else {
                MachinePowerManager.DirectSource direct = MachinePowerManager.directSource(machine);
                data.putString("route", direct == null ? "NONE" : "CREATE");
                data.putInt("effectiveRpm", direct == null ? 0 : Math.round(direct.rpm()));
                data.putDouble("q", 1.0D);
                data.putString("status", direct == null ? "NO_POWER" : "OK");
            }
            data.putDouble("production", MachinePowerManager.workRate(machine, tick));
        }

        @Override
        public ResourceLocation getUid() {
            return MACHINE_UID;
        }
    }

    private static final class DriveComponent implements IBlockComponentProvider {
        private static final DriveComponent INSTANCE = new DriveComponent();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.getBoolean(DRIVE_KEY)) {
                return;
            }
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.drive",
                data.getInt("driveCount"), data.getInt("cards")).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.su",
                data.getLong("storedSu"), data.getLong("capacitySu")));
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.bandwidth",
                data.getLong("bandwidthRpm")));
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.demand",
                data.getLong("demandRpm"), data.getLong("demandSu")));
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.q",
                data.getDouble("stockQ"), data.getDouble("bandwidthQ"), data.getDouble("finalQ")));
            appendStatus(tooltip, data.getString("status"));
        }

        @Override
        public ResourceLocation getUid() {
            return DRIVE_UID;
        }
    }

    private static final class BusComponent implements IBlockComponentProvider {
        private static final BusComponent INSTANCE = new BusComponent();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.getBoolean(BUS_KEY)) {
                return;
            }
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.output_bus",
                data.getString("machineTier"), data.getInt("effectiveRpm")).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.demand",
                data.getLong("demandRpm"), 0L));
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.q",
                1.0D, 1.0D, data.getDouble("q")));
            appendStatus(tooltip, data.getString("status"));
        }

        @Override
        public ResourceLocation getUid() {
            return BUS_UID;
        }
    }

    private static final class MachineComponent implements IBlockComponentProvider {
        private static final MachineComponent INSTANCE = new MachineComponent();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.getBoolean(MACHINE_KEY)) {
                return;
            }
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.machine",
                data.getString("route"), data.getString("tier"), data.getInt("effectiveRpm"))
                .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.production",
                String.format("%.2f", data.getDouble("production"))));
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.q",
                1.0D, 1.0D, data.getDouble("q")));
            appendStatus(tooltip, data.getString("status"));
        }

        @Override
        public ResourceLocation getUid() {
            return MACHINE_UID;
        }
    }

    private static void appendStatus(ITooltip tooltip, String status) {
        if (!"OK".equals(status)) {
            tooltip.add(Component.translatable("jade.dimensionworks_mek_stress.status." + status.toLowerCase())
                .withStyle(ChatFormatting.RED));
        }
    }
}
