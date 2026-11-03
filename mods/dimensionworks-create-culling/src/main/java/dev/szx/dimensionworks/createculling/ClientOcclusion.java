package dev.szx.dimensionworks.createculling;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visual.Visual;
import dev.engine_room.flywheel.impl.visualization.storage.Storage;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.szx.dimensionworks.createculling.mixin.VisualAccess;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DimensionWorksCreateCulling.MOD_ID, value = Dist.CLIENT)
public final class ClientOcclusion {
    private static final BooleanSupplier SHADERS = shaderGuard();
    private static Frame vanilla;
    private static boolean renderPass;
    public static long skippedRenders;
    private ClientOcclusion() {}

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        vanilla = null;
        renderPass = event.phase == TickEvent.Phase.START;
    }

    public static boolean skip(BlockEntity be) {
        if (renderPass && vanilla == null) vanilla = frame();
        boolean hidden = vanilla != null && vanilla.hidden(be);
        if (hidden) skippedRenders++;
        return hidden;
    }

    /** Runs after all visual updates and before Flywheel's engine upload, in the same plan. */
    public static void updateInstances(Storage<?> storage) {
        Frame frame = frame();
        for (Visual visual : storage.getAllVisuals()) {
            if (!(visual instanceof AbstractBlockEntityVisual<?>)) continue;
            BlockEntity be = ((VisualAccess) visual).dimensionworks$getBlockEntity();
            if (!supported(be)) continue;
            boolean visible = frame == null || !frame.hidden(be);
            ((BlockEntityVisual<?>) visual).collectCrumblingInstances(instance -> {
                if (instance != null) instance.setVisible(visible);
            });
        }
    }

    public static Frame frame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || Boolean.getBoolean("dimensionworks.culling.disabled") || SHADERS.getAsBoolean()) return null;
        return new Frame(mc.level, mc.gameRenderer.getMainCamera().getPosition());
    }

    public static boolean supported(BlockEntity be) {
        var type = be.getType();
        return type == AllBlockEntityTypes.BRACKETED_KINETIC.get() || type == AllBlockEntityTypes.ENCASED_SHAFT.get()
            || type == AllBlockEntityTypes.ENCASED_COGWHEEL.get() || type == AllBlockEntityTypes.ENCASED_LARGE_COGWHEEL.get()
            || type == AllBlockEntityTypes.GEARBOX.get() || type == AllBlockEntityTypes.MOTOR.get()
            || type == AllBlockEntityTypes.ENCASED_FAN.get() || type == AllBlockEntityTypes.MECHANICAL_PUMP.get()
            || type == AllBlockEntityTypes.WATER_WHEEL.get() || type == AllBlockEntityTypes.LARGE_WATER_WHEEL.get();
    }

    public static final class Frame {
        private final Level level;
        private final Vec3 camera;
        private final Long2BooleanOpenHashMap solids = new Long2BooleanOpenHashMap();
        private final Map<BlockEntity, Boolean> decisions = new IdentityHashMap<>();
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        private int reads;
        private int candidates;
        public Frame(Level level, Vec3 camera) { this.level = level; this.camera = camera; }
        public int reads() { return reads; }

        public boolean hidden(BlockEntity be) {
            if (be.getLevel() != level || be.isRemoved() || !supported(be)) return false;
            Boolean cached = decisions.get(be);
            if (cached != null) return cached;
            if (++candidates > 4096 || reads >= 8192) return false;
            double distance = camera.distanceToSqr(Vec3.atCenterOf(be.getBlockPos()));
            if (distance < 4 || distance > 256 * 256 || solid(BlockPos.containing(camera))) return false;
            boolean wheel = be.getType() == AllBlockEntityTypes.WATER_WHEEL.get()
                || be.getType() == AllBlockEntityTypes.LARGE_WATER_WHEEL.get();
            // Forge defaults to a collision box, which is not a rotating model bound.
            AABB bounds = wheel ? be.getRenderBoundingBox() : new AABB(be.getBlockPos());
            // Rotating teeth and fan tips can protrude beyond the default unit BE box.
            if (AllBlocks.COGWHEEL.has(be.getBlockState()) || AllBlocks.LARGE_COGWHEEL.has(be.getBlockState())
                || be.getType() == AllBlockEntityTypes.ENCASED_COGWHEEL.get()
                || be.getType() == AllBlockEntityTypes.ENCASED_FAN.get()
                || be.getType() == AllBlockEntityTypes.ENCASED_LARGE_COGWHEEL.get()
                || be.getType() == AllBlockEntityTypes.MECHANICAL_PUMP.get()) bounds = bounds.inflate(0.5);
            OcclusionProof.Box box = new OcclusionProof.Box(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
            boolean hidden = false;
            if (!box.contains(camera.x, camera.y, camera.z)) {
                var state = be.getBlockState();
                // These two casings cover the four lateral faces. The girder does not.
                if (AllBlocks.ANDESITE_ENCASED_SHAFT.has(state) || AllBlocks.BRASS_ENCASED_SHAFT.has(state)) {
                    Direction end = Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(BlockStateProperties.AXIS));
                    hidden = solid(be.getBlockPos().relative(end)) && solid(be.getBlockPos().relative(end.getOpposite()));
                }
                if (!hidden) hidden = OcclusionProof.hidden(box, camera.x, camera.y, camera.z, this::solid);
            }
            // Exhaustion is unknown, and must never leave an old hidden decision behind.
            hidden &= reads < 8192;
            decisions.put(be, hidden);
            return hidden;
        }

        private boolean solid(BlockPos pos) { return solid(pos.getX(), pos.getY(), pos.getZ()); }
        private boolean solid(int x, int y, int z) {
            long key = BlockPos.asLong(x, y, z);
            if (solids.containsKey(key)) return solids.get(key);
            if (reads >= 8192) return false;
            reads++;
            cursor.set(x, y, z);
            // Never request/load a chunk to make a render decision.
            boolean opaque = level.hasChunkAt(cursor) && level.getBlockState(cursor).isSolidRender(level, cursor);
            solids.put(key, opaque);
            return opaque;
        }
    }

    private static BooleanSupplier shaderGuard() {
        if (!ModList.get().isLoaded("oculus") && !ModList.get().isLoaded("iris")) return () -> false;
        for (String name : new String[]{"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"}) {
            try {
                Class<?> api = Class.forName(name);
                Object instance = api.getMethod("getInstance").invoke(null);
                Method enabled = api.getMethod("isShaderPackInUse");
                return () -> {
                    try { return (boolean) enabled.invoke(instance); }
                    catch (ReflectiveOperationException e) { return true; }
                };
            } catch (ReflectiveOperationException ignored) { }
        }
        return () -> true;
    }
}
