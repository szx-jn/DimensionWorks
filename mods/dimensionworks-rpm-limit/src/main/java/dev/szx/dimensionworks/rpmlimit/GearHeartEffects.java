package dev.szx.dimensionworks.rpmlimit;

import dev.szx.dimensionworks.rpmlimit.quota.FixedIntervalTokenBucket;
import dev.szx.dimensionworks.rpmlimit.quota.GearHeartQuotaType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime effects driven by the equipped Gear Heart. */
public final class GearHeartEffects {

    public enum RepairResult {
        NO_TARGET,
        ALREADY_REPAIRED,
        REPAIRED
    }

    private static final String RPM_SOURCE = "gear_heart:rpm";
    private static final String BONUS_STATE_KEY = "dimensionworks_gear_heart_bonuses";
    private static final String BONUS_VERSION_KEY = "dimensionworks_gear_heart_bonus_version";
    private static final int BONUS_VERSION = 2;
    private static final FixedIntervalTokenBucket TOKEN_BUCKET = new FixedIntervalTokenBucket();
    private static final Map<UUID, Integer> ACTIVE_QUOTA_MASKS = new ConcurrentHashMap<>();

    private static final UUID BLOCK_REACH_ID =
        UUID.fromString("61ee7d0c-7d95-4a6c-b50a-6ce2168df0a4");
    private static final UUID ENTITY_REACH_ID =
        UUID.fromString("ab7653cc-1c9d-4dbb-b58d-18f377e4ea25");

    private GearHeartEffects() {}

    public static void tickPlayer(ServerPlayer player) {
        Optional<ItemStack> maybeHeart = equippedHeart(player);
        if (maybeHeart.isEmpty()) {
            clearAll(player);
            updateQuotaMask(player.getUUID(), null);
            return;
        }

        ItemStack heart = maybeHeart.get();
        applyBaseBonuses(player);
        if (GearHeartState.isFullyReleased(heart) || !GearHeartState.isCursed(heart)) {
            clearCurses(player);
            updateQuotaMask(player.getUUID(), null);
            return;
        }

        updateQuotaMask(player.getUUID(), GearHeartState.getRepairedMask(heart));
        syncQuotaState(player, heart);
    }

    private static void updateQuotaMask(UUID owner, Integer mask) {
        Integer previous = mask == null ? ACTIVE_QUOTA_MASKS.remove(owner) : ACTIVE_QUOTA_MASKS.put(owner, mask);
        if (!java.util.Objects.equals(previous, mask))
            dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry.requestReallocation();
    }

    public static void clearPlayer(ServerPlayer player) {
        clearAll(player);
        TOKEN_BUCKET.clearOwner(player.getUUID());
        updateQuotaMask(player.getUUID(), null);
    }

    public static boolean tryQuota(UUID owner, GearHeartQuotaType type,
                                   double amount, double perSecond, long tick) {
        return TOKEN_BUCKET.tryTake(owner, type, amount, perSecond, tick);
    }

    public static int activeQuotaMask(UUID owner) {
        return owner == null ? 0 : ACTIVE_QUOTA_MASKS.getOrDefault(owner, 0);
    }

