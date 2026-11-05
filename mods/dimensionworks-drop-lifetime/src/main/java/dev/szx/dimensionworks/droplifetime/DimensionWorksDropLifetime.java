package dev.szx.dimensionworks.droplifetime;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@Mod(DimensionWorksDropLifetime.MOD_ID)
public final class DimensionWorksDropLifetime {
    public static final String MOD_ID = "dimensionworks_drop_lifetime";

    public DimensionWorksDropLifetime() {
    }

    @Mod.EventBusSubscriber(
            modid = MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
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
                // Forge permits other mods to add rarities at runtime.
                default -> BASE_LIFETIME_SECONDS;
            };
            itemEntity.lifespan = lifetimeSeconds * TICKS_PER_SECOND;
        }

        private static boolean isClaimedByPlayer(Level level, BlockPos pos) {
            if (!ModList.get().isLoaded("ftbchunks")) {
                return false;
            }

            try {
                Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
                Object api = apiClass.getMethod("api").invoke(null);
                Class<?> apiInterface = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
                if (!Boolean.TRUE.equals(apiInterface.getMethod("isManagerLoaded").invoke(api))) {
                    return false;
                }

                Object manager = apiInterface.getMethod("getManager").invoke(api);
                Class<?> chunkDimPosClass = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
                Constructor<?> positionConstructor = chunkDimPosClass.getConstructor(Level.class, BlockPos.class);
                Object chunkPosition = positionConstructor.newInstance(level, pos);
                Class<?> managerInterface = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
                Method getChunk = managerInterface.getMethod("getChunk", chunkDimPosClass);
                return getChunk.invoke(manager, chunkPosition) != null;
            } catch (ReflectiveOperationException | LinkageError exception) {
                return false;
            }
        }
    }
}
