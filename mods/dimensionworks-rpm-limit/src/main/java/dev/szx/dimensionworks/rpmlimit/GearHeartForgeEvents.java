package dev.szx.dimensionworks.rpmlimit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioDropsEvent;
import top.theillusivec4.curios.api.event.CurioUnequipEvent;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.capability.ItemizedCurioCapability;

/** Forge and Curios event bridge for Gear Heart behaviour and tooltips. */
public final class GearHeartForgeEvents {

    private static final ResourceLocation CAPABILITY_ID =
        new ResourceLocation(DimensionWorksRpmLimit.MOD_ID, GearHeartState.SLOT_ID);

    private static final String[] CURSE_KEYS = {
        "tooltip.kubejs.gear_heart.curse.attack_damage",
        "tooltip.kubejs.gear_heart.curse.rpm_limit",
        "tooltip.kubejs.gear_heart.curse.output_failure",
        "tooltip.kubejs.gear_heart.curse.freeze",
        "tooltip.kubejs.gear_heart.curse.night_hunger",
        "tooltip.kubejs.gear_heart.curse.stress_cost",
        "tooltip.kubejs.gear_heart.curse.stress_capacity"
    };
    private static final int[] CURSE_BITS = {
        GearHeartState.ATTACK_DAMAGE,
        GearHeartState.RPM_LIMIT,
        GearHeartState.OUTPUT_FAILURE,
        GearHeartState.FREEZE,
        GearHeartState.NIGHT_HUNGER,
        GearHeartState.STRESS_COST,
        GearHeartState.STRESS_CAPACITY
    };
    private static final String[] REPAIRED_KEYS = {
        "tooltip.kubejs.gear_heart.repaired.attack_damage",
        "tooltip.kubejs.gear_heart.repaired.rpm_limit",
        "tooltip.kubejs.gear_heart.repaired.output_failure",
        "tooltip.kubejs.gear_heart.repaired.freeze",
        "tooltip.kubejs.gear_heart.repaired.night_hunger",
        "tooltip.kubejs.gear_heart.repaired.stress_cost",
        "tooltip.kubejs.gear_heart.repaired.stress_capacity"
    };

    private GearHeartForgeEvents() {}

    @SubscribeEvent
    public static void attachCurioCapability(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (!GearHeartState.isGearHeart(stack))
            return;
        event.addCapability(
            CAPABILITY_ID,
            CuriosApi.createCurioProvider(
                new ItemizedCurioCapability(GearHeartCurioItem.INSTANCE, stack)
            )
        );
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!GearHeartState.isGearHeart(stack))
            return;

        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.translatable("tooltip.kubejs.gear_heart.base")
            .withStyle(ChatFormatting.GREEN));
        event.getToolTip().add(Component.translatable("tooltip.kubejs.gear_heart.reach")
            .withStyle(ChatFormatting.GREEN));
        event.getToolTip().add(Component.translatable("tooltip.kubejs.gear_heart.haste")
            .withStyle(ChatFormatting.GREEN));
        event.getToolTip().add(Component.translatable("tooltip.kubejs.gear_heart.luck")
            .withStyle(ChatFormatting.GREEN));

        if (GearHeartState.isBlessed(stack)) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.kubejs.gear_heart.blessed_header")
                .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.kubejs.gear_heart.blessed")
                .withStyle(ChatFormatting.GREEN));
            return;
        }

        if (!GearHeartState.isDamagedGearHeart(stack))
            return;

        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.translatable("tooltip.kubejs.gear_heart.curse_header")
            .withStyle(ChatFormatting.DARK_RED));

        int repaired = GearHeartState.getRepairedMask(stack);
        for (int i = 0; i < CURSE_KEYS.length; i++) {
            boolean isRepaired = (repaired & CURSE_BITS[i]) != 0;
            MutableComponent line = Component.translatable(
                isRepaired ? REPAIRED_KEYS[i] : CURSE_KEYS[i]
            );
            event.getToolTip().add(line.withStyle(isRepaired ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }

    @SubscribeEvent
    public static void livingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        if (event.getSource().getDirectEntity() != player)
            return;
        event.setAmount(GearHeartEffects.reducedAttackDamage(player, event.getAmount()));
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GearHeartEffects.isFrozen(player))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelWhenFrozen(event, event.getEntity());
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelWhenFrozen(event, event.getEntity());
    }

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (cancelWhenFrozen(event, event.getEntity()))
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (event.getHand() != InteractionHand.MAIN_HAND)
            return;

        int curseMask = GearHeartState.repairBitForFragment(event.getItemStack());
        if (curseMask == 0)
            return;

        GearHeartEffects.RepairResult result =
            GearHeartEffects.repairEquippedCurse(player, curseMask);
        switch (result) {
            case REPAIRED -> {
                if (!player.isCreative())
                    event.getItemStack().shrink(1);
                player.displayClientMessage(
                    Component.translatable("message.kubejs.gear_heart.repair.success"), true
                );
            }
            case ALREADY_REPAIRED -> player.displayClientMessage(
                Component.translatable("message.kubejs.gear_heart.repair.already_repaired"), true
            );
            case NO_TARGET -> player.displayClientMessage(
                Component.translatable("message.kubejs.gear_heart.repair.no_target"), true
            );
            default -> {
                return;
            }
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelWhenFrozen(event, event.getEntity());
    }

    @SubscribeEvent
    public static void entityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelWhenFrozen(event, event.getEntity());
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && GearHeartEffects.isFrozen(player))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tossItem(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && GearHeartEffects.isFrozen(player))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void preventUnequip(CurioUnequipEvent event) {
        if (GearHeartState.isGearHeart(event.getStack()))
            event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void keepOnDeath(DropRulesEvent event) {
        event.addOverride(GearHeartState::isGearHeart, ICurio.DropRule.ALWAYS_KEEP);
    }

    @SubscribeEvent
    public static void removeAnyDeathDrop(CurioDropsEvent event) {
        event.getDrops().removeIf(itemEntity -> GearHeartState.isGearHeart(itemEntity.getItem()));
    }

    private static boolean cancelWhenFrozen(PlayerInteractEvent event,
                                            net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer && GearHeartEffects.isFrozen(serverPlayer)) {
            event.setCanceled(true);
            return true;
        }
        return false;
    }
}
