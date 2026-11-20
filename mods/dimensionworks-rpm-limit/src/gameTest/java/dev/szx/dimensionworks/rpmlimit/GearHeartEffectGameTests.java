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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(DimensionWorksRpmLimit.MOD_ID)
@PrefixGameTestTemplate(false)
public class GearHeartEffectGameTests {
    static class RecordingConnection extends Connection {
        final ArrayList<Packet<?>> packets = new ArrayList<>();
        RecordingConnection() {
            super(PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(this);
            net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(this);
        }
        @Override public void send(Packet<?> packet, PacketSendListener listener, boolean flush) { packets.add(packet); }
    }
    static ServerPlayer equipped(GameTestHelper h, RecordingConnection connection) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"HeartTest"), net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,p, new net.minecraft.server.network.CommonListenerCookie(p.getGameProfile(), 0, net.minecraft.server.level.ClientInformation.createDefault(), false, net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE));
        RpmLimitManager.init(p);
        h.assertTrue(GearHeartEffects.ensureGearHeartSlots(p), "Test player must receive the existing Gear Heart slot/item");
        GearHeartEffects.tickPlayer(p);
        return p;
    }
    static void close(ServerPlayer p) {
        GearHeartEffects.clearPlayer(p);
        p.serverLevel().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        ((io.netty.channel.embedded.EmbeddedChannel)p.connection.getConnection().channel()).finishAndReleaseAll();
    }
    private static ItemStack heart(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player).orElseThrow()
            .findCurios(GearHeartState.SLOT_ID).get(0).stack();
    }
    private static void equip(ServerPlayer player, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).orElseThrow()
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
            var blockReach = p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
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
            for (var attribute : java.util.List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE)) {
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
            var cursed = new LivingIncomingDamageEvent(p, new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().playerAttack(p), 10.25F));
            NeoForge.EVENT_BUS.post(cursed);
            near(h, cursed.getAmount(), 9.225D, "Damaged Gear Heart must retain the precise 10% penalty");
            fullyRepair(h, p);
            var released = new LivingIncomingDamageEvent(p, new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().playerAttack(p), 10.25F));
            NeoForge.EVENT_BUS.post(released);
            near(h, released.getAmount(), 15.375D, "Full release must deal 50% more damage without the old penalty");
            equip(p, ItemStack.EMPTY);
            var normal = new LivingIncomingDamageEvent(p, new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().playerAttack(p), 10.25F));
            NeoForge.EVENT_BUS.post(normal);
            near(h, normal.getAmount(), 10.25D, "No heart must leave outgoing damage unchanged");
            for (var attribute : java.util.List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE)) {
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
            for (var attribute : java.util.List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE)) {
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
    public static void repairedAndReopenedHeartPreservesComponentsThroughDiskEncoding(GameTestHelper h) {
        var p = equipped(h, new RecordingConnection());
        try {
            var stack = heart(p);
            var name = net.minecraft.network.chat.Component.literal("组件保留测试");
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
            stack.enchant(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 3);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                stack, tag -> tag.putString("dw_test_marker", "kept"));
            h.assertTrue(stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA).value() == 1,
                "未修复齿轮之心必须使用模型组件 1");
            fullyRepair(h, p);
            var blessed = heart(p);
            h.assertTrue(!blessed.has(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA),
                "完全修复必须清除损坏模型组件");
            var reopened = GearHeartState.reopenCurse(blessed, 3);
            var loaded = ItemStack.parse(h.getLevel().registryAccess(), reopened.save(h.getLevel().registryAccess())).orElseThrow();
            h.assertTrue(GearHeartState.getRepairedMask(loaded) == 0b1111011,
                "重新打开第三项诅咒的标记必须在落盘后保持");
            h.assertTrue(loaded.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA).value() == 1,
                "重新打开诅咒必须恢复损坏模型");
            h.assertTrue(name.equals(loaded.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
                && loaded.getEnchantments().equals(blessed.getEnchantments())
                && loaded.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getString("dw_test_marker").equals("kept"),
                "修复、物品转换和落盘不得丢失名称、附魔或第三方自定义数据");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void offlineProfileReadsActualNeoForgeAndCuriosSave(GameTestHelper h) {
        var p = equipped(h, new RecordingConnection());
        try {
            for (int stage = 0; stage < 3; stage++) GearHeartEffects.repairEquippedCurse(p, 1 << stage);
            var saved = p.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            var profile = RpmPlayerState.fromPlayerNbt(saved);
            h.assertTrue(saved.contains("NeoForgeData") && saved.getCompound("neoforge:attachments").contains("curios:inventory"),
                "测试必须使用实际 NeoForge/Curios 附件序列化");
            h.assertTrue(profile.base() == 2048 && profile.effective() == 2048 && !profile.commandOverride(),
                "从实际玩家文件恢复的 RPM 必须匹配齿轮之心的第三修复阶段");
            RpmLimitManager.setLimit(p, 64);
            var explicit = RpmPlayerState.fromPlayerNbt(p.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
            h.assertTrue(explicit.base() == 64 && explicit.effective() == 64 && explicit.commandOverride(),
                "实际落盘的命令限速不得被齿轮之心覆盖");
            h.succeed();
        } finally { close(p); }
    }
}
