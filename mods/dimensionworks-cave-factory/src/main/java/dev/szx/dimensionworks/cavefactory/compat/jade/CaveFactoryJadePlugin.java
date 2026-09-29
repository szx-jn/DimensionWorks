package dev.szx.dimensionworks.cavefactory.compat.jade;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.block.FactoryControllerBlock;
import dev.szx.dimensionworks.cavefactory.block.FluidStabilizerBlock;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryMachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(DimensionWorksCaveFactory.MOD_ID)
public final class CaveFactoryJadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID = DimensionWorksCaveFactory.id("factory_status");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new FactoryStatusProvider(), FactoryControllerBlock.class);
        registration.registerBlockComponent(new FactoryStatusProvider(), FluidStabilizerBlock.class);
    }

    private static final class FactoryStatusProvider implements IBlockComponentProvider {
        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof FactoryMachineBlockEntity machine)) {
                return;
            }
            tooltip.add(Component.translatable(
                "jade.dimensionworks_cave_factory.status",
                machine.structureValid() ? Component.translatable("jade.dimensionworks_cave_factory.valid")
                    : Component.translatable("jade.dimensionworks_cave_factory.invalid"),
                Math.round(Math.abs(machine.effectiveRpm())),
                machine.phase().name()
            ));
            if (machine.redstoneLocked()) {
                tooltip.add(Component.translatable("jade.dimensionworks_cave_factory.locked"));
            }
        }
    }
}
