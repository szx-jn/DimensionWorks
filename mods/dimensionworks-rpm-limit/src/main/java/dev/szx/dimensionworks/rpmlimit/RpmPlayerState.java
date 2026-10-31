package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/** Durable player state and migration of offline profiles whose transient ForgeData was lost. */
public final class RpmPlayerState {
    static final String LIMIT = "dimensionworks_rpm_limit";
    static final String INITIALIZED = "dimensionworks_rpm_initialized";
    static final String OVERRIDE = "dimensionworks_rpm_command_override";
    private RpmPlayerState() {}
    public record Profile(int base, int effective, boolean commandOverride) {}

    public static void persist(CompoundTag data) {
        CompoundTag stable = data.getCompound(Player.PERSISTED_NBT_TAG);
        for (String key : new String[]{LIMIT, INITIALIZED, OVERRIDE}) {
            if (data.contains(key)) stable.put(key, data.get(key).copy());
        }
        data.put(Player.PERSISTED_NBT_TAG, stable);
    }
    public static void restore(CompoundTag data) {
        CompoundTag stable = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (stable.contains(LIMIT, Tag.TAG_ANY_NUMERIC)) {
            for (String key : new String[]{LIMIT, INITIALIZED, OVERRIDE}) {
                if (stable.contains(key)) data.put(key, stable.get(key).copy());
            }
            data.putBoolean(INITIALIZED, true);
        }
    }
    public static void copy(CompoundTag original, CompoundTag replacement) {
        restore(original);
        for (String key : new String[]{LIMIT, INITIALIZED, OVERRIDE}) {
            if (original.contains(key)) replacement.put(key, original.get(key).copy());
        }
        persist(replacement);
    }
    public static Profile fromPlayerNbt(CompoundTag player) {
        CompoundTag data = player.getCompound("ForgeData").copy();
        restore(data);
        boolean override = data.getBoolean(OVERRIDE);
        int base = data.contains(LIMIT, Tag.TAG_ANY_NUMERIC) ? cap(data.getInt(LIMIT)) : RpmLimitConfig.DEFAULT_RPM.get();
        CompoundTag heart = equippedHeart(player);
        CompoundTag heartTag = heart.getCompound("tag");
        boolean enchanted = "kubejs:gear_heart_enchanted".equals(heart.getString("id"));
        boolean legacyCurse = enchanted && heartTag.getInt("CustomModelData") == 2;
        int repairedMask = heartTag.getInt(GearHeartState.REPAIRED_MASK_KEY);
        boolean fullyRepaired = enchanted && !legacyCurse || GearHeartState.isFullyReleasedMask(repairedMask);
        boolean repaired = !heart.isEmpty() && (fullyRepaired
            || GearHeartState.isRepairApplied(repairedMask, GearHeartState.ROTATION_SPEED));
        if (repaired && !override) base = Math.max(base, RpmLimitConfig.GEAR_HEART_ROTATION_REPAIRED_RPM.get());
        int effective = base;
        if (!override && !heart.isEmpty() && !fullyRepaired) {
            effective = Math.min(base, repaired ? RpmLimitConfig.GEAR_HEART_ROTATION_REPAIRED_RPM.get()
                : RpmLimitConfig.GEAR_HEART_ROTATION_CURSED_RPM.get());
        }
        return new Profile(cap(base), cap(effective), override);
    }
    private static CompoundTag equippedHeart(CompoundTag player) {
        ListTag slots = player.getCompound("ForgeCaps").getCompound("curios:inventory").getList("Curios", Tag.TAG_COMPOUND);
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag slot = slots.getCompound(i);
            if (!GearHeartState.SLOT_ID.equals(slot.getString("Identifier"))) continue;
            ListTag items = slot.getCompound("StacksHandler").getCompound("Stacks").getList("Items", Tag.TAG_COMPOUND);
            for (int j = 0; j < items.size(); j++) {
                CompoundTag item = items.getCompound(j);
                String id = item.getString("id");
                if (item.getByte("Count") > 0 && ("kubejs:gear_heart".equals(id) || "kubejs:gear_heart_enchanted".equals(id))) return item;
            }
        }
        return new CompoundTag();
    }
    private static int cap(int value) { return Math.max(1, Math.min(value, RpmLimitConfig.MAX_RPM.get())); }
}
