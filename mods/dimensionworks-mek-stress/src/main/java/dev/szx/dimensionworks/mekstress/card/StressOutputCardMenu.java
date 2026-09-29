package dev.szx.dimensionworks.mekstress.card;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

public final class StressOutputCardMenu extends AbstractContainerMenu {

    private static final int DATA_RPM = 0;
    private static final int DATA_STRESS_LOW = 1;
    private static final int DATA_STRESS_HIGH = 2;

    private final Inventory inventory;
    private final InteractionHand hand;
    private final boolean clientSide;
    private final ContainerData data = new ContainerData() {
        private int syncedRpm;
        private int syncedStressLow;
        private int syncedStressHigh;

        @Override
        public int get(int index) {
            if (!clientSide) {
                StressOutputSettings settings = currentSettings();
                return switch (index) {
                    case DATA_RPM -> settings.rpm();
                    case DATA_STRESS_LOW -> (int) settings.stressPerTick();
                    case DATA_STRESS_HIGH -> (int) (settings.stressPerTick() >>> Integer.SIZE);
                    default -> 0;
                };
            }
            return switch (index) {
                case DATA_RPM -> syncedRpm;
                case DATA_STRESS_LOW -> syncedStressLow;
                case DATA_STRESS_HIGH -> syncedStressHigh;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (!clientSide) {
                return;
            }
            switch (index) {
                case DATA_RPM -> syncedRpm = value;
                case DATA_STRESS_LOW -> syncedStressLow = value;
                case DATA_STRESS_HIGH -> syncedStressHigh = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public StressOutputCardMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readEnum(InteractionHand.class));
    }

    public StressOutputCardMenu(int id, Inventory inventory, InteractionHand hand) {
        this(id, inventory, hand, true);
    }

    private StressOutputCardMenu(int id, Inventory inventory, InteractionHand hand, boolean addDataSlots) {
        super(DimensionWorksMekStress.STRESS_OUTPUT_CARD_MENU.get(), id);
        this.inventory = inventory;
        this.hand = hand;
        this.clientSide = inventory.player.level().isClientSide();
        if (addDataSlots) {
            addDataSlots(data);
        }
    }

    public int rpm() {
        return data.get(DATA_RPM);
    }

    public long stressPerTick() {
        return Integer.toUnsignedLong(data.get(DATA_STRESS_LOW))
            | (Integer.toUnsignedLong(data.get(DATA_STRESS_HIGH)) << Integer.SIZE);
    }

    public InteractionHand hand() {
        return hand;
    }

    public boolean applyFromPlayer(Player player, int requestedRpm, long requestedStress) {
        if (player != inventory.player) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof StressOutputCardItem card)) {
            return false;
        }
        card.setSettings(stack, new StressOutputSettings(requestedRpm, requestedStress));
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == inventory.player
            && player.getItemInHand(hand).getItem() instanceof StressOutputCardItem;
    }

    private StressOutputSettings currentSettings() {
        ItemStack stack = inventory.player.getItemInHand(hand);
        if (stack.getItem() instanceof StressOutputCardItem card) {
            return card.getSettings(stack);
        }
        return new StressOutputSettings(0, 0L);
    }
}
