package dev.szx.dimensionworks.transfer;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.items.mobile_packager.PackPlayerPacket;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import dev.szx.dimensionworks.transfer.elastic.ElasticItems;
import dev.szx.dimensionworks.transfer.elastic.ElasticParcelState;
import dev.szx.dimensionworks.transfer.elastic.ElasticVerticalBelt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dimensionworks_elastic")
@PrefixGameTestTemplate(false)
public final class ElasticVerticalBeltGameTests {
    private static ServerPlayer player(GameTestHelper h, boolean elastic) {
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "BeltParcel"));
        p.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), p);
        p.setItemInHand(InteractionHand.MAIN_HAND, elastic ? ElasticItems.PACKAGER.get().getDefaultInstance() : CMPItems.MOBILE_PACKAGER.asStack());
        PackPlayerPacket.pack(p, p, true);
        return p;
    }

    private static BlockPos belt(GameTestHelper h, Direction facing, float rpm) {
        BlockPos base = h.absolutePos(new BlockPos(3, 1, 3));
        // 更换轴向前拆除整条旧带，避免中途混合新旧 facing 导致 Create 拆链。
        if (AllBlocks.BELT.has(h.getLevel().getBlockState(base))) h.getLevel().removeBlock(base, false);
        boolean positive = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        for (int index = 0; index < 6; index++) {
            BeltPart part = index == 0 ? (positive ? BeltPart.START : BeltPart.END)
                    : index == 5 ? (positive ? BeltPart.END : BeltPart.START) : BeltPart.MIDDLE;
            h.getLevel().setBlockAndUpdate(base.above(index), AllBlocks.BELT.getDefaultState()
                    .setValue(BeltBlock.SLOPE, BeltSlope.VERTICAL).setValue(BeltBlock.HORIZONTAL_FACING, facing)
                    .setValue(BeltBlock.PART, part));
        }
        BeltBlock.initBelt(h.getLevel(), base);
        speed(h, base, rpm);
        return base;
    }

    private static void speed(GameTestHelper h, BlockPos base, float rpm) {
        for (int index = 0; index < 6; index++) ((BeltBlockEntity) h.getLevel().getBlockEntity(base.above(index))).setSpeed(rpm);
    }

    private static void touch(GameTestHelper h, ServerPlayer p, BlockPos pos, Direction side, double y) {
        var state = h.getLevel().getBlockState(pos);
        AABB shape = state.getCollisionShape(h.getLevel(), pos, CollisionContext.of(p)).bounds().move(pos);
        double half = p.getBbWidth() / 2;
        double x = pos.getX() + .5, z = pos.getZ() + .5;
        if (side == Direction.EAST) x = shape.maxX + half;
        if (side == Direction.WEST) x = shape.minX - half;
        if (side == Direction.SOUTH) z = shape.maxZ + half;
        if (side == Direction.NORTH) z = shape.minZ - half;
        p.setPos(x, y, z);
        p.setOnGround(false);
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static void close(ServerPlayer p) { ElasticParcelState.resetCharge(p); p.discard(); }

    @GameTest(template = "empty")
    public static void bothFacesAndBothRotationSignsMoveVerticallyForEveryAxis(GameTestHelper h) {
        var p = player(h, true);
        try {
            for (Direction facing : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos base = belt(h, facing, 96);
                for (Direction side : new Direction[]{facing, facing.getOpposite()}) {
                    for (float rpm : new float[]{96, -96}) {
                        speed(h, base, rpm);
                        touch(h, p, base.above(2), side, base.getY() + 2.2);
                        double before = p.getY();
                        // 东面正转上行、南面正转下行；另外两面相反，不依据方块 facing 名称猜方向。
                        int sign = side == Direction.EAST || side == Direction.NORTH ? 1 : -1;
                        p.setDeltaMovement(0, -2, 0);
                        p.travel(Vec3.ZERO);
                        h.assertTrue(Math.abs(p.getY() - before - sign * rpm / 480) < 1e-6,
                                "Actual travel must follow moving surface: " + facing + " / " + side + " / " + rpm);
                    }
                }
            }
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template = "empty")
    public static void crossesSegmentsWithoutGravityOrVelocityAccumulationAndReverses(GameTestHelper h) {
        var p = player(h, true);
        try {
            BlockPos base = belt(h, Direction.SOUTH, -48);
            touch(h, p, base.above(1), Direction.SOUTH, base.getY() + 1.2);
            double before = p.getY();
            for (int tick = 0; tick < 30; tick++) {
                p.fallDistance = 20;
                p.travel(Vec3.ZERO);
                h.assertTrue(Math.abs(p.getDeltaMovement().y - .1) < 1e-6 && p.fallDistance == 0,
                        "Every segment must carry at fixed belt speed and reset falling");
                h.assertTrue(!p.noPhysics && !p.isNoGravity() && !p.onGround(), "No noclip, gravity flag or grounded charge may remain");
            }
            h.assertTrue(Math.abs(p.getY() - before - 3) < 1e-6, "Must cross three segment joins without losing speed");
            speed(h, base, 48);
            p.travel(Vec3.ZERO);
            h.assertTrue(Math.abs(p.getDeltaMovement().y + .1) < 1e-6, "Reversal must apply on the next movement");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template = "empty")
    public static void ordinaryPackagesUnpackedPlayersAndStaleElasticFlagsCannotRide(GameTestHelper h) {
        var normal = player(h, false);
        var elastic = player(h, true);
        try {
            BlockPos base = belt(h, Direction.SOUTH, -96);
            touch(h, normal, base.above(2), Direction.SOUTH, base.getY() + 2.2);
            h.assertTrue(ElasticVerticalBelt.findContact(normal) == null, "An ordinary CMP package must not acquire vertical transport");
            touch(h, elastic, base.above(2), Direction.SOUTH, base.getY() + 2.2);
            h.assertTrue(ElasticVerticalBelt.findContact(elastic) != null, "Elastic package must attach at the same surface");
            PlayerParcel.unpack(elastic);
            ElasticParcelState.setElastic(elastic, true);
            h.assertTrue(ElasticVerticalBelt.findContact(elastic) == null, "A stale elastic marker without CMP package state must not authorize riding");
            h.assertTrue(!BeltBlock.canTransportObjects(h.getLevel().getBlockState(base)), "Create's original item/entity restriction must remain intact");
            h.succeed();
        } finally { close(normal); close(elastic); }
    }

    @GameTest(template = "empty")
    public static void stopShiftUnpackAndLeavingEndpointsRestoreNormalMovement(GameTestHelper h) {
        var p = player(h, true);
        try {
            BlockPos base = belt(h, Direction.SOUTH, -48);
            touch(h, p, base.above(2), Direction.SOUTH, base.getY() + 2.2);
            p.setShiftKeyDown(true);
            h.assertTrue(ElasticVerticalBelt.findContact(p) == null, "Shift releases the belt");
            p.setShiftKeyDown(false);
            speed(h, base, 0);
            p.travel(Vec3.ZERO);
            h.assertTrue(ElasticVerticalBelt.findContact(p) == null && p.getDeltaMovement().y < 0, "Stopped belts must restore gravity");
            speed(h, base, -48);
            touch(h, p, base.above(5), Direction.SOUTH, base.getY() + 5.9);
            for (int tick = 0; tick < 4; tick++) p.travel(Vec3.ZERO);
            h.assertTrue(ElasticVerticalBelt.findContact(p) == null, "After the upper endpoint there must be no invisible grip");
            touch(h, p, base, Direction.SOUTH, base.getY() - .4);
            speed(h, base, 48);
            for (int tick = 0; tick < 4; tick++) p.travel(Vec3.ZERO);
            h.assertTrue(ElasticVerticalBelt.findContact(p) == null, "After the lower endpoint there must be no invisible grip");
            touch(h, p, base.above(2), Direction.SOUTH, base.getY() + 2.2);
            p.setPos(p.getX(), p.getY(), p.getZ() + .2);
            h.assertTrue(ElasticVerticalBelt.findContact(p) == null, "The belt must not attract a nearby detached player");
            touch(h, p, base.above(2), Direction.SOUTH, base.getY() + 2.2);
            PlayerParcel.unpack(p);
            h.assertTrue(ElasticVerticalBelt.findContact(p) == null && !p.noPhysics && !p.isNoGravity(), "Unpacking immediately restores ordinary movement");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template = "empty")
    public static void ceilingAndFloorBlockTransportWithoutNoclipOrReboundAcceleration(GameTestHelper h) {
        var p = player(h, true);
        try {
            BlockPos base = belt(h, Direction.SOUTH, -96);
            BlockPos segment = base.above(2);
            BlockPos ceiling = segment.south().above();
            h.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
            touch(h, p, segment, Direction.SOUTH, ceiling.getY() - p.getBbHeight() - .05);
            p.travel(Vec3.ZERO);
            h.assertTrue(p.getBoundingBox().maxY <= ceiling.getY() + 1e-6, "Upward transport must collide with the ceiling");
            BlockPos floor = segment.south().below();
            h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            speed(h, base, 96);
            touch(h, p, segment, Direction.SOUTH, floor.getY() + 1.05);
            p.travel(Vec3.ZERO);
            h.assertTrue(p.getY() >= floor.getY() + 1 - 1e-6 && p.getDeltaMovement().y <= 0,
                    "Downward transport must collide with floor and avoid slime rebound acceleration while carried");
            h.assertTrue(!p.noPhysics && !p.isNoGravity(), "Transport must not leave temporary physics flags behind");
            h.succeed();
        } finally { close(p); }
    }

    @GameTest(template = "empty")
    public static void flightPassengersTutorialAndOtherBeltSlopesKeepTheirExistingRules(GameTestHelper h) {
        var p = player(h, true);
        try {
            BlockPos base = belt(h, Direction.SOUTH, -96);
            BlockPos segment = base.above(2);
            touch(h, p, segment, Direction.SOUTH, segment.getY() + .2);
            p.getAbilities().flying = true;
            h.assertTrue(ElasticVerticalBelt.findContact(p) == null, "Flight must not be captured");
            p.getAbilities().flying = false;
            var pig = EntityType.PIG.create(h.getLevel());
            p.startRiding(pig, true);
            h.assertTrue(ElasticVerticalBelt.findContact(p) == null, "A mounted player must not be captured");
            p.stopRiding();
            touch(h, p, segment, Direction.SOUTH, segment.getY() + .2);
            var tutorial = new net.minecraft.nbt.CompoundTag();
            dev.szx.dimensionworks.transfer.tutorial.TutorialSavedData.get(p).put(p.getUUID(), tutorial);
            h.assertTrue(ElasticVerticalBelt.findContact(p) == null, "The tutorial movement lock must take priority");
            tutorial.putBoolean("Done", true);
            for (BeltSlope slope : new BeltSlope[]{BeltSlope.HORIZONTAL, BeltSlope.UPWARD, BeltSlope.DOWNWARD, BeltSlope.SIDEWAYS}) {
                h.getLevel().setBlockAndUpdate(segment, h.getLevel().getBlockState(segment).setValue(BeltBlock.SLOPE, slope));
                h.assertTrue(ElasticVerticalBelt.findContact(p) == null, "Only VERTICAL may enter the new movement path: " + slope);
            }
            h.succeed();
        } finally { close(p); }
    }
}
