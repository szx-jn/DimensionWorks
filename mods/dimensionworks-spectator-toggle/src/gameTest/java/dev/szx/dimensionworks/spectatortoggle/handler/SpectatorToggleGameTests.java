package dev.szx.dimensionworks.spectatortoggle.handler;

import dev.szx.dimensionworks.spectatortoggle.DimensionWorksSpectatorToggle;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksSpectatorToggle.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal") // 1.21.1 官方 helper 的模拟玩家覆盖 isSpectator，断言实际 gameMode。
public final class SpectatorToggleGameTests {
    @GameTest(template = "empty")
    public static void returnsToExactSavedPointAndMode(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.ADVENTURE);
        player.moveTo(10.25, 80.5, -21.75, 35, -15);
        SpectatorToggleController.toggle(player);
        helper.assertTrue(player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR, "首次请求必须进入旁观");
        var point = player.getPersistentData().getCompound(SpectatorToggleState.RETURN_POINT_KEY);
        helper.assertTrue(point.getDouble("x") == 10.25 && point.getDouble("z") == -21.75,
                "返回点必须保留精确坐标");
        player.moveTo(30, 120, 30, 0, 0);
        SpectatorToggleController.toggle(player);
        helper.assertTrue(player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE, "返回必须恢复冒险模式");
        helper.assertTrue(player.getX() == 10.25 && player.getY() == 80.5 && player.getZ() == -21.75,
                "返回不能丢失小数坐标");
        helper.assertTrue(player.getYRot() == 35 && player.getXRot() == -15, "返回必须恢复朝向");
        helper.assertTrue(!player.getPersistentData().contains(SpectatorToggleState.RETURN_POINT_KEY),
                "返回成功后必须清除会话记录");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shutdownCleanupRestoresModeWithoutTeleporting(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        SpectatorToggleController.toggle(player);
        player.moveTo(100.5, 90, -100.5);
        SpectatorToggleController.resetOnServerStopping(player);
        helper.assertTrue(player.gameMode.getGameModeForPlayer() == GameType.CREATIVE, "停服必须恢复创造模式");
        helper.assertTrue(player.getX() == 100.5 && player.getZ() == -100.5, "停服清理在当前位置完成");
        helper.assertTrue(!player.getPersistentData().contains(SpectatorToggleState.RETURN_POINT_KEY),
                "停服不能保存活跃会话");
        player.discard();
        helper.succeed();
    }
}
