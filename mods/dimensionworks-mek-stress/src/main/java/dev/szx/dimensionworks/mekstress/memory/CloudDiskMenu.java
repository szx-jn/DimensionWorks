package dev.szx.dimensionworks.mekstress.memory;

import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Read-only cloud previews and explicit, server-authoritative inventory withdrawals. */
public final class CloudDiskMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 27;
    private final Player owner;
    private final SimpleContainer preview = new SimpleContainer(PAGE_SIZE);
    private final SimpleContainerData paging = new SimpleContainerData(2);
    private long seenRevision = -1;
    private int seenPage = -1;

    public CloudDiskMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory); }

    public CloudDiskMenu(int id, Inventory inventory) {
        super(DimensionWorksMekStress.CLOUD_DISK_MENU.get(), id);
        owner = inventory.player;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(preview, row * 9 + column, 8 + column * 18, 38 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                    @Override public boolean mayPickup(Player player) { return false; }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 126 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 184));
        addDataSlots(paging);
        refresh();
    }

    public int page() { return paging.get(0); }
    public int pages() { return Math.max(1, paging.get(1)); }

    private void refresh() {
        if (!(owner instanceof ServerPlayer player)) return;
        CloudDiskSavedData cloud = CloudDiskSavedData.get(player.server);
        if (seenRevision == cloud.revision() && seenPage == page()) return;
        int pages = Math.max(1, (cloud.size(player.getUUID()) + PAGE_SIZE - 1) / PAGE_SIZE);
        paging.set(1, pages);
        paging.set(0, Math.min(page(), pages - 1));
        for (int slot = 0; slot < PAGE_SIZE; slot++)
            preview.setItem(slot, preview(cloud.view(player.getUUID(), page() * PAGE_SIZE + slot)));
        seenRevision = cloud.revision();
        seenPage = page();
    }

    private static ItemStack preview(ItemStack original) {
        if (original.isEmpty() || !original.hasTag() || !original.getTag().contains("MatterBall")) return original;
        ItemStack icon = new ItemStack(original.getItem(), original.getCount());
        var payload = original.getTag().getCompound("MatterBall");
        var lore = new net.minecraft.nbt.ListTag();
        lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.translatable(
            "gui.dimensionworks_mek_stress.cloud_contents",
            payload.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).size(),
            payload.getList("Fluids", net.minecraft.nbt.Tag.TAG_COMPOUND).size()))));
        var display = new net.minecraft.nbt.CompoundTag();
        display.put("Lore", lore);
        icon.getOrCreateTag().put("display", display);
        return icon;
    }

    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }

    @Override public boolean stillValid(Player player) { return player == owner && player.isAlive(); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer) || !stillValid(player) || player.containerMenu != this) return false;
        if (id == 0 && page() > 0) paging.set(0, page() - 1);
        else if (id == 1 && page() + 1 < pages()) paging.set(0, page() + 1);
        else return false;
        broadcastChanges();
        return true;
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot >= 0 && slot < PAGE_SIZE) {
            if ((type == ClickType.PICKUP || type == ClickType.QUICK_MOVE) && (button == 0 || button == 1)) claim(player, slot);
            return;
        }
        super.clicked(slot, button, type, player);
    }

    private void claim(Player player, int slot) {
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || player.containerMenu != this) return;
        if (!getCarried().isEmpty()) return;
        boolean moved = CloudDiskSavedData.get(serverPlayer.server).claim(player.getUUID(), page() * PAGE_SIZE + slot,
            stack -> player.getInventory().add(stack));
        if (!moved && !preview.getItem(slot).isEmpty()) player.displayClientMessage(
            Component.translatable("message.dimensionworks_mek_stress.cloud_inventory_full"), true);
        player.getInventory().setChanged();
        refresh();
        sendAllDataToRemote();
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) {
        if (slot >= 0 && slot < PAGE_SIZE) claim(player, slot);
        return ItemStack.EMPTY;
    }
}
