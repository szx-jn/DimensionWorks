package dev.szx.dimensionworks.transfer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import de.theidler.create_mobile_packages.items.mobile_packager.PackPlayerPacket;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import dev.szx.dimensionworks.transfer.elastic.ElasticItems;
import dev.szx.dimensionworks.transfer.elastic.ElasticParcelState;
import dev.szx.dimensionworks.transfer.elastic.ElasticVerticalBelt;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 仅在忽略的独立客户端世界运行；检查实际客户端运动和服务端位置，不截取图片。 */
@Mod.EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class ElasticVerticalBeltClientCheck {
    private static final BlockPos BASE = new BlockPos(0, 80, 0);
    private static final BlockPos MOTOR = BASE.west();
    private static int stage, ticks;
    private static double initialY;
    private static long started;
    private static volatile double serverY;
    private static volatile boolean ready;

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (!Boolean.getBoolean("dimensionworks.elasticVerticalBeltClientCheck") || event.phase != TickEvent.Phase.END || !ready) return;
        var server = event.getServer();
        if (!server.getPlayerList().getPlayers().isEmpty()) serverY = server.getPlayerList().getPlayers().get(0).getY();
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("dimensionworks.elasticVerticalBeltClientCheck") || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (started == 0) started = System.currentTimeMillis();
        if (System.currentTimeMillis() - started > 120_000) throw new AssertionError("Vertical belt client check timeout at stage " + stage);
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        if (stage == 0) {
            stage = 1;
            mc.options.keyShift.setDown(false);
            mc.getSingleplayerServer().execute(() -> {
                var server = mc.getSingleplayerServer();
                var p = server.getPlayerList().getPlayer(mc.player.getUUID());
                p.setGameMode(GameType.SURVIVAL);
                var level = p.serverLevel();
                for (BlockPos pos : BlockPos.betweenClosed(-3, 80, -3, 3, 91, 3)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                for (int index = 0; index < 10; index++) {
                    level.setBlockAndUpdate(BASE.above(index), AllBlocks.BELT.getDefaultState()
                            .setValue(BeltBlock.HORIZONTAL_FACING, Direction.SOUTH).setValue(BeltBlock.SLOPE, BeltSlope.VERTICAL)
                            .setValue(BeltBlock.PART, index == 0 ? BeltPart.START : index == 9 ? BeltPart.END : BeltPart.MIDDLE));
                }
                BeltBlock.initBelt(level, BASE);
                level.setBlockAndUpdate(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
                ((CreativeMotorBlockEntity) level.getBlockEntity(MOTOR)).generatedSpeed.setValue(-48);
                p.setItemInHand(InteractionHand.MAIN_HAND, ElasticItems.PACKAGER.get().getDefaultInstance());
                PackPlayerPacket.pack(p, p, true);
                p.setDeltaMovement(Vec3.ZERO);
                p.teleportTo(.5, 82, 13.0 / 16 + p.getBbWidth() / 2);
                ready = true;
            });
            return;
        }
        var belt = mc.level.getBlockEntity(BASE.above(2));
        if (!(belt instanceof BeltBlockEntity segment)) return;
        if (stage == 1 && ready && ElasticParcelState.isElastic(mc.player) && segment.getSpeed() < 0
            && ElasticVerticalBelt.findContact(mc.player) != null) {
            initialY = mc.player.getY();
            stage = 2; ticks = 0;
        } else if (stage == 2 && ++ticks == 20) {
            checkMovement(mc, true);
            motor(mc, 48);
            stage = 3;
        } else if (stage == 3 && segment.getSpeed() > 0) {
            initialY = mc.player.getY();
            stage = 4; ticks = 0;
        } else if (stage == 4 && ++ticks == 20) {
            checkMovement(mc, false);
            motor(mc, 0);
            stage = 5;
        } else if (stage == 5 && segment.getSpeed() == 0) {
            stage = 6; ticks = 0;
        } else if (stage == 6 && ++ticks == 3) {
            if (ElasticVerticalBelt.findContact(mc.player) != null || mc.player.getDeltaMovement().y >= 0)
                throw new AssertionError("Stopped motor did not release real client into normal gravity");
            mc.getSingleplayerServer().execute(() -> PlayerParcel.unpack(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())));
            stage = 7; ticks = 0;
        } else if (stage == 7 && ++ticks >= 10 && !PlayerParcel.isPacked(mc.player)) {
            if (ElasticParcelState.isElastic(mc.player) || ElasticVerticalBelt.findContact(mc.player) != null)
                throw new AssertionError("Unpacked client still has vertical belt transport");
            WDTransfer.LOGGER.info("ELASTIC_VERTICAL_BELT_CLIENT_CHECK_PASS: powered vertical belt, actual upward/downward motion, reversal, server/client positions, stop/gravity and unpack");
            stage = 8;
            mc.stop();
        }
    }

    private static void checkMovement(Minecraft mc, boolean up) {
        double distance = (mc.player.getY() - initialY) * (up ? 1 : -1);
        if (distance < 1.5 || distance > 2.5 || Math.abs(mc.player.getY() - serverY) > .5)
            throw new AssertionError("Vertical belt motion or synchronization failed: up=" + up + ", distance=" + distance + ", client=" + mc.player.getY() + ", server=" + serverY);
    }

    private static void motor(Minecraft mc, int rpm) {
        mc.getSingleplayerServer().execute(() -> {
            var level = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
            ((CreativeMotorBlockEntity) level.getBlockEntity(MOTOR)).generatedSpeed.setValue(rpm);
        });
    }
}
