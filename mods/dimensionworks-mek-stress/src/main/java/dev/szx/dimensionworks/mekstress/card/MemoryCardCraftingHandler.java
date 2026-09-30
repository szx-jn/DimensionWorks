package dev.szx.dimensionworks.mekstress.card;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import dev.szx.dimensionworks.mekstress.MekStressConfig;
import dev.szx.dimensionworks.mekstress.core.MemoryCardCrafting;
import dev.szx.dimensionworks.mekstress.core.MemoryCardType;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Adds the defective byproduct without changing the four-card recipe contract. */
public final class MemoryCardCraftingHandler {
    private static final Map<Player, PendingCraft> PENDING_QUICK_MOVES = new WeakHashMap<>();

    private MemoryCardCraftingHandler() {
    }

    public static void prepareQuickMove(Player player, Slot resultSlot) {
        if (!isEligible(player)) {
            return;
        }
        ItemStack result = resultSlot.getItem();
        if (!(result.getItem() instanceof MemoryCardItem card) || !card.type().craftable()) {
            return;
        }
        int defects = MemoryCardCrafting.defectiveCount(
            result.getCount(), MekStressConfig.defectiveChance(), player.getRandom()::nextDouble);
        PENDING_QUICK_MOVES.put(player, new PendingCraft(card, defects));
    }

    public static void finishQuickMove(Player player) {
        PENDING_QUICK_MOVES.remove(player);
    }

    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (!isEligible(player)) {
            PENDING_QUICK_MOVES.remove(player);
            return;
        }

        PendingCraft pending = PENDING_QUICK_MOVES.remove(player);
        if (pending != null) {
            applyQuickMoveResult(player, event.getCrafting(), pending);
            return;
        }
        applyCarriedResult(player, event.getCrafting());
    }

    private static void applyCarriedResult(Player player, ItemStack crafted) {
        if (!(crafted.getItem() instanceof MemoryCardItem card) || !card.type().craftable()) {
            return;
        }
        int total = crafted.getCount();
        int defects = MemoryCardCrafting.defectiveCount(
            total, MekStressConfig.defectiveChance(), player.getRandom()::nextDouble);
        if (defects <= 0) {
            return;
        }
        crafted.setCount(total - defects);
        giveDefective(player, card, defects);
    }

    private static void applyQuickMoveResult(Player player, ItemStack leftover, PendingCraft pending) {
        int defectsInLeftover = Math.min(pending.defects, leftover.getCount());
        leftover.setCount(leftover.getCount() - defectsInLeftover);
        giveDefective(player, pending.card, defectsInLeftover);

        int remainingDefects = pending.defects - defectsInLeftover;
        if (remainingDefects <= 0) {
            return;
        }
        removeNormalCards(player, pending.card, remainingDefects);
        giveDefective(player, pending.card, remainingDefects);
    }

    private static void removeNormalCards(Player player, MemoryCardItem card, int count) {
        Inventory inventory = player.getInventory();
        int remaining = count;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() != card) {
                continue;
            }
            ItemStack removed = inventory.removeItem(slot, remaining);
            remaining -= removed.getCount();
        }
    }

    private static void giveDefective(Player player, MemoryCardItem normalCard, int count) {
        if (count <= 0) {
            return;
        }
        Item defective = DimensionWorksMekStress
            .memoryCard(normalCard.tier(), MemoryCardType.DEFECTIVE).get();
        ItemStack stack = new ItemStack(defective, count);
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static boolean isEligible(Player player) {
        return player != null
            && player.level() != null
            && !player.level().isClientSide
            && !(player instanceof FakePlayer);
    }

    private static final class PendingCraft {
        private final MemoryCardItem card;
        private final int defects;

        private PendingCraft(MemoryCardItem card, int defects) {
            this.card = card;
            this.defects = defects;
        }
    }
}
