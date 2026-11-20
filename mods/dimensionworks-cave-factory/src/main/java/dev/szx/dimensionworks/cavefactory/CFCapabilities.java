package dev.szx.dimensionworks.cavefactory;

import com.simibubi.create.AllBlockEntityTypes;
import dev.szx.dimensionworks.cavefactory.blockentity.FactoryMachineBlockEntity;
import dev.szx.dimensionworks.cavefactory.compat.create.CreateModuleHost;
import dev.szx.dimensionworks.cavefactory.compat.create.CreateModuleHostProvider;
import dev.szx.dimensionworks.cavefactory.item.FluidAmpouleItem;
import dev.szx.dimensionworks.cavefactory.logic.ModuleHost;
import dev.szx.dimensionworks.cavefactory.registry.CFBlockEntities;
import dev.szx.dimensionworks.cavefactory.registry.CFItems;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

final class CFCapabilities {
    static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
        NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DimensionWorksCaveFactory.MOD_ID);
    private static final java.util.function.Supplier<AttachmentType<CreateModuleHost>> CREATE_HOST = ATTACHMENTS.register(
        "create_module_host", () -> AttachmentType.builder(holder -> {
            BlockEntity owner = (BlockEntity) holder;
            return new CreateModuleHost(owner, CreateModuleHostProvider.capabilityFor(owner));
        }).build());

    private CFCapabilities() {}

    static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ModuleHost.CAPABILITY, CFBlockEntities.CONTROLLER.get(), (owner, side) -> owner);
        event.registerBlockEntity(ModuleHost.CAPABILITY, CFBlockEntities.STABILIZER.get(), (owner, side) -> owner);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CFBlockEntities.CONTROLLER.get(), FactoryMachineBlockEntity::itemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CFBlockEntities.CONTROLLER.get(), FactoryMachineBlockEntity::fluidHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CFBlockEntities.STABILIZER.get(), FactoryMachineBlockEntity::itemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CFBlockEntities.STABILIZER.get(), FactoryMachineBlockEntity::fluidHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CFBlockEntities.PORT.get(), (owner, side) -> owner.itemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CFBlockEntities.PORT.get(), (owner, side) -> owner.fluidHandler(side));
        for (var type : java.util.List.of(AllBlockEntityTypes.MECHANICAL_MIXER.get(), AllBlockEntityTypes.MECHANICAL_PRESS.get(),
                AllBlockEntityTypes.SAW.get(), AllBlockEntityTypes.MILLSTONE.get(), AllBlockEntityTypes.CRUSHING_WHEEL_CONTROLLER.get())) {
            event.registerBlockEntity(ModuleHost.CAPABILITY, type, (owner, side) -> owner.getData(CREATE_HOST));
        }
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new FluidAmpouleItem.AmpouleFluidHandler(stack),
            CFItems.FLUID_AMPOULE.get());
    }
}
