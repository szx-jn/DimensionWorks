package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Shared item/state rules for the KubeJS Gear Heart items. */
public final class GearHeartState {

    public static final String SLOT_ID = "gear_heart";
    public static final String REPAIRED_MASK_KEY = "DimensionWorksGearHeartRepaired";

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
        new ResourceLocation("kubejs", "gear_heart");
    private static final ResourceLocation ENCHANTED_ID =
        new ResourceLocation("kubejs", "gear_heart_enchanted");
    private static final ResourceLocation[] REPAIR_FRAGMENT_IDS = {
        new ResourceLocation("kubejs", "gear_heart_repair_fragment_1"),
        new ResourceLocation("kubejs", "gear_heart_repair_fragment_2"),
        new ResourceLocation("kubejs", "gear_heart_repair_fragment_3"),
        new ResourceLocation("kubejs", "gear_heart_repair_fragment_4"),
        new ResourceLocation("kubejs", "gear_heart_repair_fragment_5"),
        new ResourceLocation("kubejs", "gear_heart_repair_fragment_6"),
        new ResourceLocation("kubejs", "gear_heart_repair_fragment_7")
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
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
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

    public static boolean isLegacyCursed(ItemStack stack) {
        return isEnchantedGearHeart(stack) && getCustomModelData(stack) == 2;
    }

    public static int fragmentIndexFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
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
        int normalized = normalizeLegacyMask(repairedMask);
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

    public static boolean isFullyReleasedMask(int repairedMask) {
        return normalizeLegacyMask(repairedMask) == ALL_CURSES;
    }

    public static boolean isFullyReleased(ItemStack stack) {
        return isBlessed(stack) || isFullyReleasedMask(getRepairedMask(stack));
    }

    public static boolean isRepairApplied(int repairedMask, int repairBit) {
        return (normalizeLegacyMask(repairedMask) & repairBit) != 0;
    }

    public static int normalizeLegacyMask(int repairedMask) {
        return repairedMask & ALL_CURSES;
    }

    public static ItemStack finalizeRepair(ItemStack repaired) {
        Item item = ForgeRegistries.ITEMS.getValue(ENCHANTED_ID);
        if (item == null) {
            return repaired;
        }

        ItemStack finalStack = new ItemStack(item);
        CompoundTag tag = repaired.getTag();
        if (tag != null) {
            finalStack.setTag(tag.copy());
        }
        finalStack.getOrCreateTag().remove("CustomModelData");
        setRepairedMask(finalStack, ALL_CURSES);
        return finalStack;
    }

    public static ItemStack migrateLegacyCursed(ItemStack legacy) {
        Item item = ForgeRegistries.ITEMS.getValue(NORMAL_ID);
        if (item == null) {
            return legacy.copy();
        }

        ItemStack migrated = new ItemStack(item);
        CompoundTag tag = legacy.getTag();
        if (tag != null) {
            migrated.setTag(tag.copy());
        }
        migrated.getOrCreateTag().putInt(
            REPAIRED_MASK_KEY, normalizeLegacyMask(legacy.getOrCreateTag().getInt(REPAIRED_MASK_KEY)));
        migrated.getOrCreateTag().putInt("CustomModelData", 1);
        return migrated;
    }

    public static boolean isCurseActive(ItemStack stack, int curseMask) {
        return isCursed(stack) && (getRepairedMask(stack) & curseMask) == 0;
    }

    public static int getRepairedMask(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(REPAIRED_MASK_KEY)) {
            return 0;
        }
        return normalizeLegacyMask(tag.getInt(REPAIRED_MASK_KEY));
    }

    public static void setRepairedMask(ItemStack stack, int mask) {
        stack.getOrCreateTag().putInt(REPAIRED_MASK_KEY, normalizeLegacyMask(mask));
    }

    public static int getCustomModelData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt("CustomModelData");
    }

    private static boolean isItem(ItemStack stack, ResourceLocation expected) {
        return stack != null && !stack.isEmpty()
            && expected.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
