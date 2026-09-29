package dev.szx.dimensionworks.mekstress.card;

import dev.szx.dimensionworks.mekstress.core.MemoryTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MemoryCardItem extends Item {
    private final MemoryTier tier;

    public MemoryCardItem(MemoryTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public MemoryTier tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dimensionworks_mek_stress.memory_card",
            tier.cardCapacitySu()).withStyle(ChatFormatting.GRAY));
    }
}
