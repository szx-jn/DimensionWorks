package dev.szx.dimensionworks.rpmlimit.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class AdvancedControllerMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public final int target;
    public final int maximum;
    public final int effectiveMaximum;
    private final net.minecraft.world.level.Level level;

    public AdvancedControllerMenu(int id, Inventory inventory, BlockPos pos, int target, int maximum,
                                  int effectiveMaximum) {
        super(AdvancedController.MENU.get(), id);
        this.level = inventory.player.level();
        this.pos = pos.immutable();
        this.target = target;
        this.maximum = maximum;
        this.effectiveMaximum = effectiveMaximum;
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return player.level() == level
            && player.level().getBlockState(pos).is(AdvancedController.BLOCK.get())
            && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
}
