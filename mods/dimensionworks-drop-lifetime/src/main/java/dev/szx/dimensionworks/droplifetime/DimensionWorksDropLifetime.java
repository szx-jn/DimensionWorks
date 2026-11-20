package dev.szx.dimensionworks.droplifetime;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;


@Mod(DimensionWorksDropLifetime.MOD_ID)
public final class DimensionWorksDropLifetime {
    public static final String MOD_ID = "dimensionworks_drop_lifetime";

    public DimensionWorksDropLifetime() {
    }

    @EventBusSubscriber(modid = MOD_ID)
    public static final class ItemLifetimeHandler {
        private static final int BASE_LIFETIME_SECONDS = 25;
        private static final int MAX_LIFETIME_SECONDS = 180;
        private static final int CLAIMED_CHUNK_LIFETIME_SECONDS = 5 * 60;
        private static final int TICKS_PER_SECOND = 20;

        private ItemLifetimeHandler() {
        }

        @SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity itemEntity)) {
                return;
            }

            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                return;
            }

            Level level = itemEntity.level();
            if (isClaimedByPlayer(level, itemEntity.blockPosition())) {
                itemEntity.lifespan = CLAIMED_CHUNK_LIFETIME_SECONDS * TICKS_PER_SECOND;
                return;
            }

            Rarity rarity = stack.getRarity();
            int lifetimeSeconds = switch (rarity) {
                case COMMON -> BASE_LIFETIME_SECONDS;
                case UNCOMMON -> 50;
                case RARE -> 75;
                case EPIC -> MAX_LIFETIME_SECONDS;
                // NeoForge 允许模组扩展稀有度。
                default -> BASE_LIFETIME_SECONDS;
            };
            itemEntity.lifespan = lifetimeSeconds * TICKS_PER_SECOND;
        }

        private static boolean isClaimedByPlayer(Level level, BlockPos pos) {
            var api = FTBChunksAPI.api();
            return api.isManagerLoaded()
                    && api.getManager().getChunk(new ChunkDimPos(level, pos)) != null;
        }
    }
}
