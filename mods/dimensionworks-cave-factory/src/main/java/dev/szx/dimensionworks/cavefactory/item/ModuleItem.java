package dev.szx.dimensionworks.cavefactory.item;

import dev.szx.dimensionworks.cavefactory.logic.FactoryModule;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModuleItem extends Item {
    private final FactoryModule module;

    public ModuleItem(FactoryModule module, Properties properties) {
        super(properties);
        this.module = module;
    }

    public FactoryModule module() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dimensionworks_cave_factory.module."
            + module.kind().name().toLowerCase()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dimensionworks_cave_factory.module."
            + module.id()).withStyle(ChatFormatting.DARK_AQUA));
        if (!module.implementedThisMilestone()) {
            tooltip.add(Component.translatable("tooltip.dimensionworks_cave_factory.module.preview")
                .withStyle(ChatFormatting.GRAY));
        }
    }
}
