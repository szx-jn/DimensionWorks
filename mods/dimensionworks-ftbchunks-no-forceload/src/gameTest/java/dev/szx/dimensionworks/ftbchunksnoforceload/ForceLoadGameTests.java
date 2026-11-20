package dev.szx.dimensionworks.ftbchunksnoforceload;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksFtbChunksNoForceLoad.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForceLoadGameTests {
    private ForceLoadGameTests() {}

    @GameTest(template = "empty")
    public static void claimsRemainAvailableButForceLoadingIsDenied(GameTestHelper helper) throws Exception {
        var source = helper.getLevel().getServer().createCommandSourceStack();
        var team = FTBTeamsAPI.api().getManager().createServerTeam(
                source, "dw-test-" + UUID.randomUUID(), "", Color4I.WHITE);
        var manager = FTBChunksAPI.api().getManager();
        var data = manager.getOrCreateData(team);
        var position = new ChunkDimPos(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
        helper.assertTrue(data.claim(source, position, false).isSuccess(), "普通圈地必须可用");
        var chunk = manager.getChunk(position);
        helper.assertTrue(chunk != null, "圈地记录必须保留");
        helper.assertTrue(data.forceLoad(source, position, false) == ClaimResult.StandardProblem.NOT_ENOUGH_POWER,
                "强加载申请必须被明确拒绝");
        ((ClaimedChunkImpl) chunk).setForceLoadedTime(System.currentTimeMillis());
        helper.assertTrue(!chunk.isForceLoaded(), "直接修改时间不能启用强加载");
        helper.assertTrue(data.unclaim(source, position, false).isSuccess(), "普通取消圈地必须可用");
        helper.succeed();
    }
}
