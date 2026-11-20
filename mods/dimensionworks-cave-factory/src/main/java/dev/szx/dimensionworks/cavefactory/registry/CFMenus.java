package dev.szx.dimensionworks.cavefactory.registry;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.menu.FactoryMachineMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CFMenus {
    public static DeferredHolder<MenuType<?>, MenuType<FactoryMachineMenu>> FACTORY_MACHINE;

    private CFMenus() {}

    public static void register() {
        FACTORY_MACHINE = DimensionWorksCaveFactory.CFRegistry.MENUS.register(
            "factory_machine",
            () -> IMenuTypeExtension.create(FactoryMachineMenu::new)
        );
    }

    public static MenuType<FactoryMachineMenu> factoryMachine() {
        return FACTORY_MACHINE.get();
    }
}
