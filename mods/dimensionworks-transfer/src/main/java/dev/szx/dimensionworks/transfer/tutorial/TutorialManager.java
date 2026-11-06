package dev.szx.dimensionworks.transfer.tutorial;

import dev.szx.dimensionworks.transfer.WDKeys;
import dev.szx.dimensionworks.transfer.access.TransferAccess;
import dev.szx.dimensionworks.transfer.handler.TransferController;
import dev.szx.dimensionworks.transfer.network.TransferNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

/** Owns the one-time Gear Heart to logistics-dimension tutorial. */
public final class TutorialManager {
    private static final String STAGE_TWO = "dimensionworks_gear_heart_stage_two";
    private static final String SAVED_HAND = "Hand";
    private static final String SAVED_SLOT = "Slot";
    private static final String USED = "Used";
    private static final String DONE = "Done";
    private static final ResourceLocation KEY_ID = ResourceLocation.fromNamespaceAndPath("kubejs", "dimension_key");
    private static final ResourceLocation HEART_ID = ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart");
    private static final ResourceLocation BLESSED_HEART_ID =
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_enchanted");

    private TutorialManager() {}

    public static boolean locked(ServerPlayer player) {
        CompoundTag state = TutorialSavedData.get(player).get(player.getUUID());
        return state != null && !state.getBoolean(DONE);
    }

    /** Starts the tutorial exactly once after stage two has been permanently recorded. */
    public static void check(ServerPlayer player) {
        TutorialSavedData savedData = TutorialSavedData.get(player);
        CompoundTag state = savedData.get(player.getUUID());
        if (state != null || player.tickCount % 20 != 0) {
            return;
        }

        boolean milestone = player.getPersistentData()
            .getCompound(Player.PERSISTED_NBT_TAG)
            .getBoolean(STAGE_TWO);
        if (milestone || hasReachedStageTwo(player)) {
            begin(player, savedData);
        }
    }

    /** Keeps all player actions frozen until the key successfully moves them into dw:transfer. */
    public static void tick(ServerPlayer player) {
        TutorialSavedData savedData = TutorialSavedData.get(player);
        CompoundTag state = savedData.get(player.getUUID());
        if (state == null || state.getBoolean(DONE)) {
            return;
        }

        if (player.level().dimension().equals(WDKeys.TRANSFER_LEVEL) && state.getBoolean(USED)) {
            finish(player, state, savedData);
            return;
        }

        player.getInventory().selected = state.getInt(SAVED_SLOT);
        if (!isKey(player.getMainHandItem())) {
            player.setItemInHand(InteractionHand.MAIN_HAND, createKey());
        }
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0;

        if (player.tickCount % 40 == 0) {
            TransferNetwork.tutorial(player, true);
        }

        double x = state.getDouble("X");
        double y = state.getDouble("Y");
        double z = state.getDouble("Z");
        if (player.distanceToSqr(x, y, z) > 0.01) {
            player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        }
    }

    public static boolean isKey(ItemStack stack) {
        return KEY_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    private static ItemStack createKey() {
        var item = ForgeRegistries.ITEMS.getValue(KEY_ID);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Right-click use is the only operation allowed while locked. */
    public static boolean use(ServerPlayer player) {
        ItemStack usedKey = player.getMainHandItem();
        if (!isKey(usedKey)) {
            return false;
        }

        TutorialSavedData savedData = TutorialSavedData.get(player);
        CompoundTag state = savedData.get(player.getUUID());
        boolean alreadyUnlocked = TransferAccess.hasUnlockTag(player);
        player.addTag(TransferAccess.UNLOCK_TAG);
        if (state != null && !state.getBoolean(DONE)) {
            state.putBoolean(USED, true);
            savedData.setDirty();
        }

        if (!TransferController.enterWithKey(player)) {
            if (!alreadyUnlocked) {
                player.removeTag(TransferAccess.UNLOCK_TAG);
            }
            if (state != null) {
                state.putBoolean(USED, false);
                savedData.setDirty();
            }
            return false;
        }

        if (state == null || state.getBoolean(DONE)) {
            usedKey.shrink(1);
        }
        // The right-click/respawn inventory synchronization must finish before restoring Hand.
        // tick() owns restoration on the following server tick, once the player is in the plot.
        return true;
    }

    private static void begin(ServerPlayer player, TutorialSavedData savedData) {
        ItemStack key = createKey();
        if (key.isEmpty()) {
            return;
        }

        CompoundTag state = new CompoundTag();
        state.put(SAVED_HAND, player.getMainHandItem().save(new CompoundTag()));
        state.putInt(SAVED_SLOT, player.getInventory().selected);
        state.putDouble("X", player.getX());
        state.putDouble("Y", player.getY());
        state.putDouble("Z", player.getZ());
        savedData.put(player.getUUID(), state);

        player.setItemInHand(InteractionHand.MAIN_HAND, key);
        player.closeContainer();
        TransferNetwork.tutorial(player, true);
    }

    private static boolean hasReachedStageTwo(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
            .resolve()
            .map(inventory -> inventory.findFirstCurio(stack -> {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (BLESSED_HEART_ID.equals(itemId)) {
                    return !stack.hasTag() || stack.getTag().getInt("CustomModelData") != 2;
                }
                if (!HEART_ID.equals(itemId) || !stack.hasTag()) {
                    return false;
                }
                int repairedMask = stack.getTag().getInt("DimensionWorksGearHeartRepaired");
                return (repairedMask & 3) == 3;
            }).isPresent())
            .orElse(false);
    }

    private static void finish(ServerPlayer player, CompoundTag state, TutorialSavedData savedData) {
        if (state.getBoolean(DONE) || !player.level().dimension().equals(WDKeys.TRANSFER_LEVEL)) {
            return;
        }

        player.getInventory().selected = state.getInt(SAVED_SLOT);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.of(state.getCompound(SAVED_HAND)));
        player.getInventory().setChanged();
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.sendAllDataToRemote();
        state.remove(SAVED_HAND);
        state.putBoolean(DONE, true);
        savedData.setDirty();
        TransferNetwork.tutorial(player, false);
    }
}
