package dev.szx.dimensionworks.mekstress.card;

import appeng.items.materials.UpgradeCardItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class StressOutputCardItem extends UpgradeCardItem {
    public StressOutputCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.dimensionworks_mek_stress.output_card")
            .withStyle(ChatFormatting.GRAY));
    }
}
