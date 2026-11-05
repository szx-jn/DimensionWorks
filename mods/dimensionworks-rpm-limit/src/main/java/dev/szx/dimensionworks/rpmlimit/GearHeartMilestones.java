package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Permanent progression, separate from the currently equipped heart's adjustable quotas. */
public final class GearHeartMilestones {
    public static final String STAGE_TWO = "dimensionworks_gear_heart_stage_two";
    private GearHeartMilestones() {}

    public static boolean reachedStageTwo(CompoundTag data) {
        return data.getCompound(Player.PERSISTED_NBT_TAG).getBoolean(STAGE_TWO);
    }

    public static void record(CompoundTag data, ItemStack heart) {
        if (reachedStageTwo(data) || !GearHeartState.isGearHeart(heart)) return;
        boolean finished = GearHeartState.isBlessed(heart) && !GearHeartState.isLegacyCursed(heart);
        int mask = GearHeartState.getRepairedMask(heart);
        if (finished || (mask & (GearHeartState.ROTATION_SPEED | GearHeartState.CREATE_CAPACITY))
                == (GearHeartState.ROTATION_SPEED | GearHeartState.CREATE_CAPACITY)) {
            CompoundTag stable = data.getCompound(Player.PERSISTED_NBT_TAG);
            stable.putBoolean(STAGE_TWO, true);
            data.put(Player.PERSISTED_NBT_TAG, stable);
        }
    }

    public static void copy(CompoundTag original, CompoundTag replacement) {
        if (!reachedStageTwo(original)) return;
        CompoundTag stable = replacement.getCompound(Player.PERSISTED_NBT_TAG);
        stable.putBoolean(STAGE_TWO, true);
        replacement.put(Player.PERSISTED_NBT_TAG, stable);
    }
}
