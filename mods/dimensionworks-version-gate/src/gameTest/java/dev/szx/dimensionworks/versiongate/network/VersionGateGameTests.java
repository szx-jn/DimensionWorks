package dev.szx.dimensionworks.versiongate.network;

import dev.szx.dimensionworks.versiongate.DimensionWorksVersionGate;
import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.util.ArrayList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksVersionGate.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VersionGateGameTests {
    @GameTest(template = "empty")
    public static void configurationTaskReadsEachLoginAndRoundTripsVersion(GameTestHelper helper) throws Exception {
        var directory = Files.createTempDirectory("dw-version-config-test-");
        var file = directory.resolve("VERSION");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var task = new VersionGateNetwork.VersionTask(directory);
            var sent = new ArrayList<CustomPacketPayload>();
            Files.writeString(file, "0.3.52");
            task.run(sent::add);
            Files.writeString(file, "0.3.53");
            task.run(sent::add);
            helper.assertTrue(((LoginVersionPacket) sent.get(0)).serverVersion().equals("0.3.52"),
                    "首次配置任务必须读取实际 VERSION");
            var second = (LoginVersionPacket) sent.get(1);
            helper.assertTrue(second.serverVersion().equals("0.3.53"), "下一次登录不能复用缓存版本");
            LoginVersionPacket.STREAM_CODEC.encode(buffer, second);
            helper.assertTrue(LoginVersionPacket.STREAM_CODEC.decode(buffer).equals(second), "配置载荷编解码必须一致");
            helper.assertTrue(task.type().equals(VersionGateNetwork.TASK_TYPE), "确认必须完成对应配置任务");
        } finally {
            buffer.release();
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
        helper.succeed();
    }
}
