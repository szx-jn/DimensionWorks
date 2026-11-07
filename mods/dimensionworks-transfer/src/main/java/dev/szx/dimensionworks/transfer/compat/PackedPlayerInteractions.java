package dev.szx.dimensionworks.transfer.compat;

import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import dev.szx.dimensionworks.transfer.tutorial.TutorialManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Player parcels are living contents: unpack them, never insert their inventory into machines. */
public final class PackedPlayerInteractions {
    private PackedPlayerInteractions() {}

    public static boolean unpackAt(ServerPlayer player, PackagerBlockEntity packager) {
        if (!PlayerParcel.isPacked(player) || TutorialManager.locked(player)
            || packager.getLevel() != player.level() || packager.animationTicks != 0 || !packager.heldBox.isEmpty()) return false;
        // The player has no item contents to insert; use CMP's one authoritative unpack operation.
        return PlayerParcel.unpack(player);
    }

    public static void tick(ServerPlayer player) {
        if (!PlayerParcel.isPacked(player) || TutorialManager.locked(player)) return;
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1,-1,-1), center.offset(1,1,1))) {
            if (!player.level().hasChunkAt(pos)) continue;
            if (!(player.level().getBlockEntity(pos) instanceof PackagerBlockEntity packager)) continue;
            var facing = packager.getBlockState().getValue(PackagerBlock.FACING);
            // Only the front intake: standing beside/behind a machine must not unwrap the player.
            AABB mouth = new AABB(pos).move(facing.getStepX() * .75, facing.getStepY() * .75, facing.getStepZ() * .75);
            if (mouth.intersects(player.getBoundingBox()) && unpackAt(player, packager)) return;
        }
    }
}
