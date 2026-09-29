package dev.szx.dimensionworks.mekstress.memory;

import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import dev.szx.dimensionworks.mekstress.DimensionWorksMekStress;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.IItemHandler;

/** Bounded removal queue for retired Applied Create stress storage items. */
public final class MemoryLegacyMigration {
    private static final Set<String> RETIRED_ITEMS = Set.of(
        "stress_storage_cell_1k", "stress_storage_cell_4k", "stress_storage_cell_16k",
        "stress_storage_cell_64k", "stress_storage_cell_256k", "stress_storage_cell_1m",
        "stress_storage_cell_4m", "stress_storage_cell_16m", "stress_storage_cell_64m",
        "stress_storage_cell_256m", "stress_storage_component_1k", "stress_storage_component_4k",
        "stress_storage_component_16k", "stress_storage_component_64k",
        "stress_storage_component_256k", "stress_storage_component_1m",
        "stress_storage_component_4m", "stress_storage_component_16m",
        "stress_storage_component_64m", "stress_storage_component_256m",
        "andesite_stress_cell_housing", "brass_stress_cell_housing", "creative_stress_cell"
    );
    private static final ResourceLocation[] SEGMENTED_INVENTORIES = {
        ISegmentedInventory.CELLS,
        ISegmentedInventory.STORAGE,
        ISegmentedInventory.CONFIG,
        ISegmentedInventory.UPGRADES
    };
    private static final int INTERVAL_TICKS = 20;
    private static final int MAX_QUEUED_INVENTORIES = 8_192;
    private static final int WORK_PER_INTERVAL = 256;
    private static final Object QUEUE_LOCK = new Object();
    private static final ArrayDeque<GlobalPos> BLOCK_QUEUE = new ArrayDeque<>();
    private static final Set<GlobalPos> QUEUED_BLOCKS = new HashSet<>();
    private static final ArrayDeque<EntityRef> ENTITY_QUEUE = new ArrayDeque<>();
    private static final Set<EntityRef> QUEUED_ENTITIES = new HashSet<>();

