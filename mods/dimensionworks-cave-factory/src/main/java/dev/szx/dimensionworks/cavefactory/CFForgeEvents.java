package dev.szx.dimensionworks.cavefactory;

import dev.szx.dimensionworks.cavefactory.api.AmpouleExpiry;
import dev.szx.dimensionworks.cavefactory.compat.create.CreateModuleHost;
import dev.szx.dimensionworks.cavefactory.compat.create.CreateModuleHostProvider;
import dev.szx.dimensionworks.cavefactory.item.ModuleItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import dev.szx.dimensionworks.cavefactory.logic.ModuleHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CFForgeEvents {
    private CFForgeEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(CFForgeEvents.class);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ModuleHost.class);
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Reserved for later compatibility hooks that must run after registries are frozen.
        });
    }

    @SubscribeEvent
    public static void claimModuleHost(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (blockEntity == null) {
            return;
        }
        blockEntity.getCapability(ModuleHost.CAPABILITY).ifPresent(host -> {
            if (host instanceof CreateModuleHost createHost) {
                createHost.claimOwner(player);
            }
        });
    }

    @SubscribeEvent
    public static void interactWithModuleHost(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getEntity().isSpectator()) {
            return;
        }
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity == null) {
            return;
        }
        ModuleHost host = blockEntity.getCapability(ModuleHost.CAPABILITY).orElse(null);
        if (!(host instanceof CreateModuleHost createHost)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ModuleItem) {
            if (!createHost.isOwner(player)) {
                player.displayClientMessage(Component.translatable(
                    "message.dimensionworks_cave_factory.not_owner"
                ), true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            if (!createHost.install(player, held)) {
                player.displayClientMessage(Component.translatable(
                    "message.dimensionworks_cave_factory.module_incompatible"
                ), true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (held.isEmpty() && player.isShiftKeyDown()) {
            if (!createHost.isOwner(player)) {
                player.displayClientMessage(Component.translatable(
                    "message.dimensionworks_cave_factory.not_owner"
                ), true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            if (createHost.ejectModules()) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public static void ejectModulesOnBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (blockEntity != null) {
            blockEntity.getCapability(ModuleHost.CAPABILITY).ifPresent(ModuleHost::ejectModules);
        }
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity blockEntity = event.getObject();
        var capability = CreateModuleHostProvider.capabilityFor(blockEntity);
        if (capability != dev.szx.dimensionworks.cavefactory.logic.MachineCapability.NONE) {
            event.addCapability(
                DimensionWorksCaveFactory.id("create_module_host"),
                new CreateModuleHostProvider(blockEntity, capability)
            );
        }
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        long now = System.currentTimeMillis();
        long window = CaveFactoryConfig.STABILITY_SECONDS.get() * 1_000L;
        String dimension = event.getTo().location().toString();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack settled = settleStack(player, player.getInventory().items.get(slot), dimension, now, window);
            player.getInventory().items.set(slot, settled);
        }
        for (int slot = 0; slot < player.getInventory().offhand.size(); slot++) {
            ItemStack settled = settleStack(player, player.getInventory().offhand.get(slot), dimension, now, window);
            player.getInventory().offhand.set(slot, settled);
        }
        ItemStack carried = settleStack(player, player.containerMenu.getCarried(), dimension, now, window);
        player.containerMenu.setCarried(carried);
    }

    private static ItemStack settleStack(Player player, ItemStack stack, String dimension, long now, long window) {
        if (stack.isEmpty()) {
            return stack;
        }
        AmpouleExpiry.observe(stack, dimension, now, window);
        AmpouleExpiry.Settlement settlement = AmpouleExpiry.settle(stack, now);
        if (!settlement.expired()) {
            return stack;
        }
        int expiredCount = stack.getCount();
        if (!settlement.residue().isEmpty()) {
            ItemStack residue = settlement.residue().copyWithCount(expiredCount);
            if (!player.getInventory().add(residue)) {
                player.drop(residue, false);
            }
        }
        return settlement.stack().copyWithCount(expiredCount);
    }
}
