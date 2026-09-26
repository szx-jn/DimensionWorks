package dev.szx.dimensionworks.rpmlimit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class RpmForgeEvents {
    private static final int REQUIRED_CREATE_ROTATION_CAP = 10240;

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        RpmLimitManager.init(e.getEntity());
        if (e.getEntity() instanceof ServerPlayer player)
            RpmLimitManager.applyLimitToOwned(player, RpmLimitManager.get(player.getUUID()));
    }

    public static void config(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if ("create".equals(config.getModId()) && config.getType() == ModConfig.Type.SERVER)
            enforceCreateRotationCap();
    }

    private static void enforceCreateRotationCap() {
        try {
            var createCap = AllConfigs.server().kinetics.maxRotationSpeed;
            if (createCap.get() >= REQUIRED_CREATE_ROTATION_CAP)
                return;

            int previous = createCap.get();
            createCap.set(REQUIRED_CREATE_ROTATION_CAP);
            DimensionWorksRpmLimit.LOGGER.info(
                "Raised Create's maxRotationSpeed from {} to {} to match DimensionWorks.",
                previous, REQUIRED_CREATE_ROTATION_CAP);
        } catch (Throwable error) {
            DimensionWorksRpmLimit.LOGGER.warn(
                "Could not enforce Create's maxRotationSpeed; check the world's create-server.toml.",
                error);
        }
    }

    /**
     * Create clamps every kinetic block to its own {@code kinetics.maxRotationSpeed}, which lives in
     * the per-world {@code create-server.toml}. When that file is missing from the pack, a new world
     * is created empty and every key falls back to Create's default of 256 RPM, which makes this
     * mod's own ceiling unreachable. Reading the value back on server start is the cheapest way to
     * notice that without digging through the world folder.
     */
    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent e) {
        enforceCreateRotationCap();
        int expected = RpmLimitConfig.MAX_RPM.get();
        int createCap = -1;
        try {
            Object raw = AllConfigs.server().kinetics.maxRotationSpeed.get();
            if (raw instanceof Number number)
                createCap = number.intValue();
        } catch (Throwable ignored) {
            return;
        }

        if (createCap > 0 && createCap < expected)
            DimensionWorksRpmLimit.LOGGER.warn(
                "Create caps rotation at {} RPM but this pack is configured for {} RPM. Raise " +
                    "kinetics.maxRotationSpeed in <world>/serverconfig/create-server.toml, or make sure " +
                    "defaultconfigs/create-server.toml ships with the pack so new worlds start correct.",
                createCap, expected);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer p)
            RpmLimitManager.refresh(p.server.getPlayerList().getPlayers(), p.server.getTickCount());
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
                                () -> Component.literal("Set " + target.getGameProfile().getName() + " RPM limit to " + limit + "."),
                                true
                            );
                            return 1;
                        })))));
    }
}
