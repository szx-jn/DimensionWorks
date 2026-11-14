package dev.szx.dimensionworks.transfer.dispatch;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.territory.Territory;
import dev.szx.dimensionworks.transfer.territory.TerritoryManager;
import dev.szx.dimensionworks.transfer.territory.TerritorySavedData;
import dev.szx.dimensionworks.transfer.tutorial.TutorialManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DispatchUpgradeItem extends Item {
    public DispatchUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(DispatchBlocks.TERMINAL.get())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.FAIL;
        Territory territory = TerritoryManager.getTerritory(player);
        if (!level.dimension().equals(WDKeys.TRANSFER_LEVEL) || territory == null
            || !DispatchController.terminal(territory).equals(pos)
            || player.distanceToSqr(Vec3.atCenterOf(pos)) > 36 || TutorialManager.locked(player)) {
            player.displayClientMessage(Component.translatable("message.dimensionworks_transfer.upgrade_not_owner"), true);
            return InteractionResult.FAIL;
        }
        TerritorySavedData data = TerritorySavedData.get(player.server.overworld());
        if (data.alexCavesUnlocked(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.dimensionworks_transfer.upgrade_already_installed"), true);
            return InteractionResult.FAIL;
        }
        data.unlockAlexCaves(player.getUUID());
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("message.dimensionworks_transfer.upgrade_installed"), true);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.2F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dimensionworks_transfer.twilight_dispatch_upgrade")
            .withStyle(ChatFormatting.GRAY));
    }
}
