package dev.szx.dimensionworks.transfer.api;

import net.minecraft.world.entity.player.Player;

/** Read-only lock query decoupling tutorial progression from elastic, dispatch, and network layers. */
@FunctionalInterface
public interface ITutorialLockPolicy {
    boolean isLocked(Player player);
}