    public static boolean anyQuotaUnreleased(int quotaBit) {
        synchronized (ACTIVE_QUOTA_MASKS) {
            for (int mask : ACTIVE_QUOTA_MASKS.values()) {
                if ((mask & quotaBit) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasQuota(UUID owner) {
        return owner != null && ACTIVE_QUOTA_MASKS.containsKey(owner);
    }

    public static Set<UUID> unreleasedOwners(int quotaBit) {
        Set<UUID> owners = new LinkedHashSet<>();
        synchronized (ACTIVE_QUOTA_MASKS) {
            for (Map.Entry<UUID, Integer> entry : ACTIVE_QUOTA_MASKS.entrySet()) {
                if ((entry.getValue() & quotaBit) == 0) {
                    owners.add(entry.getKey());
                }
            }
        }
        return owners;
    }

    public static boolean tryQuotaForOwners(Collection<UUID> owners, GearHeartQuotaType type,
                                            double amount, double perSecond, long tick) {
        return TOKEN_BUCKET.tryTakeMany(owners, type, amount, perSecond, tick);
    }

    public static boolean tryQuotaPlan(Collection<FixedIntervalTokenBucket.QuotaRequest> requests) {
        return TOKEN_BUCKET.tryTakePlan(requests);
    }

    public static RepairResult repairEquippedCurse(ServerPlayer player, int repairBit) {
        if (repairBit == 0 || Integer.bitCount(repairBit) != 1) {
            return RepairResult.NO_TARGET;
        }

        Optional<ICuriosItemHandler> maybeInventory = CuriosApi.getCuriosInventory(player).resolve();
        if (maybeInventory.isEmpty()) {
            return RepairResult.NO_TARGET;
        }

        ICuriosItemHandler inventory = maybeInventory.get();
        for (SlotResult result : inventory.findCurios(GearHeartState.SLOT_ID)) {
            ItemStack stack = result.stack();
            if (GearHeartState.isFullyReleased(stack)) {
                return RepairResult.ALREADY_REPAIRED;
            }
            if (!GearHeartState.isCursed(stack)) {
                continue;
            }

            int fragmentIndex = Integer.numberOfTrailingZeros(repairBit) + 1;
            int repairedMask = GearHeartState.getRepairedMask(stack);
            if (!GearHeartState.canRepair(repairedMask, fragmentIndex)) {
                return RepairResult.ALREADY_REPAIRED;
            }

            ItemStack repaired = stack.copy();
            GearHeartState.setRepairedMask(repaired, repairedMask | repairBit);
            if (repairBit == GearHeartState.FINAL_PRODUCTION) {
                repaired = GearHeartState.finalizeRepair(repaired);
            }

            inventory.setEquippedCurio(
                result.slotContext().identifier(),
                result.slotContext().index(),
                repaired
            );
            return RepairResult.REPAIRED;
        }
        return RepairResult.NO_TARGET;
    }

    public static boolean ensureGearHeartSlots(ServerPlayer player) {
        Optional<ICuriosItemHandler> maybeInventory = CuriosApi.getCuriosInventory(player).resolve();
        if (maybeInventory.isEmpty()) {
            return false;
        }

        ICuriosItemHandler inventory = maybeInventory.get();
        var handler = inventory.getStacksHandler(GearHeartState.SLOT_ID).orElse(null);
        if (handler == null) {
            return false;
        }

        boolean changed = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack current = handler.getStacks().getStackInSlot(slot);
            if (!GearHeartState.shouldFillEmptySlot(current)) {
                continue;
            }

            ItemStack heart = GearHeartState.createUnrepairedCursedHeart();
            if (heart.isEmpty()) {
                continue;
            }
            inventory.setEquippedCurio(GearHeartState.SLOT_ID, slot, heart);
            syncQuotaState(player, heart);
            changed = true;
        }
        return changed;
    }

    private static Optional<ItemStack> equippedHeart(ServerPlayer player) {
        Optional<ICuriosItemHandler> maybeInventory = CuriosApi.getCuriosInventory(player).resolve();
        if (maybeInventory.isEmpty()) {
            return Optional.empty();
        }

        ICuriosItemHandler inventory = maybeInventory.get();
        for (SlotResult result : inventory.findCurios(GearHeartState.SLOT_ID)) {
            ItemStack stack = result.stack();
            if (!GearHeartState.isGearHeart(stack)) {
                continue;
            }
            if (GearHeartState.isLegacyCursed(stack)) {
                ItemStack migrated = GearHeartState.migrateLegacyCursed(stack);
                inventory.setEquippedCurio(
                    result.slotContext().identifier(),
                    result.slotContext().index(),
                    migrated
                );
                return Optional.of(migrated);
            }
            return Optional.of(stack);
        }
        return Optional.empty();
    }

    private static void syncQuotaState(ServerPlayer player, ItemStack heart) {
        int repairedMask = GearHeartState.getRepairedMask(heart);
        boolean rotationRepaired =
            GearHeartState.isRepairApplied(repairedMask, GearHeartState.ROTATION_SPEED);
        int rpm = rotationRepaired
            ? RpmLimitConfig.GEAR_HEART_ROTATION_REPAIRED_RPM.get()
            : RpmLimitConfig.GEAR_HEART_ROTATION_CURSED_RPM.get();

        // Promote the persisted default once when the rotation curse is repaired. An explicit
        // /dw rpm (or API setLimit) value has higher priority and must survive login unchanged.
        if (rotationRepaired && !RpmLimitManager.hasCommandOverride(player)
            && RpmLimitApi.getLimit(player) < rpm) {
            RpmLimitManager.setGearHeartDefaultLimit(player, rpm);
        }
        RpmLimitApi.setExternalLimit(player, RPM_SOURCE, rpm);
    }

    private static void clearCurses(ServerPlayer player) {
        RpmLimitApi.removeExternalLimit(player, RPM_SOURCE);
    }

    private static void clearAll(ServerPlayer player) {
        clearCurses(player);
        removeBaseBonuses(player);
    }

    private static void applyBaseBonuses(ServerPlayer player) {
        // Previous releases supplied infinite Luck I. Migrate once, without clearing
        // finite potion effects or stronger effects supplied by another source.
        if (player.getPersistentData().getBoolean(BONUS_STATE_KEY)
            && player.getPersistentData().getInt(BONUS_VERSION_KEY) < BONUS_VERSION) {
            removeHeartEffect(player, MobEffects.LUCK, 0);
        }
        player.getPersistentData().putBoolean(BONUS_STATE_KEY, true);
        player.getPersistentData().putInt(BONUS_VERSION_KEY, BONUS_VERSION);
        addModifier(player, ForgeMod.BLOCK_REACH.get(), BLOCK_REACH_ID, "Gear Heart Block Reach", 2.0d);
        addModifier(player, ForgeMod.ENTITY_REACH.get(), ENTITY_REACH_ID, "Gear Heart Entity Reach", 2.0d);

        if (!hasInfiniteEffect(player, MobEffects.DIG_SPEED, 1)) {
            player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SPEED, MobEffectInstance.INFINITE_DURATION, 1,
                false, false, true
            ));
        }
        if (!hasInfiniteEffect(player, MobEffects.WEAKNESS, 0)) {
            player.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS, MobEffectInstance.INFINITE_DURATION, 0,
                false, false, true
            ));
        }
        if (!hasInfiniteEffect(player, MobEffects.NIGHT_VISION, 0)) {
            player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0,
                false, false, true
            ));
        }
    }

    private static void removeBaseBonuses(ServerPlayer player) {
        AttributeInstance blockReach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (blockReach != null) {
            blockReach.removeModifier(BLOCK_REACH_ID);
        }
        AttributeInstance entityReach = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (entityReach != null) {
            entityReach.removeModifier(ENTITY_REACH_ID);
        }

        if (player.getPersistentData().getBoolean(BONUS_STATE_KEY)) {
            removeHeartEffect(player, MobEffects.DIG_SPEED, 1);
            removeHeartEffect(player, MobEffects.WEAKNESS, 0);
            removeHeartEffect(player, MobEffects.NIGHT_VISION, 0);
            if (player.getPersistentData().getInt(BONUS_VERSION_KEY) < BONUS_VERSION) {
                removeHeartEffect(player, MobEffects.LUCK, 0);
            }
            player.getPersistentData().remove(BONUS_STATE_KEY);
            player.getPersistentData().remove(BONUS_VERSION_KEY);
        }
    }

    private static void removeHeartEffect(ServerPlayer player, net.minecraft.world.effect.MobEffect effect,
                                          int amplifier) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance != null && instance.getAmplifier() == amplifier
            && instance.getDuration() == MobEffectInstance.INFINITE_DURATION
            && !instance.isAmbient() && !instance.isVisible() && instance.showIcon()) {
            player.removeEffect(effect);
        }
    }

    private static void addModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                    UUID id, String name, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || instance.getModifier(id) != null) {
            return;
        }
        instance.addTransientModifier(new AttributeModifier(
            id, name, amount, AttributeModifier.Operation.ADDITION
        ));
    }

    private static boolean hasInfiniteEffect(ServerPlayer player, net.minecraft.world.effect.MobEffect effect,
                                             int amplifier) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance != null && instance.getAmplifier() >= amplifier
            && instance.getDuration() == MobEffectInstance.INFINITE_DURATION;
    }
}
