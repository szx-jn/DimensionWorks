package dev.szx.dimensionworks.transfer;

import com.mojang.blaze3d.platform.NativeImage;
import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.items.mobile_packager.PackPlayerPacket;
import dev.szx.dimensionworks.transfer.elastic.ElasticItems;
import dev.szx.dimensionworks.transfer.elastic.ElasticParcelState;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in disposable client check. All world/option changes stay under run-elastic-client. */
@Mod.EventBusSubscriber(modid = WDTransfer.MOD_ID, value = Dist.CLIENT)
public final class ElasticClientSmokeCheck {
    private static int stage;
    private static int ticks;
    private static int renders;
    private static Vec3 releasePosition;
    private static double peakLaunchSpeed;
    private static long started;
    private static boolean startupCaptured;

    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
    public static void rendered(RenderPlayerEvent.Pre event) {
        if (ElasticParcelState.isElastic(event.getEntity())) renders++;
    }

    @SubscribeEvent
    public static void beforeMovement(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && stage == 3 && event.player == Minecraft.getInstance().player)
            peakLaunchSpeed = Math.max(peakLaunchSpeed, event.player.getDeltaMovement().length());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (Boolean.getBoolean("dimensionworks.elasticVerticalBeltClientCheck")) return;
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (started == 0) started = System.currentTimeMillis();
        if (System.currentTimeMillis() - started > 150_000) throw new AssertionError("Elastic client check timed out at stage " + stage);
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            if (!startupCaptured && System.currentTimeMillis()-started>15_000) {
                startupCaptured=true;
                WDTransfer.LOGGER.info("Elastic client startup screen: {}", mc.screen == null ? "none" : mc.screen.getClass().getName());
                screenshot(mc,"startup-screen.png");
            }
            return;
        }
        if (stage == 0) {
            stage = 1;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            // Optional-mod warnings in this minimal fixture must not cover HUD pixel assertions.
            mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
            mc.getSingleplayerServer().execute(() -> {
                var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                p.setGameMode(GameType.SURVIVAL);
                p.serverLevel().setDayTime(1000);
                p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
                for (int x=-12;x<=12;x++) for(int z=-12;z<=12;z++) {
                    p.serverLevel().setBlockAndUpdate(new BlockPos(x,79,z), Blocks.STONE.defaultBlockState());
                    for(int y=80;y<=84;y++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState());
                }
                p.teleportTo(.5,80,.5);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ElasticItems.PACKAGER.get().getDefaultInstance());
                PackPlayerPacket.pack(p,p,true);
            });
            return;
        }
        if (stage == 1) {
            if (!ElasticParcelState.isElastic(mc.player) || !mc.player.onGround()) return;
            var original = mc.getItemRenderer().getModel(CMPItems.MOBILE_PACKAGER.asStack(), mc.level, mc.player, 0);
            var upgraded = mc.getItemRenderer().getModel(ElasticItems.PACKAGER.get().getDefaultInstance(), mc.level, mc.player, 0);
            var a = original.getQuads(null,null,RandomSource.create(0));
            var b = upgraded.getQuads(null,null,RandomSource.create(0));
            if(a.isEmpty() || a.size()!=b.size())throw new AssertionError("Tool model missing or changed");
            for(int i=0;i<a.size();i++)if(!Arrays.equals(a.get(i).getVertices(),b.get(i).getVertices()))throw new AssertionError("Tool model geometry changed");
            mc.player.setXRot(0);mc.player.setYRot(0);
            mc.options.keyShift.setDown(true);
            stage = 2;ticks = 0;
        } else if (stage == 2) {
            ticks++;
            if (ticks == 25) screenshot(mc,"partial-charge.png");
            // Hold past the cap so client prediction cannot release one server tick early.
            if (ticks >= 60 && goldHud(mc)) {
                screenshot(mc,"full-charge.png");
                if(renders==0)throw new AssertionError("Packed-player rendering never ran");
                releasePosition=mc.player.position();
                mc.player.setXRot(-30);
                mc.options.keyShift.setDown(false);
                stage=3;ticks=0;
            }
            if(ticks>120)throw new AssertionError("Full charge never became gold in the rendered HUD");
        } else if (stage == 3) {
            if (++ticks==8) {
                if (Math.abs(peakLaunchSpeed - 18.5625) > 1e-5)
                    throw new AssertionError("Client did not receive the full launch velocity before movement: " + peakLaunchSpeed);
                if(mc.player.position().distanceTo(releasePosition)<2 || mc.player.getY()<=releasePosition.y+.2)
                    throw new AssertionError("Charged look-direction launch did not move the real client: " + mc.player.position() + " from " + releasePosition);
                screenshot(mc,"launch.png");
                mc.getSingleplayerServer().execute(() -> {
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    PlayerParcel.unpack(p);
                });
                stage=4;ticks=0;
            }
        } else if (stage==4 && ++ticks>=15) {
            if(ElasticParcelState.isElastic(mc.player) || PlayerParcel.isPacked(mc.player))throw new AssertionError("Unpack did not synchronize to client");
            WDTransfer.LOGGER.info("ELASTIC_CLIENT_CHECK_PASS: identical tool model, parcel render, two-second charge HUD, full launch and unpack; GUI={}x{}, scale={}, peakLaunchSpeed={}",
                mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),mc.getWindow().getGuiScale(),peakLaunchSpeed);
            stage=5;mc.stop();
        }
    }

    private static void screenshot(Minecraft mc,String name) {
        try {
            Path target=mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
            Files.createDirectories(target.getParent());
            try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(target); }
            WDTransfer.LOGGER.info("Elastic client screenshot: {}",target);
        } catch(java.io.IOException e) { throw new AssertionError(e); }
    }

    private static boolean goldHud(Minecraft mc) {
        int left=mc.getWindow().getGuiScaledWidth()/2-62;
        int y=mc.getWindow().getGuiScaledHeight()-22-20-3;
        double scale=mc.getWindow().getGuiScale();
        try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            for(int segment=0;segment<3;segment++) {
                int color=image.getPixelRGBA((int)((left+segment*42+20)*scale),(int)(y*scale));
                if(color!=0xFF5BD4FF)return false;
            }
            return true;
        }
    }
}
