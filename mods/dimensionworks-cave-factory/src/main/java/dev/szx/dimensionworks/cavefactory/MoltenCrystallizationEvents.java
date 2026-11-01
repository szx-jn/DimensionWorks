package dev.szx.dimensionworks.cavefactory;

import dev.szx.dimensionworks.cavefactory.registry.CFBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Map;

/** Tracks molten-metal fluid placements and turns them into crystal after 30 exposed seconds. */
public final class MoltenCrystallizationEvents {
    private static final long EXPOSURE_TICKS = 30L * 20L;
    private static final long CHECK_INTERVAL_TICKS = 20L;

    private MoltenCrystallizationEvents() {}

    @SubscribeEvent
    public static void onFluidFlow(BlockEvent.FluidPlaceBlockEvent event) {
        track(event.getLevel(), event.getPos(), event.getNewState());
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isMoltenMetal(event.getPlacedBlock())) {
            MoltenCrystallizationData.get(level).reset(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBucketUse(PlayerInteractEvent.RightClickBlock event) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (itemId == null || !itemId.getNamespace().equals("dimensionworks")
            || !itemId.getPath().startsWith("molten_") || !itemId.getPath().endsWith("_bucket")
            || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos target = event.getPos().relative(event.getFace());
        MoltenCrystallizationData.get(level).reset(target);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
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
                String fluidId = ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()).getPath();
                var crystal = CFBlocks.CRYSTALLIZED_MOLTEN_METALS.get(fluidId);
                if (crystal != null) {
                    level.setBlock(pos, crystal.get().defaultBlockState(), 3);
                }
                remove.add(packedPos);
            }
        });

        if (!remove.isEmpty()) {
            remove.forEach(tracked::remove);
            data.setDirty();
        }
    }

    private static void track(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel && isMoltenMetal(state)) {
            MoltenCrystallizationData.get(serverLevel).track(pos);
        }
    }

    private static boolean isMoltenMetal(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null && id.getNamespace().equals("dimensionworks")
            && id.getPath().startsWith("molten_");
    }
}
