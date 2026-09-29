package dev.szx.dimensionworks.cavefactory.compat.jei;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.logic.MachineType;
import dev.szx.dimensionworks.cavefactory.registry.CFItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public final class CaveFactoryJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return DimensionWorksCaveFactory.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(
            List.of(new ItemStack(CFItems.FACTORY_CASING.get())),
            Component.translatable("jei.dimensionworks_cave_factory.casing")
        );
        registration.addItemStackInfo(
            List.of(new ItemStack(CFItems.FACTORY_PORT.get())),
            Component.translatable("jei.dimensionworks_cave_factory.port")
        );
        registration.addItemStackInfo(
            List.of(new ItemStack(CFItems.FLUID_STABILIZER.get())),
            Component.translatable("jei.dimensionworks_cave_factory.stabilizer")
        );
        registration.addItemStackInfo(
            List.of(new ItemStack(CFItems.FLUID_AMPOULE.get())),
            Component.translatable("jei.dimensionworks_cave_factory.ampoule")
        );
        for (MachineType machine : MachineType.values()) {
            registration.addItemStackInfo(
                List.of(new ItemStack(CFItems.controllerItem(machine).get())),
                Component.translatable(machine.survivalEnabled()
                    ? "jei.dimensionworks_cave_factory.controller.active"
                    : "jei.dimensionworks_cave_factory.controller.preview")
            );
            if (!machine.survivalEnabled()) {
                registration.addItemStackInfo(
                    List.of(
                        new ItemStack(CFItems.matrix(machine).get()),
                        new ItemStack(CFItems.residue(machine).get())
                    ),
                    Component.translatable("jei.dimensionworks_cave_factory.preview_item")
                );
            }
        }
        for (dev.szx.dimensionworks.cavefactory.logic.FactoryModule module
            : dev.szx.dimensionworks.cavefactory.logic.FactoryModule.values()) {
            if (!module.implementedThisMilestone()) {
                registration.addItemStackInfo(
                    List.of(new ItemStack(CFItems.module(module).get())),
                    Component.translatable("jei.dimensionworks_cave_factory.preview_item")
                );
            }
        }
    }
}
