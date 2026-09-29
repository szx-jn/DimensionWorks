package dev.szx.dimensionworks.mekstress.integration.jade;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.StressFormula;
import dev.szx.dimensionworks.mekstress.api.StressPoweredMachine;
import dev.szx.dimensionworks.mekstress.core.StressEnergyBuffer;
import dev.szx.dimensionworks.mekstress.core.StressRules;
import mekanism.common.integration.lookingat.LookingAtUtils;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.WorldUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(DimensionWorksMekStress.MOD_ID)
public final class MekStressJadePlugin implements IWailaPlugin {

    static final String STORED_SU_KEY = "dimensionworks_mek_stress_stored_su";
    static final String MAX_SU_KEY = "dimensionworks_mek_stress_max_su";
    private static final ResourceLocation DATA_UID =
        new ResourceLocation(DimensionWorksMekStress.MOD_ID, "stored_su");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MekStressJadeDataProvider.INSTANCE, TileEntityMekanism.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(MekStressJadePlugin::replaceEnergyDisplay);
    }

    private static void replaceEnergyDisplay(ITooltip tooltip, snownee.jade.api.Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) {
            return;
        }
        CompoundTag serverData = blockAccessor.getServerData();
        if (!serverData.contains(STORED_SU_KEY)) {
            return;
        }
        tooltip.remove(LookingAtUtils.ENERGY);
        tooltip.add(Component.translatable(
            "jade.dimensionworks_mek_stress.stored_su",
            serverData.getLong(STORED_SU_KEY),
            serverData.getLong(MAX_SU_KEY)
        ).withStyle(ChatFormatting.GRAY));
    }

    private enum MekStressJadeDataProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return DATA_UID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            TileEntityMekanism machine = resolveMachine(accessor);
            if (machine == null || !StressRules.canAcceptStress(machine)) {
                return;
            }
            StressEnergyBuffer buffer = ((StressPoweredMachine) machine)
                .dimensionworks$stressEnergyBuffer();
            if (buffer == null) {
                return;
            }
            double joulesPerSu = MekStressConfig.joulesPerSu();
            data.putLong(
                STORED_SU_KEY,
                StressFormula.joulesToStressFloor(buffer.dimensionworks$storedJoules(), joulesPerSu)
            );
            data.putLong(
                MAX_SU_KEY,
                StressFormula.joulesToStressFloor(buffer.dimensionworks$capacityJoules(), joulesPerSu)
            );
        }

        private static TileEntityMekanism resolveMachine(BlockAccessor accessor) {
            BlockEntity blockEntity = accessor.getBlockEntity();
            if (blockEntity instanceof TileEntityBoundingBlock bounding
                && bounding.hasReceivedCoords()
                && !accessor.getPosition().equals(bounding.getMainPos())) {
                BlockPos mainPos = bounding.getMainPos();
                blockEntity = WorldUtils.getTileEntity(accessor.getLevel(), mainPos);
            }
            return blockEntity instanceof TileEntityMekanism machine ? machine : null;
        }
    }
}
