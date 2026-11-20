package dev.szx.dimensionworks.wrenchcompat;

import dev.ftb.mods.ftbultimine.FTBUltimine;
import dev.ftb.mods.ftbultimine.api.shape.ShapeContext;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import dev.architectury.impl.NetworkAggregator;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@GameTestHolder(DimensionWorksWrenchCompat.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchCompatGameTests {
    private static ServerPlayer createPlayer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "dw-test-" + UUID.randomUUID().toString().substring(0, 8));
        var cookie = new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        var player = new ServerPlayer(server, helper.getLevel(), profile, cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // 配置须早于真实登录事件；Architectury 使用独立声明的载荷通道。
        NetworkRegistry.configureMockConnection(connection);
        ChannelAttributes.getOrCreateAdHocChannels(connection).addAll(NetworkAggregator.S2C_TYPE.keySet());
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    private static void removePlayer(ServerPlayer player) {
        player.getServer().getPlayerList().remove(player);
        player.discard();
        ((EmbeddedChannel) player.connection.getConnection().channel()).finishAndReleaseAll();
    }

    @GameTest(template = "empty")
    public static void batchDismantlesActualAe2AndMekanismBlocksAndRestoresPressedState(GameTestHelper helper) {
        var player = createPlayer(helper);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:wrench"))));
        var aePos = helper.absolutePos(new BlockPos(1, 1, 1));
        var mekPos = helper.absolutePos(new BlockPos(3, 1, 1));
        var level = helper.getLevel();
        level.setBlockAndUpdate(aePos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:energy_acceptor")).defaultBlockState());
        level.setBlockAndUpdate(mekPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:basic_energy_cube")).defaultBlockState());
        helper.assertTrue(!level.getBlockState(aePos).isAir() && !level.getBlockState(mekPos).isAir(),
                "实际 AE2/Mekanism 方块必须成功注册");
        var data = FTBUltimine.getInstance().getOrCreatePlayerData(player);
        data.setPressed(true);
        var context = new ShapeContext(player, aePos, Direction.UP, level.getBlockState(aePos), (a, b) -> true, 16);
        int count = WrenchCompatRightClickHandler.INSTANCE.handleRightClickBlock(context,
                InteractionHand.MAIN_HAND, List.of(aePos, mekPos));
        helper.assertTrue(count == 2, "两个目标方块都应被实际拆除，实际=" + count);
        helper.assertTrue(level.getBlockState(aePos).isAir() && level.getBlockState(mekPos).isAir(),
                "不能只报告成功而保留目标方块");
        helper.assertTrue(data.isPressed(), "批处理结束后必须恢复 Ultimine 按键状态");
        removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deniedBlockInteractionPreservesMachine(GameTestHelper helper) {
        var player = createPlayer(helper);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:wrench"))));
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var level = helper.getLevel();
        var state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:basic_energy_cube")).defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        Consumer<PlayerInteractEvent.RightClickBlock> deny = event -> {
            if (event.getPos().equals(pos)) event.setUseBlock(TriState.FALSE);
        };
        NeoForge.EVENT_BUS.addListener(deny);
        try {
            var context = new ShapeContext(player, pos, Direction.UP, state, (a, b) -> true, 16);
            int count = WrenchCompatRightClickHandler.INSTANCE.handleRightClickBlock(context,
                    InteractionHand.MAIN_HAND, List.of(pos));
            helper.assertTrue(count == 0 && level.getBlockState(pos).equals(state),
                    "其他模组拒绝方块交互时必须保留机器");
        } finally {
            NeoForge.EVENT_BUS.unregister(deny);
            removePlayer(player);
        }
        helper.succeed();
    }
}