    private MemoryLegacyMigration() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            clearPlayer(player);
        }
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (hasItemStorage(blockEntity)) {
                enqueue(GlobalPos.of(level.dimension(), blockEntity.getBlockPos().immutable()));
            }
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !hasItemStorage(event.getEntity())) {
            return;
        }
        enqueue(new EntityRef(event.getLevel().dimension(), event.getEntity().getUUID()));
    }

    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity().getUUID() != null) {
            synchronized (QUEUE_LOCK) {
                EntityRef ref = new EntityRef(event.getLevel().dimension(), event.getEntity().getUUID());
                if (QUEUED_ENTITIES.remove(ref)) {
                    ENTITY_QUEUE.remove(ref);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
            || event.getServer().getTickCount() % INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            clearPlayer(player);
        }
        processQueues(event.getServer(), WORK_PER_INTERVAL);
    }

    public static boolean isRetired(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "appliedcreate".equals(id.getNamespace()) && RETIRED_ITEMS.contains(id.getPath());
    }

    private static void processQueues(MinecraftServer server, int budget) {
        int remaining = budget;
        while (remaining > 0) {
            GlobalPos pos = pollBlock();
            if (pos == null) {
                break;
            }
            ServerLevel level = server.getLevel(pos.dimension());
            if (level != null && level.getChunkSource().getChunkNow(pos.pos().getX() >> 4, pos.pos().getZ() >> 4) != null) {
                BlockEntity blockEntity = level.getBlockEntity(pos.pos());
                if (blockEntity != null) {
                    clearBlockEntity(blockEntity);
                }
            }
            remaining--;
        }

        while (remaining > 0) {
            EntityRef ref = pollEntity();
            if (ref == null) {
                break;
            }
            ServerLevel level = server.getLevel(ref.dimension());
            if (level != null) {
                Entity entity = level.getEntity(ref.id());
                if (entity != null) {
                    clearEntity(entity);
                }
            }
            remaining--;
        }
    }

    private static void clearPlayer(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            clearStack(stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            clearStack(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            clearStack(stack);
        }
        clearProviders(player);
        player.containerMenu.broadcastChanges();
    }

    private static void clearEntity(Entity entity) {
        if (entity instanceof ItemEntity item) {
            if (isRetired(item.getItem())) {
                item.discard();
            }
            return;
        }
        if (entity instanceof Container container) {
            clearContainer(container);
        }
        clearProviders(entity);
    }

    private static void clearBlockEntity(BlockEntity blockEntity) {
        if (blockEntity instanceof Container container) {
            clearContainer(container);
        }
        if (blockEntity instanceof ISegmentedInventory segmented) {
            for (ResourceLocation segment : SEGMENTED_INVENTORIES) {
                clearInternalInventory(segmented.getSubInventory(segment));
            }
        }
        clearProviders(blockEntity);
    }

    private static void clearContainer(Container container) {
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (isRetired(container.getItem(slot))) {
                container.setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            container.setChanged();
        }
    }

    private static void clearInternalInventory(InternalInventory inventory) {
        if (inventory == null || inventory.isEmpty()) {
            return;
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (isRetired(inventory.getStackInSlot(slot))) {
                inventory.setItemDirect(slot, ItemStack.EMPTY);
                inventory.sendChangeNotification(slot);
            }
        }
    }

    private static void clearProviders(ICapabilityProvider provider) {
        Set<IItemHandler> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Direction side : Direction.values()) {
            provider.getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                .ifPresent(handler -> {
                    if (visited.add(handler)) {
                        clearItemHandler(handler);
                    }
                });
        }
        provider.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
            .ifPresent(handler -> {
                if (visited.add(handler)) {
                    clearItemHandler(handler);
                }
            });
    }

    private static void clearItemHandler(IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            for (int attempt = 0; attempt < 4; attempt++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!isRetired(stack)) {
                    break;
                }
                ItemStack extracted = handler.extractItem(slot, stack.getCount(), false);
                if (extracted.isEmpty()) {
                    break;
                }
            }
        }
    }

    private static boolean hasItemStorage(Object candidate) {
        if (candidate instanceof Container || candidate instanceof ISegmentedInventory) {
            return true;
        }
        return candidate instanceof ICapabilityProvider provider
            && provider.getCapability(ForgeCapabilities.ITEM_HANDLER, null).isPresent();
    }

    private static void enqueue(GlobalPos pos) {
        synchronized (QUEUE_LOCK) {
            if (BLOCK_QUEUE.size() + ENTITY_QUEUE.size() >= MAX_QUEUED_INVENTORIES || !QUEUED_BLOCKS.add(pos)) {
                return;
            }
            BLOCK_QUEUE.addLast(pos);
        }
    }

    private static void enqueue(EntityRef ref) {
        synchronized (QUEUE_LOCK) {
            if (BLOCK_QUEUE.size() + ENTITY_QUEUE.size() >= MAX_QUEUED_INVENTORIES || !QUEUED_ENTITIES.add(ref)) {
                return;
            }
            ENTITY_QUEUE.addLast(ref);
        }
    }

    private static GlobalPos pollBlock() {
        synchronized (QUEUE_LOCK) {
            GlobalPos pos = BLOCK_QUEUE.pollFirst();
            if (pos != null) {
                QUEUED_BLOCKS.remove(pos);
            }
            return pos;
        }
    }

    private static EntityRef pollEntity() {
        synchronized (QUEUE_LOCK) {
            EntityRef ref = ENTITY_QUEUE.pollFirst();
            if (ref != null) {
                QUEUED_ENTITIES.remove(ref);
            }
            return ref;
        }
    }

    private static void clearStack(ItemStack stack) {
        if (isRetired(stack)) {
            stack.setCount(0);
        }
    }

    private record EntityRef(ResourceKey<Level> dimension, UUID id) {
    }
}
