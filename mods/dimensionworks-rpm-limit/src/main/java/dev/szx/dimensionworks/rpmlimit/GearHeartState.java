package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/** Shared item/state rules for the KubeJS Gear Heart items. */
public final class GearHeartState {

    public static final String SLOT_ID = "gear_heart";
    public static final String REPAIRED_MASK_KEY = "DimensionWorksGearHeartRepaired";
    public static final int REPAIR_STAGES = 7;

    public static final int ROTATION_SPEED = 1;
    public static final int CREATE_CAPACITY = 1 << 1;
    public static final int MEK_ORE_OPERATIONS = 1 << 2;
    public static final int AE_CRAFTING_JOBS = 1 << 3;
    public static final int CROSS_PROCESS = 1 << 4;
    public static final int ACTIVE_MACHINES = 1 << 5;
    public static final int FINAL_PRODUCTION = 1 << 6;
    public static final int ALL_CURSES = ROTATION_SPEED | CREATE_CAPACITY | MEK_ORE_OPERATIONS
        | AE_CRAFTING_JOBS | CROSS_PROCESS | ACTIVE_MACHINES | FINAL_PRODUCTION;

    private static final ResourceLocation NORMAL_ID =
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart");
    private static final ResourceLocation ENCHANTED_ID =
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_enchanted");
    private static final ResourceLocation[] REPAIR_FRAGMENT_IDS = {
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_repair_fragment_1"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_repair_fragment_2"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_repair_fragment_3"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_repair_fragment_4"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_repair_fragment_5"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_repair_fragment_6"),
        ResourceLocation.fromNamespaceAndPath("kubejs", "gear_heart_repair_fragment_7")
    };
    private static final int[] REPAIR_FRAGMENT_BITS = {
        ROTATION_SPEED,
        CREATE_CAPACITY,
        MEK_ORE_OPERATIONS,
        AE_CRAFTING_JOBS,
        CROSS_PROCESS,
        ACTIVE_MACHINES,
        FINAL_PRODUCTION
    };

    private GearHeartState() {}

    public static boolean isGearHeart(ItemStack stack) {
        return stack != null && !stack.isEmpty() && isGearHeart(stack.getItem());
    }

    public static boolean isGearHeart(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return NORMAL_ID.equals(id) || ENCHANTED_ID.equals(id);
    }

    public static boolean isEnchantedGearHeart(ItemStack stack) {
        return isItem(stack, ENCHANTED_ID);
    }

    public static boolean isNormalGearHeart(ItemStack stack) {
        return isItem(stack, NORMAL_ID);
    }

    public static boolean isCursed(ItemStack stack) {
        return isDamagedGearHeart(stack);
    }

    public static boolean isBlessed(ItemStack stack) {
        return isEnchantedGearHeart(stack);
    }

    public static boolean isDamagedGearHeart(ItemStack stack) {
        return isItem(stack, NORMAL_ID);
    }

    public static ItemStack createUnrepairedCursedHeart() {
        Item item = BuiltInRegistries.ITEM.getOptional(NORMAL_ID).orElse(null);
        return item == null ? ItemStack.EMPTY : createUnrepairedCursedHeart(item);
    }

    static ItemStack createUnrepairedCursedHeart(Item item) {
        ItemStack heart = new ItemStack(item);
        heart.set(DataComponents.CUSTOM_DATA, CustomData.of(unrepairedCursedHeartTag()));
        heart.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        return heart;
    }

    static boolean shouldFillEmptySlot(ItemStack current) {
        return current == null || shouldFillEmptySlot(current.isEmpty());
    }

    static boolean shouldFillEmptySlot(boolean empty) {
        return empty;
    }

    static CompoundTag unrepairedCursedHeartTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(REPAIRED_MASK_KEY, 0);
        return tag;
    }

    public static int fragmentIndexFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (int i = 0; i < REPAIR_FRAGMENT_IDS.length; i++) {
            if (REPAIR_FRAGMENT_IDS[i].equals(itemId)) {
                return i + 1;
            }
        }
        return 0;
    }

    public static int repairBitForFragment(ItemStack stack) {
        int index = fragmentIndexFor(stack);
        return index == 0 ? 0 : REPAIR_FRAGMENT_BITS[index - 1];
    }

    public static int nextRepairIndex(int repairedMask) {
        int normalized = normalizeMask(repairedMask);
        for (int index = 1; index <= REPAIR_FRAGMENT_BITS.length; index++) {
            if ((normalized & REPAIR_FRAGMENT_BITS[index - 1]) == 0) {
                return index;
            }
        }
        return 0;
    }

    public static boolean canRepair(int repairedMask, int fragmentIndex) {
        return fragmentIndex >= 1
            && fragmentIndex <= REPAIR_FRAGMENT_BITS.length
            && nextRepairIndex(repairedMask) == fragmentIndex;
    }

    public static int repairStage(int repairedMask) {
        int next = nextRepairIndex(repairedMask);
        return next == 0 ? REPAIR_STAGES : next - 1;
    }

    public static int rotationLimit(int repairedMask, int initialRpm, int releasedRpm) {
        int stageRpm = switch (repairStage(repairedMask)) {
            case 0 -> initialRpm;
            case 1 -> 512;
            case 2 -> 1024;
            case 3 -> 2048;
            case 4 -> 4096;
            case 5 -> 5120;
            case 6 -> 8192;
            default -> releasedRpm;
        };
        return Math.max(initialRpm, Math.min(stageRpm, releasedRpm));
    }

    public static boolean isFullyReleasedMask(int repairedMask) {
        return normalizeMask(repairedMask) == ALL_CURSES;
    }

    public static boolean isFullyReleased(ItemStack stack) {
        return isBlessed(stack)
            || isFullyReleasedMask(getRepairedMask(stack));
    }

    public static boolean isRepairApplied(int repairedMask, int repairBit) {
        return (normalizeMask(repairedMask) & repairBit) != 0;
    }

    public static int normalizeMask(int repairedMask) {
        return repairedMask & ALL_CURSES;
    }

    public static ItemStack reopenCurse(ItemStack current, int curseIndex) {
        if (current == null || current.isEmpty() || curseIndex < 1 || curseIndex > 7
            || !isGearHeart(current)) {
            return ItemStack.EMPTY;
        }

        Item normalItem = BuiltInRegistries.ITEM.getOptional(NORMAL_ID).orElse(null);
        if (normalItem == null) {
            return ItemStack.EMPTY;
        }

        int bit = REPAIR_FRAGMENT_BITS[curseIndex - 1];
        ItemStack reopened = current.transmuteCopy(normalItem, 1);
        setRepairedMask(reopened, getRepairedMask(current) & ~bit);
        reopened.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        return reopened;
    }

    public static ItemStack finalizeRepair(ItemStack repaired) {
        Item item = BuiltInRegistries.ITEM.getOptional(ENCHANTED_ID).orElse(null);
        if (item == null) {
            return repaired;
        }

        ItemStack finalStack = repaired.transmuteCopy(item, 1);
        finalStack.remove(DataComponents.CUSTOM_MODEL_DATA);
        setRepairedMask(finalStack, ALL_CURSES);
        return finalStack;
    }

    public static boolean isCurseActive(ItemStack stack, int curseMask) {
        return isCursed(stack) && (getRepairedMask(stack) & curseMask) == 0;
    }

    public static int getRepairedMask(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag == null || !tag.contains(REPAIRED_MASK_KEY)) {
            return 0;
        }
        return normalizeMask(tag.getInt(REPAIRED_MASK_KEY));
    }

    public static void setRepairedMask(ItemStack stack, int mask) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(REPAIRED_MASK_KEY, normalizeMask(mask)));
    }

    private static boolean isItem(ItemStack stack, ResourceLocation expected) {
        return stack != null && !stack.isEmpty()
            && expected.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
