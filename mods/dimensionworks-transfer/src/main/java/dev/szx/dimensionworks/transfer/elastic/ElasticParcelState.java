package dev.szx.dimensionworks.transfer.elastic;

import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager;
import dev.szx.dimensionworks.transfer.compat.PlayerParcel;
import dev.szx.dimensionworks.transfer.compat.PackedPlayerTransport;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ElasticParcelState {
    public static final String ELASTIC = "dimensionworks_elastic_parcel";
    private static final Map<UUID, ElasticCharge> CHARGES = new HashMap<>();

    private ElasticParcelState() {}

    public static boolean isElastic(Player player) {
        return PlayerParcel.isPacked(player) && player.getPersistentData().getBoolean(ELASTIC);
    }

    public static void setElastic(Player player, boolean elastic) {
        if (elastic) player.getPersistentData().putBoolean(ELASTIC, true);
        else player.getPersistentData().remove(ELASTIC);
    }

    public static void selectTool(ServerPlayer source, Player target) {
        ItemStack tool = source.getMainHandItem();
        if (!(tool.getItem() instanceof MobilePackager)) tool = source.getOffhandItem();
        setElastic(target, tool.getItem() instanceof ElasticPackager);
    }

    public static void clear(ServerPlayer player) {
        setElastic(player, false);
        resetCharge(player);
    }

    public static void synchronize(ServerPlayer player) {
        ElasticNetwork.sync(player);
    }

    public static boolean canCharge(Player player) {
        return isElastic(player) && player.isAlive() && player.onGround()
            && !player.isPassenger() && !PackedPlayerTransport.ignoresBlocks(player)
            && !(player instanceof ServerPlayer serverPlayer
                && dev.szx.dimensionworks.transfer.tutorial.TutorialManager.locked(serverPlayer));
    }

    public static void input(ServerPlayer player, boolean down, float pitch, float yaw) {
        if (!isElastic(player) || !Float.isFinite(pitch) || !Float.isFinite(yaw)) {
            resetCharge(player);
            return;
        }
        ElasticCharge charge = CHARGES.computeIfAbsent(player.getUUID(), id -> new ElasticCharge());
        int released = charge.input(down);
        if (released > 0 && canCharge(player)) {
            Vec3 direction = Vec3.directionFromRotation(net.minecraft.util.Mth.clamp(pitch, -90, 90), yaw);
            player.setDeltaMovement(direction.scale(ElasticCharge.speed(released)));
            player.setOnGround(false);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    public static void tick(ServerPlayer player) {
        if (!isElastic(player) || !player.isAlive()) {
            resetCharge(player);
            return;
        }
        ElasticCharge charge = CHARGES.get(player.getUUID());
        if (charge != null) charge.tick(canCharge(player));
    }

    public static void resetCharge(Player player) { CHARGES.remove(player.getUUID()); }
    public static void stop() { CHARGES.clear(); }
}
