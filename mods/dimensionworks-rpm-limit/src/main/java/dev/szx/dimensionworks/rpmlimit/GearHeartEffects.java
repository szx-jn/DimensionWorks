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
    private static final String BONUS_RELEASED_KEY = "dimensionworks_gear_heart_bonus_released";
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
        GearHeartMilestones.record(player.getPersistentData(), heart);
        boolean fullyReleased = GearHeartState.isFullyReleased(heart);
        applyBaseBonuses(player, fullyReleased);
        syncQuotaState(player, heart);
        if (fullyReleased || !GearHeartState.isCursed(heart)) {
            clearCurses(player);
            updateQuotaMask(player.getUUID(), null);
            return;
        }

        updateQuotaMask(player.getUUID(), GearHeartState.getRepairedMask(heart));
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
            if (GearHeartState.isFullyReleasedMask(repairedMask | repairBit)) {
                repaired = GearHeartState.finalizeRepair(repaired);
            }

            inventory.setEquippedCurio(
                result.slotContext().identifier(),
                result.slotContext().index(),
                repaired
            );
            tickPlayer(player);
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
            return Optional.of(stack);
        }
        return Optional.empty();
    }

    public static boolean hasEquippedHeart(ServerPlayer player) {
        return equippedHeart(player).isPresent();
    }

    public static float outgoingDamageMultiplier(ServerPlayer player) {
        Optional<ItemStack> heart = equippedHeart(player);
        if (heart.isEmpty()) {
            return 1.0F;
        }
        return GearHeartState.isFullyReleased(heart.get()) ? 1.5F : 0.9F;
    }

    private static void syncQuotaState(ServerPlayer player, ItemStack heart) {
        boolean fullyReleased = GearHeartState.isFullyReleased(heart);
        int repairedMask = fullyReleased ? GearHeartState.ALL_CURSES : GearHeartState.getRepairedMask(heart);
        int rpm = RpmLimitConfig.gearHeartRotationLimit(repairedMask);

        // Every repair stage, including a reopened curse, follows its current RPM limit.
        // Explicit /dw rpm (or API setLimit) values keep their priority.
        if (!RpmLimitManager.hasCommandOverride(player) && RpmLimitApi.getLimit(player) != rpm) {
            RpmLimitManager.setGearHeartDefaultLimit(player, rpm);
        }
        if (fullyReleased) {
            clearCurses(player);
        } else {
            RpmLimitApi.setExternalLimit(player, RPM_SOURCE, rpm);
        }
    }

    private static void clearCurses(ServerPlayer player) {
        RpmLimitApi.removeExternalLimit(player, RPM_SOURCE);
    }

    private static void clearAll(ServerPlayer player) {
        clearCurses(player);
        removeBaseBonuses(player);
    }

    private static void applyBaseBonuses(ServerPlayer player, boolean fullyReleased) {
        boolean previouslyReleased = player.getPersistentData().getBoolean(BONUS_RELEASED_KEY);
        if (previouslyReleased != fullyReleased) {
            removeHeartEffect(player, MobEffects.DIG_SPEED, previouslyReleased ? 2 : 1);
        }
        player.getPersistentData().putBoolean(BONUS_STATE_KEY, true);
        player.getPersistentData().putBoolean(BONUS_RELEASED_KEY, fullyReleased);
        double reach = fullyReleased ? 4.0D : 2.0D;
        addModifier(player, ForgeMod.BLOCK_REACH.get(), BLOCK_REACH_ID, "Gear Heart Block Reach", reach);
        addModifier(player, ForgeMod.ENTITY_REACH.get(), ENTITY_REACH_ID, "Gear Heart Entity Reach", reach);

        ensureHeartEffect(player, MobEffects.DIG_SPEED, fullyReleased ? 2 : 1);
        ensureHeartEffect(player, MobEffects.NIGHT_VISION, 0);
    }

    private static void ensureHeartEffect(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        // Stronger effects belong to another source; preserve their duration and display flags.
        if (current != null && current.getAmplifier() > amplifier) return;
        if (current != null && current.getAmplifier() == amplifier && current.isInfiniteDuration()
            && !current.isVisible() && current.showIcon()) return;
        player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier,
            false, false, true));
    }

    /** The command teleport path rebuilds the client player without resending active effects. */
    public static void synchronizeAfterDimensionChange(ServerPlayer player) {
        tickPlayer(player);
        if (!hasEquippedHeart(player)) return;
        for (var effect : java.util.List.of(MobEffects.DIG_SPEED, MobEffects.NIGHT_VISION)) {
            MobEffectInstance instance = player.getEffect(effect);
            if (instance != null) player.connection.send(
                new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(player.getId(), instance));
        }
        var attributes = new java.util.ArrayList<AttributeInstance>();
        for (var attribute : java.util.List.of(ForgeMod.BLOCK_REACH.get(), ForgeMod.ENTITY_REACH.get())) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) attributes.add(instance);
        }
        if (!attributes.isEmpty()) player.connection.send(
            new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(player.getId(), attributes));
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
            removeHeartEffect(player, MobEffects.DIG_SPEED,
                player.getPersistentData().getBoolean(BONUS_RELEASED_KEY) ? 2 : 1);
            removeHeartEffect(player, MobEffects.NIGHT_VISION, 0);
            player.getPersistentData().remove(BONUS_STATE_KEY);
            player.getPersistentData().remove(BONUS_RELEASED_KEY);
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
        if (instance == null) {
            return;
        }
        AttributeModifier previous = instance.getModifier(id);
        if (previous != null) {
            if (previous.getAmount() == amount && previous.getOperation() == AttributeModifier.Operation.ADDITION) {
                return;
            }
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new AttributeModifier(
            id, name, amount, AttributeModifier.Operation.ADDITION
        ));
    }

}
