package dev.szx.dimensionworks.transfer;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.items.mobile_packager.PackPlayerPacket;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState;
import de.theidler.create_mobile_packages.items.mobile_packager.CMPPlayerPack;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import dev.szx.dimensionworks.transfer.elastic.*;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dimensionworks_elastic")
@PrefixGameTestTemplate(false)
public final class ElasticParcelGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "ElasticTest"));
        p.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), p);
        p.setItemInHand(InteractionHand.MAIN_HAND, ElasticItems.PACKAGER.get().getDefaultInstance());
        return p;
    }
    private static void close(ServerPlayer p) { ElasticParcelState.resetCharge(p); p.discard(); }
    private static void pack(ServerPlayer p) { PackPlayerPacket.pack(p, p, true); }
    private static void checkVec(GameTestHelper h, Vec3 actual, Vec3 expected, String message) {
        h.assertTrue(actual.distanceTo(expected) < 1e-6, message + ": " + actual + " != " + expected);
    }

    @GameTest(template="empty")
    public static void normalAndElasticToolsRetainSameBaseProperties(GameTestHelper h) {
        var elastic = player(h); var normal = player(h);
        try {
            normal.setItemInHand(InteractionHand.MAIN_HAND, CMPItems.MOBILE_PACKAGER.asStack());
            pack(elastic); pack(normal);
            h.assertTrue(ElasticParcelState.isElastic(elastic) && !ElasticParcelState.isElastic(normal), "Only the upgraded tool selects elastic state");
            var a=elastic.getDimensions(elastic.getPose()); var b=normal.getDimensions(normal.getPose());
            h.assertTrue(a.width==b.width && a.height==b.height, "Package dimensions must match");
            h.assertTrue(elastic.getAttributeValue(Attributes.MOVEMENT_SPEED) == normal.getAttributeValue(Attributes.MOVEMENT_SPEED), "Movement modifier must match");
            h.assertTrue(elastic.getPersistentData().getInt(PackedPlayerState.ESCAPES) == normal.getPersistentData().getInt(PackedPlayerState.ESCAPES), "Escape state must match");
            var style = ElasticItems.PACKAGER.get().getRarity(ElasticItems.PACKAGER.get().getDefaultInstance()).getStyleModifier().apply(net.minecraft.network.chat.Style.EMPTY);
            h.assertTrue(style.getColor().getValue() == ChatFormatting.GREEN.getColor(), "Tool rarity must actually be green");
            h.assertTrue(PlayerParcel.unpack(elastic) && !elastic.getPersistentData().contains(ElasticParcelState.ELASTIC), "Unpack must clear elastic state");
            h.succeed();
        } finally { close(elastic); close(normal); }
    }

    @GameTest(template="empty")
    public static void immunityIsPreservedAndActualOffhandSelectsElastic(GameTestHelper h) {
        var source = player(h); var target = player(h);
        try {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(CMPPlayerPack.IMMUNITY.get(), 200));
            PackPlayerPacket.pack(source, target, false);
            h.assertTrue(!PlayerParcel.isPacked(target) && !ElasticParcelState.isElastic(target), "Existing immunity must reject packing");
            target.removeEffect(CMPPlayerPack.IMMUNITY.get());
            source.setShiftKeyDown(true);
            source.setItemInHand(InteractionHand.MAIN_HAND, CMPItems.MOBILE_PACKAGER.asStack());
            ItemStack tool = ElasticItems.PACKAGER.get().getDefaultInstance();
            source.setItemInHand(InteractionHand.OFF_HAND, tool);
            tool.interactLivingEntity(source, target, InteractionHand.OFF_HAND);
            h.assertTrue(ElasticParcelState.isElastic(target), "The actual offhand interaction must select elastic");
            h.assertTrue(target.getPersistentData().getBoolean(PackedPlayerState.PACKED_BY_OTHER), "Packed-by-other flag must be preserved");
            for (int i=0;i<5;i++) PackedPlayerState.escape(target);
            h.assertTrue(!ElasticParcelState.isElastic(target) && target.hasEffect(CMPPlayerPack.IMMUNITY.get()), "Existing five-click escape and immunity must work");
            target.removeEffect(CMPPlayerPack.IMMUNITY.get());
            source.setItemInHand(InteractionHand.MAIN_HAND, tool);
            ItemStack ordinary=CMPItems.MOBILE_PACKAGER.asStack();
            source.setItemInHand(InteractionHand.OFF_HAND, ordinary);
            ordinary.interactLivingEntity(source,target,InteractionHand.OFF_HAND);
            h.assertTrue(PlayerParcel.isPacked(target) && !ElasticParcelState.isElastic(target), "Normal offhand tool must still produce an ordinary package");
            h.succeed();
        } finally { close(source); close(target); }
    }

    @GameTest(template="empty")
    public static void landingUsesVanillaSlimeBounceAndShiftSuppression(GameTestHelper h) {
        var p = player(h);
        BlockPos ground = h.absolutePos(new BlockPos(2, 1, 2));
        h.setBlock(new BlockPos(2,1,2), Blocks.STONE);
        try {
            pack(p);
            p.setPos(ground.getX()+.5,ground.getY()+1.3,ground.getZ()+.5);
            p.setDeltaMovement(.2,-1,.3);
            p.move(MoverType.SELF,new Vec3(0,-1,0));
            checkVec(h,p.getDeltaMovement(),new Vec3(.2,1,.3),"Stone landing must bounce like slime without changing horizontal velocity");
            p.setShiftKeyDown(true);
            p.setPos(ground.getX()+.5,ground.getY()+1.3,ground.getZ()+.5);
            p.setDeltaMovement(.2,-1,.3);
            p.move(MoverType.SELF,new Vec3(0,-1,0));
            checkVec(h,p.getDeltaMovement(),new Vec3(.2,0,.3),"Shift must suppress rebound");
            PlayerParcel.unpack(p); p.setShiftKeyDown(false);
            p.setPos(ground.getX()+.5,ground.getY()+1.3,ground.getZ()+.5);
            p.setDeltaMovement(0,-1,0);
            p.move(MoverType.SELF,new Vec3(0,-1,0));
            h.assertTrue(p.getDeltaMovement().y==0, "Normal unpacked players must not acquire rebound");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void serverChargeCapsAndLaunchesAlongLookDirection(GameTestHelper h) {
        var p=player(h);
        try {
            pack(p); p.setOnGround(true);
            ElasticParcelState.input(p,true,-30,90);
            for(int i=0;i<40;i++) ElasticParcelState.tick(p);
            ElasticParcelState.input(p,false,-30,90);
            checkVec(h,p.getDeltaMovement(),Vec3.directionFromRotation(-30,90).scale(18.5625),"Two-second full launch must use look pitch and yaw with five times the previous full strength");
            p.setDeltaMovement(Vec3.ZERO); p.setOnGround(true);
            ElasticParcelState.input(p,false,-30,90);
            checkVec(h,p.getDeltaMovement(),Vec3.ZERO,"Repeated release must not launch again");
            ElasticParcelState.input(p,true,0,0);
            for(int i=0;i<20;i++) ElasticParcelState.tick(p);
            ElasticParcelState.input(p,false,0,0);
            checkVec(h,p.getDeltaMovement(),Vec3.directionFromRotation(0,0).scale(1.4708333333333334),"Partial charge must retain its previous strength");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void airAndUnpackCancelChargeAndNormalPlayersCannotLaunch(GameTestHelper h) {
        var p=player(h);
        try {
            pack(p); p.setOnGround(true);
            ElasticParcelState.input(p,true,0,0);
            for(int i=0;i<40;i++) ElasticParcelState.tick(p);
            p.setOnGround(false); ElasticParcelState.tick(p);
            p.setOnGround(true); p.setDeltaMovement(Vec3.ZERO);
            ElasticParcelState.input(p,false,0,0);
            checkVec(h,p.getDeltaMovement(),Vec3.ZERO,"Air must cancel charge");
            ElasticParcelState.input(p,true,0,0);
            for(int i=0;i<40;i++) ElasticParcelState.tick(p);
            PlayerParcel.unpack(p); p.setOnGround(true);
            ElasticParcelState.input(p,false,0,0);
            checkVec(h,p.getDeltaMovement(),Vec3.ZERO,"Unpack must cancel pending launch");
            ElasticParcelState.input(p,true,0,0);
            for(int i=0;i<60;i++) ElasticParcelState.tick(p);
            ElasticParcelState.input(p,false,0,0);
            checkVec(h,p.getDeltaMovement(),Vec3.ZERO,"Unpacked players cannot use charge packets");
            pack(p); p.setOnGround(true);
            var tutorial=new CompoundTag();
            dev.szx.dimensionworks.transfer.tutorial.TutorialSavedData.get(p).put(p.getUUID(),tutorial);
            ElasticParcelState.input(p,true,0,0);
            for(int i=0;i<60;i++) ElasticParcelState.tick(p);
            ElasticParcelState.input(p,false,0,0);
            checkVec(h,p.getDeltaMovement(),Vec3.ZERO,"Existing tutorial lock must also block charge");
            tutorial.putBoolean("Done",true);
            dev.szx.dimensionworks.transfer.tutorial.TutorialSavedData.get(p).put(p.getUUID(),tutorial);
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template="empty")
    public static void nonPlayerPackagingRemainsOrdinaryMobPackage(GameTestHelper h) {
        var p=player(h); var mob=EntityType.PIG.create(h.getLevel());
        try {
            mob.setCustomName(Component.literal("Elastic mob regression"));
            mob.setPos(p.position());
            ItemStack tool=p.getMainHandItem();
            var result=tool.interactLivingEntity(p,mob,InteractionHand.MAIN_HAND);
            h.assertTrue(result.consumesAction() && mob.isRemoved(),"Existing mob packing must still consume the interaction");
            ItemStack box=p.getInventory().items.stream().filter(PackageItem::isPackage).findFirst().orElseThrow();
            ItemStack egg=PackageItem.getContents(box).getStackInSlot(0);
            h.assertTrue(egg.is(CMPItems.PACKED_MOB_SPAWN_EGG.get()),"Mobs must remain the existing ordinary packed-mob item");
            h.assertTrue(!ElasticParcelState.isElastic(p) && !egg.getOrCreateTag().contains(ElasticParcelState.ELASTIC),"No elastic mob or player state may be created");
            h.succeed();
        } finally { close(p); mob.discard(); }
    }

    @GameTest(template="empty")
    public static void upgradePreservesCompleteToolNbtAndLeavesInputIntact(GameTestHelper h) {
        ItemStack original=CMPItems.MOBILE_PACKAGER.asStack();
        original.setHoverName(Component.literal("My original packager"));
        original.enchant(Enchantments.UNBREAKING,2);
        original.getOrCreateTag().putString("packager_payload","address and saved settings");
        CompoundTag before=original.save(new CompoundTag());
        ItemStack upgraded=ElasticPackager.upgrade(original);
        h.assertTrue(upgraded.is(ElasticItems.PACKAGER.get()),"Upgrade must change the registered item");
        h.assertTrue(upgraded.getTag().equals(original.getTag()),"All original item NBT must survive");
        h.assertTrue(original.save(new CompoundTag()).equals(before),"Crafting preview must not mutate the input");
        h.succeed();
    }
}
