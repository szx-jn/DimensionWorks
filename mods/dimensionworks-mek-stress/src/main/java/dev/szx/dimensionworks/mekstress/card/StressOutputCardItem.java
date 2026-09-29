package dev.szx.dimensionworks.mekstress.card;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEParts;
import appeng.items.materials.UpgradeCardItem;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.core.StressExportStrategy;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class StressOutputCardItem extends UpgradeCardItem {

    public StressOutputCardItem(Properties properties) {
        super(properties);
    }

    public static void registerExportBusUpgrade() {
        Upgrades.add(DimensionWorksMekStress.STRESS_OUTPUT_CARD.get(), AEParts.EXPORT_BUS, 1);
        StressExportStrategy.register();
    }

    public StressOutputSettings getSettings(ItemStack stack) {
        return StressOutputSettingsData.read(stack.getTag(), defaults());
    }

    public void setSettings(ItemStack stack, StressOutputSettings settings) {
        StressOutputSettingsData.write(
            stack.getOrCreateTag(),
            settings,
            MekStressConfig.CARD_MAX_RPM.get(),
            MekStressConfig.CARD_MAX_STRESS_PER_TICK.get()
        );
    }

    public StressOutputSettings defaults() {
        return new StressOutputSettings(
            MekStressConfig.CARD_DEFAULT_RPM.get(),
            MekStressConfig.CARD_DEFAULT_STRESS_PER_TICK.get()
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(
                serverPlayer,
                new SimpleMenuProvider(
                    (id, inventory, ignored) -> new StressOutputCardMenu(id, inventory, hand),
                    Component.translatable("gui.dimensionworks_mek_stress.card_settings")
                ),
                buffer -> buffer.writeEnum(hand)
            );
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        StressOutputSettings settings = getSettings(stack);
        tooltip.add(Component.translatable("tooltip.dimensionworks_mek_stress.rpm", settings.rpm())
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dimensionworks_mek_stress.stress", settings.stressPerTick())
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dimensionworks_mek_stress.export_bus_only")
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
