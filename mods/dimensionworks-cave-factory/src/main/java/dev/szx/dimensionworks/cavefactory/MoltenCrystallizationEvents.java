package dev.szx.dimensionworks.cavefactory;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Map;

/** Tracks molten-metal fluid placements and turns them into crystallized fluid after 30 exposed seconds. */
public final class MoltenCrystallizationEvents {
    private static final long EXPOSURE_TICKS = 30L * 20L;
    private static final long CHECK_INTERVAL_TICKS = 20L;

    private MoltenCrystallizationEvents() {}

    /** Called after the liquid block is installed, including buckets, pipes and flowing fluid. */
    public static void onPlaced(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel && isMoltenMetal(state)) {
            MoltenCrystallizationData.get(serverLevel).reset(pos);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
            || level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        MoltenCrystallizationData data = MoltenCrystallizationData.get(level);
        Map<Long, Long> tracked = data.positions();
        if (tracked.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        ArrayList<Long> remove = new ArrayList<>();
        tracked.forEach((packedPos, startedAt) -> {
            BlockPos pos = BlockPos.of(packedPos);
            if (!level.hasChunkAt(pos)) {
                return;
            }
            if (!isMoltenMetal(level.getBlockState(pos))) {
                remove.add(packedPos);
                return;
            }

            if (!level.canSeeSky(pos.above())) {
                if (startedAt >= 0L) {
                    tracked.put(packedPos, -1L);
                    data.setDirty();
                }
                return;
            }

            if (startedAt < 0L) {
                tracked.put(packedPos, now);
                data.setDirty();
            } else if (now - startedAt >= EXPOSURE_TICKS) {
                String fluidId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
                var crystal = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(
                    DimensionWorksCaveFactory.MOD_ID, "crystallized_" + fluidId));
                if (crystal instanceof net.minecraft.world.level.material.FlowingFluid flowing) {
                    var original = level.getFluidState(pos);
                    var result = original.isSource() ? flowing.getSource(false)
                        : flowing.getFlowing(original.getAmount(), original.getValue(
                            net.minecraft.world.level.material.FlowingFluid.FALLING));
                    level.setBlock(pos, result.createLegacyBlock(), 3);
                }
                remove.add(packedPos);
            }
        });

        if (!remove.isEmpty()) {
            remove.forEach(tracked::remove);
            data.setDirty();
        }
    }

    private static boolean isMoltenMetal(BlockState state) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("dimensionworks")
            && id.getPath().startsWith("molten_");
    }
}
