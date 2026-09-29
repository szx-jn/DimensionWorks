package dev.szx.dimensionworks.rpmlimit;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Map;
import java.util.Optional;
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
    private static final String STRESS_COST_SOURCE = "gear_heart:stress_cost";
    private static final String STRESS_CAPACITY_SOURCE = "gear_heart:stress_capacity";
    private static final String BONUS_STATE_KEY = "dimensionworks_gear_heart_bonuses";

    private static final UUID BLOCK_REACH_ID =
        UUID.fromString("61ee7d0c-7d95-4a6c-b50a-6ce2168df0a4");
    private static final UUID ENTITY_REACH_ID =
        UUID.fromString("ab7653cc-1c9d-4dbb-b58d-18f377e4ea25");

    private static final Map<UUID, Long> NEXT_FREEZE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FREEZE_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, FreezePose> FREEZE_POSES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_OUTPUT_FAILURE_FEEDBACK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_NIGHT_HUNGER_FEEDBACK = new ConcurrentHashMap<>();

    private GearHeartEffects() {}

    public static void tickPlayer(ServerPlayer player) {
        Optional<ItemStack> maybeHeart = equippedHeart(player);
        if (maybeHeart.isEmpty()) {
            clearAll(player);
            return;
        }

        ItemStack heart = maybeHeart.get();
        applyBaseBonuses(player);
        if (!GearHeartState.isCursed(heart)) {
            clearCurses(player);
            return;
        }

        syncCurseState(player, heart);

        long now = player.level().getGameTime();
        if (GearHeartState.isCurseActive(heart, GearHeartState.FREEZE)) {
            scheduleFreeze(player, now);
            enforceFreeze(player);
        } else {
            clearFreeze(player);
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        clearAll(player);
        NEXT_FREEZE.remove(player.getUUID());
        FREEZE_UNTIL.remove(player.getUUID());
        FREEZE_POSES.remove(player.getUUID());
        NEXT_OUTPUT_FAILURE_FEEDBACK.remove(player.getUUID());
        NEXT_NIGHT_HUNGER_FEEDBACK.remove(player.getUUID());
    }

    public static RepairResult repairEquippedCurse(ServerPlayer player, int curseMask) {
        Optional<ICuriosItemHandler> maybeInventory = CuriosApi.getCuriosInventory(player).resolve();
        if (maybeInventory.isEmpty())
            return RepairResult.NO_TARGET;

        ICuriosItemHandler inventory = maybeInventory.get();
        for (SlotResult result : inventory.findCurios(GearHeartState.SLOT_ID)) {
            ItemStack stack = result.stack();
            if (!GearHeartState.isCursed(stack))
                continue;
            if (GearHeartState.isCurseActive(stack, curseMask)) {
                ItemStack repaired = stack.copy();
                GearHeartState.setRepairedMask(
                    repaired, GearHeartState.getRepairedMask(repaired) | curseMask
                );
                inventory.setEquippedCurio(
                    result.slotContext().identifier(),
                    result.slotContext().index(),
                    repaired
                );
                syncCurseState(player, repaired);
                return RepairResult.REPAIRED;
            }
            return RepairResult.ALREADY_REPAIRED;
        }
        return RepairResult.NO_TARGET;
    }

    public static boolean isFrozen(ServerPlayer player) {
        Long until = FREEZE_UNTIL.get(player.getUUID());
        if (until == null)
            return false;
        if (player.level().getGameTime() < until)
            return true;
        FREEZE_UNTIL.remove(player.getUUID());
        FREEZE_POSES.remove(player.getUUID());
        return false;
    }

    public static void enforceFreeze(ServerPlayer player) {
        if (!isFrozen(player))
            return;
        FreezePose pose = FREEZE_POSES.get(player.getUUID());
        if (pose == null) {
            startFreeze(player);
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        if (player.distanceToSqr(pose.x, pose.y, pose.z) > 1.0e-6
            || Math.abs(player.getXRot() - pose.xRot) > 0.1f
            || Math.abs(player.getYRot() - pose.yRot) > 0.1f) {
            player.connection.teleport(pose.x, pose.y, pose.z, pose.yRot, pose.xRot);
        }
    }

    public static boolean isNightHungerActive(ServerPlayer player) {
        return equippedHeart(player)
            .filter(stack -> GearHeartState.isCurseActive(stack, GearHeartState.NIGHT_HUNGER))
            .isPresent() && isNight(player.level());
    }

    public static void notifyNightHunger(ServerPlayer player) {
        notifyWithCooldown(
            player,
            NEXT_NIGHT_HUNGER_FEEDBACK,
            1200L,
            "message.kubejs.gear_heart.curse_trigger.night_hunger"
        );
    }

    public static float reducedAttackDamage(ServerPlayer player, float damage) {
        if (!hasActiveCurse(player, GearHeartState.ATTACK_DAMAGE))
            return damage;
        return Math.max(0.0f, (float) Math.floor(damage * 0.7f));
    }

    public static boolean shouldFailOutput(BlockEntity blockEntity) {
        if (!(blockEntity instanceof KineticBlockEntity kinetic) || blockEntity.getLevel() == null)
            return false;
        Level level = blockEntity.getLevel();
        if (level.isClientSide)
            return false;
        ServerPlayer owner = ownerOf(kinetic);
        if (owner == null || !hasActiveCurse(owner, GearHeartState.OUTPUT_FAILURE))
            return false;
        if (level.random.nextFloat() >= RpmLimitConfig.GEAR_HEART_OUTPUT_FAILURE_CHANCE.get())
            return false;

        notifyWithCooldown(
            owner,
            NEXT_OUTPUT_FAILURE_FEEDBACK,
            40L,
            "message.kubejs.gear_heart.curse_trigger.output_failure"
        );
        return true;
    }

    public static void failMillstoneOutput(MillstoneBlockEntity millstone) {
        ItemStack input = millstone.inputInv.getStackInSlot(0);
        if (input.isEmpty())
            return;

        ItemStack remaining = input.getCraftingRemainingItem();
        input.shrink(1);
        millstone.inputInv.setStackInSlot(0, input);
        if (!remaining.isEmpty())
            ItemHandlerHelper.insertItemStacked(millstone.outputInv, remaining, false);
        millstone.setChanged();
        millstone.sendData();
    }

    public static void failSawOutput(com.simibubi.create.content.kinetics.saw.SawBlockEntity saw) {
        if (saw.inventory.isEmpty())
            return;
        saw.inventory.clear();
        saw.setChanged();
        saw.sendData();
    }

    public static void consumeFailedWorldPress(ItemEntity itemEntity, boolean bulk) {
        ItemStack stack = itemEntity.getItem();
        if (bulk || stack.getCount() <= 1) {
            itemEntity.discard();
            return;
        }
        stack.shrink(1);
        itemEntity.setItem(stack);
    }

    public static boolean failBasinOutput(com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity operator,
                                          BasinBlockEntity basin, Recipe<?> recipe) {
        if (!shouldFailOutput(operator) || recipe == null)
            return false;
        consumeBasinIngredients(basin, recipe);
        operator.setChanged();
        operator.sendData();
        basin.notifyChangeOfContents();
        return true;
    }

    private static void consumeBasinIngredients(BasinBlockEntity basin, Recipe<?> recipe) {
        IItemHandler items = basin.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (items != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                for (int slot = 0; slot < items.getSlots(); slot++) {
                    ItemStack extracted = items.extractItem(slot, 1, true);
                    if (extracted.isEmpty() || !ingredient.test(extracted))
                        continue;
                    items.extractItem(slot, 1, false);
                    break;
                }
            }
        }

        if (!(recipe instanceof BasinRecipe basinRecipe))
            return;

        IFluidHandler fluids = basin.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
        if (fluids == null)
            return;

        for (var ingredient : basinRecipe.getFluidIngredients()) {
            int required = ingredient.getRequiredAmount();
            for (int tank = 0; tank < fluids.getTanks() && required > 0; tank++) {
                FluidStack available = fluids.getFluidInTank(tank);
                if (!ingredient.test(available))
                    continue;
                int drained = Math.min(required, available.getAmount());
                FluidStack request = available.copy();
                request.setAmount(drained);
                FluidStack removed = fluids.drain(request, IFluidHandler.FluidAction.EXECUTE);
                required -= removed.getAmount();
            }
        }
    }

    private static Optional<ItemStack> equippedHeart(ServerPlayer player) {
        Optional<ICuriosItemHandler> maybeInventory = CuriosApi.getCuriosInventory(player).resolve();
        if (maybeInventory.isEmpty())
            return Optional.empty();

        ICuriosItemHandler inventory = maybeInventory.get();
        for (SlotResult result : inventory.findCurios(GearHeartState.SLOT_ID)) {
            ItemStack stack = result.stack();
            if (!GearHeartState.isGearHeart(stack))
                continue;
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

    private static boolean hasActiveCurse(ServerPlayer player, int curseMask) {
        return equippedHeart(player)
            .filter(stack -> GearHeartState.isCurseActive(stack, curseMask))
            .isPresent();
    }

    private static ServerPlayer ownerOf(KineticBlockEntity kinetic) {
        UUID owner = RpmLimitManager.owner(kinetic);
        if (owner == null || kinetic.getLevel() == null || kinetic.getLevel().getServer() == null)
            return null;
        return kinetic.getLevel().getServer().getPlayerList().getPlayer(owner);
    }

    private static void syncCurseState(ServerPlayer player, ItemStack heart) {
        if (GearHeartState.isCurseActive(heart, GearHeartState.RPM_LIMIT)) {
            RpmLimitApi.setExternalLimit(player, RPM_SOURCE, 32);
        } else {
            RpmLimitApi.removeExternalLimit(player, RPM_SOURCE);
            if (RpmLimitApi.getLimit(player) != 512)
                RpmLimitApi.setLimit(player, 512);
        }

        if (GearHeartState.isCurseActive(heart, GearHeartState.STRESS_COST))
            RpmLimitApi.setStressCostMultiplier(player, STRESS_COST_SOURCE, 2.0d);
        else
            RpmLimitApi.removeStressCostMultiplier(player, STRESS_COST_SOURCE);

        if (GearHeartState.isCurseActive(heart, GearHeartState.STRESS_CAPACITY))
            RpmLimitApi.setStressCapacityMultiplier(player, STRESS_CAPACITY_SOURCE, 0.8d);
        else
            RpmLimitApi.removeStressCapacityMultiplier(player, STRESS_CAPACITY_SOURCE);
    }

    private static void clearCurses(ServerPlayer player) {
        RpmLimitApi.removeExternalLimit(player, RPM_SOURCE);
        RpmLimitApi.removeStressCostMultiplier(player, STRESS_COST_SOURCE);
        RpmLimitApi.removeStressCapacityMultiplier(player, STRESS_CAPACITY_SOURCE);
        clearFreeze(player);
    }

    private static void clearAll(ServerPlayer player) {
        clearCurses(player);
        removeBaseBonuses(player);
    }

    private static void applyBaseBonuses(ServerPlayer player) {
        player.getPersistentData().putBoolean(BONUS_STATE_KEY, true);
        addModifier(player, ForgeMod.BLOCK_REACH.get(), BLOCK_REACH_ID, "Gear Heart Block Reach", 2.0d);
        addModifier(player, ForgeMod.ENTITY_REACH.get(), ENTITY_REACH_ID, "Gear Heart Entity Reach", 2.0d);

        if (!hasInfiniteEffect(player, MobEffects.DIG_SPEED, 1))
            player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SPEED, MobEffectInstance.INFINITE_DURATION, 1,
                false, false, true
            ));
        if (!hasInfiniteEffect(player, MobEffects.LUCK, 0))
            player.addEffect(new MobEffectInstance(
                MobEffects.LUCK, MobEffectInstance.INFINITE_DURATION, 0,
                false, false, true
            ));
    }

    private static void removeBaseBonuses(ServerPlayer player) {
        AttributeInstance blockReach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (blockReach != null)
            blockReach.removeModifier(BLOCK_REACH_ID);
        AttributeInstance entityReach = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (entityReach != null)
            entityReach.removeModifier(ENTITY_REACH_ID);

        if (player.getPersistentData().getBoolean(BONUS_STATE_KEY)) {
            player.removeEffect(MobEffects.DIG_SPEED);
            player.removeEffect(MobEffects.LUCK);
        }
    }

    private static void addModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                    UUID id, String name, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || instance.getModifier(id) != null)
            return;
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

    private static void scheduleFreeze(ServerPlayer player, long now) {
        long next = NEXT_FREEZE.computeIfAbsent(player.getUUID(), ignored -> now + randomDelay(player));
        if (now < next)
            return;

        NEXT_FREEZE.put(player.getUUID(), now + randomDelay(player));
        double chance = RpmLimitConfig.GEAR_HEART_FREEZE_CHANCE.get();
        if (player.getRandom().nextDouble() < chance)
            startFreeze(player);
    }

    private static long randomDelay(ServerPlayer player) {
        int min = RpmLimitConfig.GEAR_HEART_FREEZE_INTERVAL_MIN_SECONDS.get();
        int max = Math.max(min, RpmLimitConfig.GEAR_HEART_FREEZE_INTERVAL_MAX_SECONDS.get());
        int seconds = min == max ? min : min + player.getRandom().nextInt(max - min + 1);
        return seconds * 20L;
    }

    private static void startFreeze(ServerPlayer player) {
        long now = player.level().getGameTime();
        FREEZE_UNTIL.put(player.getUUID(), now + RpmLimitConfig.GEAR_HEART_FREEZE_DURATION_TICKS.get());
        FREEZE_POSES.put(player.getUUID(), new FreezePose(
            player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()
        ));
        player.setDeltaMovement(Vec3.ZERO);
        player.displayClientMessage(
            Component.translatable("message.kubejs.gear_heart.curse_trigger.freeze")
                .withStyle(ChatFormatting.RED), true
        );
    }

    private static void clearFreeze(ServerPlayer player) {
        FREEZE_UNTIL.remove(player.getUUID());
        FREEZE_POSES.remove(player.getUUID());
        NEXT_FREEZE.remove(player.getUUID());
    }

    private static boolean isNight(Level level) {
        long dayTime = Math.floorMod(level.getDayTime(), 24000L);
        return dayTime >= 13000L && dayTime < 23000L;
    }

    private static void notifyWithCooldown(ServerPlayer player, Map<UUID, Long> feedbackTimes,
                                           long cooldownTicks, String translationKey) {
        long now = player.level().getGameTime();
        Long next = feedbackTimes.get(player.getUUID());
        if (next != null && now < next)
            return;

        feedbackTimes.put(player.getUUID(), now + cooldownTicks);
        player.displayClientMessage(
            Component.translatable(translationKey).withStyle(ChatFormatting.RED), true
        );
    }

    private record FreezePose(double x, double y, double z, float yRot, float xRot) {}
}
