package dev.szx.dimensionworks.rpmlimit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.szx.dimensionworks.rpmlimit.optimizer.KineticNetworkOptimizer;
import dev.szx.dimensionworks.rpmlimit.quota.CreateCapacityRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class RpmForgeEvents {
    private static final int REQUIRED_CREATE_ROTATION_CAP = 10240;

    @SubscribeEvent
    public static void starting(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
        RpmLimitManager.beginServer(event.getServer());
    }
    @SubscribeEvent
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        KineticNetworkOptimizer.flushPendingPublications();
    }

    @SubscribeEvent
    public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        RpmLimitManager.endServer();
    }
    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        GearHeartMilestones.copy(event.getOriginal().getPersistentData(), event.getEntity().getPersistentData());
        RpmPlayerState.copy(event.getOriginal().getPersistentData(), event.getEntity().getPersistentData());
        RpmLimitManager.init(event.getEntity());
    }
    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RpmLimitManager.init(player);
            GearHeartEffects.tickPlayer(player);
            RpmLimitManager.applyLimitToOwned(player, RpmLimitManager.getEffectiveLimit(player.getUUID()));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        RpmLimitManager.init(e.getEntity());
        if (e.getEntity() instanceof ServerPlayer player)
            RpmLimitManager.applyLimitToOwned(player, RpmLimitManager.getEffectiveLimit(player.getUUID()));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) {
            RpmLimitManager.rememberLogout(player);
            GearHeartEffects.clearPlayer(player);
        }
    }

    public static void config(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if ("create".equals(config.getModId()) && config.getType() == ModConfig.Type.SERVER)
            enforceCreateRotationCap();
    }

    private static void enforceCreateRotationCap() {
        var createCap = AllConfigs.server().kinetics.maxRotationSpeed;
        if (createCap.get() >= REQUIRED_CREATE_ROTATION_CAP) return;
        int previous = createCap.get();
        createCap.set(REQUIRED_CREATE_ROTATION_CAP);
        DimensionWorksRpmLimit.LOGGER.info("Raised Create maxRotationSpeed from {} to {}.", previous, REQUIRED_CREATE_ROTATION_CAP);
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        enforceCreateRotationCap();
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p)
            RpmLimitManager.refresh(p.server.getPlayerList().getPlayers(), p.server.getTickCount());
        if (e.getEntity() instanceof ServerPlayer p)
            GearHeartEffects.tickPlayer(p);
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post e) {
        {
            CreateCapacityRegistry.clearRemoved();
            KineticNetworkOptimizer.abortBatch();
            KineticNetworkOptimizer.flushPendingPublications();
        }
    }

    @SubscribeEvent
    public static void place(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player) || !(e.getLevel() instanceof ServerLevel level))
            return;

        var pos = e.getPos().immutable();
        level.getServer().execute(() -> {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof KineticBlockEntity kinetic)
                RpmLimitManager.setOwner(kinetic, player.getUUID());
        });
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("dw")
            .requires(s -> s.hasPermission(2))
            .then(Commands.literal("rpm")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1, 10240))
                        .executes(c -> {
                            ServerPlayer target = EntityArgument.getPlayer(c, "player");
                            int limit = IntegerArgumentType.getInteger(c, "limit");
                            RpmLimitManager.setLimit(target, limit);
                            c.getSource().sendSuccess(
                                () -> Component.translatable("commands.dimensionworks_rpm_limit.limit",
                                    target.getGameProfile().getName(), RpmLimitManager.getLimit(target.getUUID()),
                                    RpmLimitManager.getEffectiveLimit(target.getUUID()))
                                    .append(RpmLimitManager.getEffectiveLimit(target.getUUID()) < limit
                                        ? Component.translatable("commands.dimensionworks_rpm_limit.external_limit")
                                        : Component.empty()),
                                true
                            );
                            return 1;
                        })))));
    }
}
