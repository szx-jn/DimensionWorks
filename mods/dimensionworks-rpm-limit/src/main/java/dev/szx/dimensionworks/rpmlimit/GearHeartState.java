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

    public static final int ATTACK_DAMAGE = 1;
    public static final int RPM_LIMIT = 1 << 1;
    public static final int OUTPUT_FAILURE = 1 << 2;
    public static final int FREEZE = 1 << 3;
    public static final int NIGHT_HUNGER = 1 << 4;
    public static final int STRESS_COST = 1 << 5;
    public static final int STRESS_CAPACITY = 1 << 6;
    public static final int ALL_CURSES = ATTACK_DAMAGE | RPM_LIMIT | OUTPUT_FAILURE | FREEZE
        | NIGHT_HUNGER | STRESS_COST | STRESS_CAPACITY;

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
        ATTACK_DAMAGE,
        RPM_LIMIT,
        OUTPUT_FAILURE,
        FREEZE,
        NIGHT_HUNGER,
        STRESS_COST,
        STRESS_CAPACITY
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

    public static int repairBitForFragment(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return 0;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        for (int i = 0; i < REPAIR_FRAGMENT_IDS.length; i++) {
            if (REPAIR_FRAGMENT_IDS[i].equals(itemId))
                return REPAIR_FRAGMENT_BITS[i];
        }
        return 0;
    }

    public static ItemStack migrateLegacyCursed(ItemStack legacy) {
        Item item = ForgeRegistries.ITEMS.getValue(NORMAL_ID);
        if (item == null)
            return legacy.copy();

        ItemStack migrated = new ItemStack(item);
        CompoundTag tag = legacy.getTag();
        if (tag != null)
            migrated.setTag(tag.copy());
        migrated.getOrCreateTag().putInt("CustomModelData", 1);
        return migrated;
    }

    public static boolean isCurseActive(ItemStack stack, int curseMask) {
        return isCursed(stack) && (getRepairedMask(stack) & curseMask) == 0;
    }

    public static int getRepairedMask(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(REPAIRED_MASK_KEY))
            return 0;
        return tag.getInt(REPAIRED_MASK_KEY) & ALL_CURSES;
    }

    public static void setRepairedMask(ItemStack stack, int mask) {
        stack.getOrCreateTag().putInt(REPAIRED_MASK_KEY, mask & ALL_CURSES);
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
