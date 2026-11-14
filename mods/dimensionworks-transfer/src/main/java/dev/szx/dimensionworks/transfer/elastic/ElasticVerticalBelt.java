package dev.szx.dimensionworks.transfer.elastic;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import dev.szx.dimensionworks.transfer.compat.PackedPlayerTransport;
import dev.szx.dimensionworks.transfer.tutorial.TutorialManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** 玩家贴住竖直带的外侧表面运输，不进入 Create 的物品库存或普通带乘客表。 */
public final class ElasticVerticalBelt {
    private static final double CONTACT_GAP = 1.0 / 16;
    private static final double COLLISION_EPSILON = 1.0e-5;

    public record Contact(BlockPos pos, Direction side, double speed, double gap, double center) {}

    private ElasticVerticalBelt() {}

    public static Contact findContact(Player player) {
        if (!ElasticParcelState.isElastic(player) || !player.isAlive() || player.isSpectator()
            || player.isShiftKeyDown() || player.getAbilities().flying || player.isPassenger()
            || PackedPlayerTransport.ignoresBlocks(player)
            || player.level().isClientSide && dev.szx.dimensionworks.transfer.client.TutorialOverlay.isLocked()
            || player instanceof ServerPlayer serverPlayer && TutorialManager.locked(serverPlayer)) return null;

        AABB box = player.getBoundingBox();
        Contact closest = null;
        // 只查包裹碰撞箱附近的已加载位置；不遍历整条带或查询其他实体。
        AABB nearby = box.inflate(CONTACT_GAP, 0, CONTACT_GAP);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(nearby.minX, nearby.minY, nearby.minZ),
                                                  BlockPos.containing(nearby.maxX, nearby.maxY, nearby.maxZ))) {
            if (!player.level().hasChunkAt(pos)) continue;
            var state = player.level().getBlockState(pos);
            if (!AllBlocks.BELT.has(state) || state.getValue(BeltBlock.SLOPE) != BeltSlope.VERTICAL
                || !(player.level().getBlockEntity(pos) instanceof BeltBlockEntity belt)
                || Math.abs(belt.getSpeed()) < 1) continue;
            var shape = state.getCollisionShape(player.level(), pos, CollisionContext.of(player));
            if (shape.isEmpty()) continue;
            AABB surface = shape.bounds().move(pos);
            if (box.minY >= surface.maxY || box.maxY <= surface.minY) continue;
            Direction.Axis axis = state.getValue(BeltBlock.HORIZONTAL_FACING).getAxis();
            boolean alongX = axis == Direction.Axis.X;
            if (alongX ? box.minZ >= surface.maxZ || box.maxZ <= surface.minZ
                       : box.minX >= surface.maxX || box.maxX <= surface.minX) continue;

            double positiveGap = alongX ? box.minX - surface.maxX : box.minZ - surface.maxZ;
            double negativeGap = alongX ? surface.minX - box.maxX : surface.minZ - box.maxZ;
            Direction side;
            double gap;
            if (positiveGap >= -COLLISION_EPSILON && positiveGap <= CONTACT_GAP) {
                side = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
                gap = positiveGap;
            } else if (negativeGap >= -COLLISION_EPSILON && negativeGap <= CONTACT_GAP) {
                side = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
                gap = negativeGap;
            } else continue;

            // 由轴的转向与接触面推导：同一竖直带的两侧运动方向相反。
            // 正转时东面向上、南面向下；反转同时反转两面，与 Create 带面渲染一致。
            double speed = belt.getBeltMovementSpeed() * side.getAxisDirection().getStep() * (alongX ? 1 : -1);
            double center = alongX ? pos.getZ() + .5 : pos.getX() + .5;
            if (closest == null || gap < closest.gap())
                closest = new Contact(pos.immutable(), side, speed, Math.max(0, gap), center);
        }
        return closest;
    }

    /** 在 LivingEntity.travel 中替代这一次普通重力运动，保持原有方块碰撞。 */
    public static boolean travel(Player player, Vec3 input) {
        Contact contact = findContact(player);
        if (contact == null) return false;
        boolean alongX = contact.side().getAxis() == Direction.Axis.X;
        double pull = -contact.side().getAxisDirection().getStep() * Math.min(contact.gap(), Math.abs(contact.speed()));
        double center = 0;
        if (input.x == 0 && input.z == 0) {
            double offset = contact.center() - (alongX ? player.getZ() : player.getX());
            center = offset * Math.min(Math.abs(contact.speed()), .1) * 4;
        }
        player.setDeltaMovement(alongX ? pull : center, contact.speed(), alongX ? center : pull);
        // 保留玩家横向操作，允许自行走离带面；不累加上一 tick 的输送速度。
        player.moveRelative(player.getSpeed(), input.multiply(1, 0, 1));
        Vec3 before = player.position();
        player.move(MoverType.SELF, player.getDeltaMovement());
        // 被地板/天花板挡住时不把碰撞产生的史莱姆反弹积累为额外推力。
        player.setDeltaMovement(player.position().subtract(before));
        player.setOnGround(false);
        player.fallDistance = 0;
        player.calculateEntityAnimation(false);
        return true;
    }
}
