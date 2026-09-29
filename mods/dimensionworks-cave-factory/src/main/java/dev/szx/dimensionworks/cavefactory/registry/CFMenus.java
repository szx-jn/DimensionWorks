package dev.szx.dimensionworks.cavefactory.registry;

import dev.szx.dimensionworks.cavefactory.DimensionWorksCaveFactory;
import dev.szx.dimensionworks.cavefactory.menu.FactoryMachineMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

public final class CFMenus {
    public static RegistryObject<MenuType<FactoryMachineMenu>> FACTORY_MACHINE;

    private CFMenus() {}

    public static void register() {
        FACTORY_MACHINE = DimensionWorksCaveFactory.CFRegistry.MENUS.register(
            "factory_machine",
            () -> IForgeMenuType.create(FactoryMachineMenu::new)
        );
    }

    public static MenuType<FactoryMachineMenu> factoryMachine() {
        return FACTORY_MACHINE.get();
    }
}
