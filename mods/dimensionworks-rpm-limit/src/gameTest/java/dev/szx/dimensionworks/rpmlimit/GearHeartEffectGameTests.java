package dev.szx.dimensionworks.rpmlimit;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(DimensionWorksRpmLimit.MOD_ID)
@PrefixGameTestTemplate(false)
public class GearHeartEffectGameTests {
    private static class RecordingConnection extends Connection {
        final ArrayList<Packet<?>> packets = new ArrayList<>();
        RecordingConnection() { super(PacketFlow.SERVERBOUND); }
        @Override public void send(Packet<?> packet, PacketSendListener listener) { packets.add(packet); }
    }
    private static ServerPlayer equipped(GameTestHelper h, RecordingConnection connection) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"HeartTest"));
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,p);
        RpmLimitManager.init(p);
        h.assertTrue(GearHeartEffects.ensureGearHeartSlots(p), "Test player must receive the existing Gear Heart slot/item");
        GearHeartEffects.tickPlayer(p);
        return p;
    }
    private static void close(ServerPlayer p) {
        GearHeartEffects.clearPlayer(p);
        p.serverLevel().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }
    private static ItemStack heart(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
            .findCurios(GearHeartState.SLOT_ID).get(0).stack();
    }
    private static void equip(ServerPlayer player, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
            .setEquippedCurio(GearHeartState.SLOT_ID, 0, stack);
        GearHeartEffects.tickPlayer(player);
    }
    private static void fullyRepair(GameTestHelper helper, ServerPlayer player) {
        for (int stage = 0; stage < GearHeartState.REPAIR_STAGES; stage++) {
            helper.assertTrue(GearHeartEffects.repairEquippedCurse(player, 1 << stage)
                == GearHeartEffects.RepairResult.REPAIRED, "Sequential repair must succeed");
        }
    }
    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.00001D, message);
    }
    @GameTest(template="empty")
    public static void teleportResendsInfiniteEffectsAndReachAfterRespawn(GameTestHelper h) {
        checkTeleportSync(h, false);
    }
    @GameTest(template="empty")
    public static void fullReleaseSurvivesActualDimensionTeleport(GameTestHelper h) {
        checkTeleportSync(h, true);
    }
    private static void checkTeleportSync(GameTestHelper h, boolean fullRelease) {
        var connection=new RecordingConnection();var p=equipped(h,connection);
        if (fullRelease) fullyRepair(h, p);
        var haste=p.getEffect(MobEffects.DIG_SPEED);
        var night=p.getEffect(MobEffects.NIGHT_VISION);
        try {
            connection.packets.clear();
            var destination=h.getLevel().getServer().getLevel(Level.NETHER);
            p.teleportTo(destination,0.5,80,0.5,0,0);
            h.assertTrue(p.serverLevel()==destination, "Actual cross-dimension command teleport must succeed");
            h.assertTrue(p.getEffect(MobEffects.DIG_SPEED)==haste && p.getEffect(MobEffects.NIGHT_VISION)==night,
                "Sync must preserve the existing infinite server instances");
            int respawn=-1, effects=0, attrs=0;
            for(int i=0;i<connection.packets.size();i++) {
                var packet=connection.packets.get(i);
                if(packet instanceof ClientboundRespawnPacket) respawn=i;
                if(packet instanceof ClientboundUpdateMobEffectPacket) {
                    h.assertTrue(respawn>=0 && i>respawn,"Effects must be resent after the client respawn packet"); effects++;
                }
                if(packet instanceof ClientboundUpdateAttributesPacket && i>respawn && respawn>=0) attrs++;
            }
            h.assertTrue(effects>=2 && attrs>=1,"Both heart effects and reach attributes must be resent");
            h.assertTrue(!haste.isVisible() && !night.isVisible() && haste.showIcon() && night.showIcon(),"Heart particles must be hidden while icons remain");
            var blockReach = p.getAttribute(ForgeMod.BLOCK_REACH.get());
            near(h, blockReach.getValue(), blockReach.getBaseValue() + (fullRelease ? 4 : 2),
                "Reach bonus must survive teleport");
            h.assertTrue(haste.getAmplifier() == (fullRelease ? 2 : 1) && night.getAmplifier() == 0,
                "Teleport must preserve the appropriate Gear Heart effect levels");
            h.succeed();
        } finally { close(p); }
    }
    @GameTest(template="empty")
    public static void hidesOldHeartParticlesAndPreservesStrongerExternalEffect(GameTestHelper h) {
        var connection=new RecordingConnection();var p=equipped(h,connection);
        try {
            p.removeEffect(MobEffects.DIG_SPEED);
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,-1,1,false,true,true));
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,200,2,false,true,true));
            GearHeartEffects.tickPlayer(p);
            h.assertTrue(!p.getEffect(MobEffects.DIG_SPEED).isVisible(),"Existing infinite heart effect must stop emitting particles");
            var stronger=p.getEffect(MobEffects.NIGHT_VISION);
            h.assertTrue(stronger.getAmplifier()==2 && stronger.getDuration()==200 && stronger.isVisible(),"Stronger external potion must retain level, duration and particles");
            p.removeEffect(MobEffects.NIGHT_VISION);
            GearHeartEffects.tickPlayer(p);
            h.assertTrue(p.getEffect(MobEffects.NIGHT_VISION).isInfiniteDuration() && !p.getEffect(MobEffects.NIGHT_VISION).isVisible(),"Lost effect must be restored without particles");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void everyRepairRaisesRpmAndFinalRepairUpgradesBonuses(GameTestHelper h) {
        var p = equipped(h, new RecordingConnection());
        try {
            int[] expected = {512, 1024, 2048, 4096, 5120, 8192, 10240};
            for (int stage = 0; stage < expected.length; stage++) {
                h.assertTrue(GearHeartEffects.repairEquippedCurse(p, 1 << stage)
                    == GearHeartEffects.RepairResult.REPAIRED, "Repair must take effect immediately");
                h.assertTrue(RpmLimitManager.getLimit(p.getUUID()) == expected[stage]
                    && RpmLimitManager.getEffectiveLimit(p.getUUID()) == expected[stage],
                    "Every repair must raise both the saved and effective RPM limit");
                h.assertTrue(p.getEffect(MobEffects.DIG_SPEED).getAmplifier() == (stage == 6 ? 2 : 1),
                    "Only full release grants Haste III");
            }
            h.assertTrue(GearHeartState.isBlessed(heart(p)) && !GearHeartEffects.hasQuota(p.getUUID()),
                "Final repair must replace the item and remove its industrial quotas");
            for (var attribute : java.util.List.of(ForgeMod.BLOCK_REACH.get(), ForgeMod.ENTITY_REACH.get())) {
                var instance = p.getAttribute(attribute);
                near(h, instance.getValue(), instance.getBaseValue() + 4, "Full release must grant reach +4");
            }
            var haste = p.getEffect(MobEffects.DIG_SPEED);
            var night = p.getEffect(MobEffects.NIGHT_VISION);
            GearHeartEffects.tickPlayer(p);
            h.assertTrue(p.getEffect(MobEffects.DIG_SPEED) == haste && p.getEffect(MobEffects.NIGHT_VISION) == night,
                "Unchanged full-release effects must not be recreated each tick");
            h.assertTrue(haste.isInfiniteDuration() && night.isInfiniteDuration()
                && !haste.isVisible() && !night.isVisible() && haste.showIcon() && night.showIcon(),
                "Full-release effects must remain infinite with hidden particles and visible icons");
            h.assertTrue(night.getAmplifier() == 0, "Night vision level must remain unchanged");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void damageMultiplierChangesFromPenaltyToBonusAndClearsWithoutHeart(GameTestHelper h) {
        var p = equipped(h, new RecordingConnection());
        try {
            var cursed = new LivingHurtEvent(p, p.damageSources().playerAttack(p), 10.25F);
            MinecraftForge.EVENT_BUS.post(cursed);
            near(h, cursed.getAmount(), 9.225D, "Damaged Gear Heart must retain the precise 10% penalty");
            fullyRepair(h, p);
            var released = new LivingHurtEvent(p, p.damageSources().playerAttack(p), 10.25F);
            MinecraftForge.EVENT_BUS.post(released);
            near(h, released.getAmount(), 15.375D, "Full release must deal 50% more damage without the old penalty");
            equip(p, ItemStack.EMPTY);
            var normal = new LivingHurtEvent(p, p.damageSources().playerAttack(p), 10.25F);
            MinecraftForge.EVENT_BUS.post(normal);
            near(h, normal.getAmount(), 10.25D, "No heart must leave outgoing damage unchanged");
            for (var attribute : java.util.List.of(ForgeMod.BLOCK_REACH.get(), ForgeMod.ENTITY_REACH.get())) {
                var instance = p.getAttribute(attribute);
                near(h, instance.getValue(), instance.getBaseValue(), "Removing the heart must clear reach modifiers");
            }
            h.assertTrue(!p.hasEffect(MobEffects.DIG_SPEED) && !p.hasEffect(MobEffects.NIGHT_VISION),
                "Removing the full-release heart must clear its own potion effects");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void reopeningAnEarlierCurseRestoresBaseBonusesAndRecomputesRpm(GameTestHelper h) {
        var p = equipped(h, new RecordingConnection());
        try {
            fullyRepair(h, p);
            var night = p.getEffect(MobEffects.NIGHT_VISION);
            equip(p, GearHeartState.reopenCurse(heart(p), 3));
            h.assertTrue(RpmLimitManager.getEffectiveLimit(p.getUUID()) == 1024,
                "Reopened third curse must restore the RPM of the first two completed stages");
            h.assertTrue(p.getEffect(MobEffects.DIG_SPEED).getAmplifier() == 1,
                "Reopened curse must downgrade this heart's Haste III to Haste II");
            near(h, GearHeartEffects.outgoingDamageMultiplier(p), 0.9D, "Reopened heart must restore its damage penalty");
            for (var attribute : java.util.List.of(ForgeMod.BLOCK_REACH.get(), ForgeMod.ENTITY_REACH.get())) {
                var instance = p.getAttribute(attribute);
                near(h, instance.getValue(), instance.getBaseValue() + 2, "Reopened heart must restore reach +2");
            }
            h.assertTrue(p.getEffect(MobEffects.NIGHT_VISION) == night, "Night vision must survive a state transition");
            h.assertTrue(GearHeartEffects.repairEquippedCurse(p, GearHeartState.MEK_ORE_OPERATIONS)
                == GearHeartEffects.RepairResult.REPAIRED && GearHeartState.isBlessed(heart(p)),
                "Repairing the reopened gap must restore the final item");
            h.assertTrue(RpmLimitManager.getEffectiveLimit(p.getUUID()) == 10240
                && p.getEffect(MobEffects.DIG_SPEED).getAmplifier() == 2,
                "Restored full release must regain its RPM and bonuses");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void explicitCommandRpmSurvivesAllRepairsAndReopenedCurse(GameTestHelper h) {
        var p = equipped(h, new RecordingConnection());
        try {
            RpmLimitManager.setLimit(p, 128);
            fullyRepair(h, p);
            RpmLimitManager.init(p);
            GearHeartEffects.tickPlayer(p);
            h.assertTrue(RpmLimitManager.getEffectiveLimit(p.getUUID()) == 128,
                "Full release and login must preserve an explicit lower command limit");
            RpmLimitManager.setLimit(p, 10240);
            equip(p, GearHeartState.reopenCurse(heart(p), 1));
            h.assertTrue(RpmLimitManager.getEffectiveLimit(p.getUUID()) == 10240,
                "Reopened first curse must preserve an explicit higher command limit");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void strongerExternalEffectsSurviveFullReleaseAndReopenedCurse(GameTestHelper h) {
        var p = equipped(h, new RecordingConnection());
        try {
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 3, false, true, true));
            var external = p.getEffect(MobEffects.DIG_SPEED);
            fullyRepair(h, p);
            equip(p, GearHeartState.reopenCurse(heart(p), 7));
            h.assertTrue(p.getEffect(MobEffects.DIG_SPEED) == external
                && external.getDuration() == 200 && external.isVisible(),
                "State transitions must preserve a stronger external effect and its display flags");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void legacyEnchantedPlaceholderMigratesWithoutFullReleaseBonuses(GameTestHelper h) {
        var p = equipped(h, new RecordingConnection());
        try {
            var legacy = GearHeartState.finalizeRepair(heart(p));
            legacy.getOrCreateTag().putInt("CustomModelData", 2);
            GearHeartState.setRepairedMask(legacy, 0b0000011);
            equip(p, legacy);
            h.assertTrue(GearHeartState.isNormalGearHeart(heart(p)) && !GearHeartState.isFullyReleased(heart(p)),
                "Legacy enchanted placeholder must migrate to the damaged item");
            h.assertTrue(RpmLimitManager.getEffectiveLimit(p.getUUID()) == 1024
                && p.getEffect(MobEffects.DIG_SPEED).getAmplifier() == 1,
                "Legacy placeholder must receive only its actual repaired-stage RPM and base haste");
            near(h, GearHeartEffects.outgoingDamageMultiplier(p), 0.9D,
                "Legacy placeholder must not receive the full-release damage bonus");
            h.succeed();
        } finally { close(p); }
    }
}
