package dev.szx.dimensionworks.rpmlimit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

/** Operator commands for inspecting and resetting Gear Heart curse progress. */
public final class GearHeartCommands {

    private static final String[] CURSE_NAMES = {
        "转速限制", "Create 容量限制", "矿物处理额度", "AE2 合成并发",
        "跨工序额度", "活跃机器数量", "高阶产物额度"
    };
    private static final int[] CURSE_BITS = {
        GearHeartState.ROTATION_SPEED, GearHeartState.CREATE_CAPACITY,
        GearHeartState.MEK_ORE_OPERATIONS, GearHeartState.AE_CRAFTING_JOBS,
        GearHeartState.CROSS_PROCESS, GearHeartState.ACTIVE_MACHINES,
        GearHeartState.FINAL_PRODUCTION
    };

    private GearHeartCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dw")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("gearheart")
                .then(Commands.literal("status")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> showLevel(
                            context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("curse")
                    .then(Commands.literal("clear")
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("curseIndex", IntegerArgumentType.integer(1, 7))
                                .executes(context -> clearCurse(
                                    context.getSource(),
                                    EntityArgument.getPlayer(context, "player"),
                                    IntegerArgumentType.getInteger(context, "curseIndex")))))))));
    }

    private static int showLevel(net.minecraft.commands.CommandSourceStack source, ServerPlayer target) {
        ItemStack heart = findHeart(target);
        if (heart.isEmpty()) {
            source.sendFailure(Component.literal(target.getGameProfile().getName() + " 没有装备齿轮之心。"));
            return 0;
        }

        int mask = GearHeartState.getRepairedMask(heart);
        source.sendSuccess(() -> Component.literal("玩家 " + target.getGameProfile().getName()
            + " 的齿轮之心修复进度："), false);
        for (int i = 0; i < CURSE_NAMES.length; i++) {
            boolean repaired = (mask & CURSE_BITS[i]) != 0;
            Component line = Component.literal((i + 1) + ". " + CURSE_NAMES[i] + "："
                + (repaired ? "已破除" : "未破除"));
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int clearCurse(net.minecraft.commands.CommandSourceStack source,
                                  ServerPlayer target, int curseIndex) {
        var maybeInventory = CuriosApi.getCuriosInventory(target).resolve();
        if (maybeInventory.isEmpty()) {
            source.sendFailure(Component.literal("无法读取目标玩家的饰品栏。"));
            return 0;
        }

        ICuriosItemHandler inventory = maybeInventory.get();
        for (SlotResult result : inventory.findCurios(GearHeartState.SLOT_ID)) {
            ItemStack current = result.stack();
            if (!GearHeartState.isGearHeart(current)) {
                continue;
            }

            int bit = CURSE_BITS[curseIndex - 1];
            if ((GearHeartState.getRepairedMask(current) & bit) == 0) {
                source.sendFailure(Component.literal("玩家 " + target.getGameProfile().getName()
                    + " 的第 " + curseIndex + " 项诅咒本来就未破除。"));
                return 0;
            }

            ItemStack reopened = GearHeartState.reopenCurse(current, curseIndex);
            if (reopened.isEmpty()) {
                source.sendFailure(Component.literal("重置齿轮之心失败。"));
                return 0;
            }

            inventory.setEquippedCurio(
                result.slotContext().identifier(), result.slotContext().index(), reopened);
            GearHeartEffects.tickPlayer(target);
            source.sendSuccess(() -> Component.literal("已将玩家 " + target.getGameProfile().getName()
                + " 的第 " + curseIndex + " 项诅咒（" + CURSE_NAMES[curseIndex - 1]
                + "）恢复为未破除状态。"), true);
            return 1;
        }

        source.sendFailure(Component.literal("玩家 " + target.getGameProfile().getName()
            + " 没有装备齿轮之心。"));
        return 0;
    }

    private static ItemStack findHeart(ServerPlayer player) {
        var maybeInventory = CuriosApi.getCuriosInventory(player).resolve();
        if (maybeInventory.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (SlotResult result : maybeInventory.get().findCurios(GearHeartState.SLOT_ID)) {
            if (GearHeartState.isGearHeart(result.stack())) {
                return result.stack();
            }
        }
        return ItemStack.EMPTY;
    }
}
