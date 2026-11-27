package dev.szx.dimensionworks.transfer.tutorial;

import dev.szx.dimensionworks.transfer.WDTransfer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Enforces tutorial lock restrictions and key usage without polluting core transfer handlers. */
@EventBusSubscriber(modid = WDTransfer.MOD_ID)
public final class TutorialEvents {
    private TutorialEvents() {
    }

    @SubscribeEvent
    public static void tutorialItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (TutorialManager.locked(player)) {
            event.setCanceled(true);
            if (event.getHand() == InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) {
                TutorialManager.use(player);
            }
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) {
            event.setCanceled(true);
            TutorialManager.use(player);
        }
    }

    @SubscribeEvent
    public static void tutorialBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) {
            event.setCanceled(true);
            if (event.getHand() == InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) {
                TutorialManager.use(player);
            }
        }
    }

    @SubscribeEvent
    public static void tutorialEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) {
            event.setCanceled(true);
            if (event.getHand() == InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) {
                TutorialManager.use(player);
            }
        }
    }

    @SubscribeEvent
    public static void tutorialEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) {
            event.setCanceled(true);
            if (event.getHand() == InteractionHand.MAIN_HAND && TutorialManager.isKey(event.getItemStack())) {
                TutorialManager.use(player);
            }
        }
    }

    @SubscribeEvent
    public static void tutorialLeft(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tutorialAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tutorialDrop(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && TutorialManager.locked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TutorialManager.check(player);
            TutorialManager.tick(player);
        }
    }
}
