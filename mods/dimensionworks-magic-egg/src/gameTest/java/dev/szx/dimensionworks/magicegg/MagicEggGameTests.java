package dev.szx.dimensionworks.magicegg;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@GameTestHolder(DimensionWorksMagicEgg.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal")
public final class MagicEggGameTests {
    @GameTest(template = "empty")
    public static void selectionAppliesHolderEffectOnlyAfterAuthorizedConfirmation(GameTestHelper helper) throws Exception {
        var player = helper.makeMockServerPlayerInLevel();
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        var countdown = new CompoundTag();
        MagicEggSelection.open(player, countdown);
        // 仅测试夹具读取会话 token，不向生产接口增加调试或客户端授权入口。
        var field = MagicEggSelection.class.getDeclaredField("SESSIONS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var sessions = (Map<UUID, SelectionSession>) field.get(null);
        try {
            var session = sessions.get(player.getUUID());
            helper.assertTrue(session != null, "食用应创建服务端会话");
            MagicEggSelection.submit(player, UUID.randomUUID(), false, List.of("minecraft:speed"));
            helper.assertTrue(!countdown.getBoolean("dw_magic_century_egg_pending"), "无效 token 不能启动倒计时");
            helper.assertTrue(!player.hasEffect(MobEffects.MOVEMENT_SPEED), "无效 token 不能给予效果");
            MagicEggSelection.submit(player, session.token(), false, List.of("minecraft:speed"));
            var effect = player.getEffect(MobEffects.MOVEMENT_SPEED);
            helper.assertTrue(effect != null && effect.getAmplifier() == 4 && effect.getDuration() == 12000,
                    "授权确认必须给予 V 级、10 分钟效果");
            helper.assertTrue(countdown.getBoolean("dw_magic_century_egg_pending"), "必须写入原 KubeJS 倒计时数据");
            helper.assertTrue(countdown.getLong("dw_magic_century_egg_deadline") == player.tickCount + 12000,
                    "倒计时必须沿用玩家 tickCount");
            helper.assertTrue(SelectionHistory.read(player.getPersistentData()).equals(List.of("minecraft:speed")),
                    "必须保存实际确认的选择");
            MagicEggSelection.submit(player, session.token(), false, List.of("minecraft:poison"));
            helper.assertTrue(!player.hasEffect(MobEffects.POISON), "已消费的会话不能重放");
        } finally {
            sessions.remove(player.getUUID());
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void effectPayloadRejectsOversizedCollectionBeforeAllocating(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            buffer.writeUUID(UUID.randomUUID());
            buffer.writeBoolean(false);
            buffer.writeVarInt(4097);
            boolean rejected = false;
            try {
                MagicEggNetwork.Submit.CODEC.decode(buffer);
            } catch (DecoderException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected, "超出注册表规模的集合必须在读取内容前拒绝");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void administratorRecoveryClearsCountdownAndOpenSelection(GameTestHelper helper) throws Exception {
        var player = helper.makeMockServerPlayerInLevel();
        var countdown = new CompoundTag();
        countdown.putBoolean("dw_magic_century_egg_pending", true);
        countdown.putLong("dw_magic_century_egg_deadline", 12345);
        var field = MagicEggSelection.class.getDeclaredField("SESSIONS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var sessions = (Map<UUID, SelectionSession>) field.get(null);
        sessions.put(player.getUUID(), new SelectionSession(List.of(), List.of(), false, countdown));
        try {
            helper.assertTrue(sessions.containsKey(player.getUUID()), "测试前应存在未完成的选择会话");
            MagicEggSelection.clearCountdown(player, countdown);
            helper.assertTrue(!countdown.getBoolean("dw_magic_century_egg_pending"), "必须清除立即处决标记");
            helper.assertTrue(countdown.getLong("dw_magic_century_egg_deadline") == 0, "必须清空死亡截止时间");
            helper.assertTrue(!sessions.containsKey(player.getUUID()), "必须取消该玩家未完成的选择会话");
        } finally {
            sessions.remove(player.getUUID());
            player.discard();
        }
        helper.succeed();
    }
}
