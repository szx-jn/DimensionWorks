package dev.szx.dimensionworks.createculling;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.simibubi.create.AllBlocks;
import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.backend.BackendManager;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Automated real graphical-client checks. Included only by the probe run configuration. */
@Mod.EventBusSubscriber(modid = DimensionWorksCreateCulling.MOD_ID, value = Dist.CLIENT)
public final class ClientRenderProbe {
    private static final Path RESULT = Path.of("probe-result.txt");
    private static final List<String> checks = new ArrayList<>();
    private static final BlockPos[] TARGETS = {
        new BlockPos(-10,100,0), new BlockPos(-6,100,0), new BlockPos(0,100,-6),
        new BlockPos(10,100,-6), new BlockPos(-10,100,-12), new BlockPos(-6,100,6),
        new BlockPos(0,100,6), new BlockPos(10,100,6), new BlockPos(-12,100,6)
    };
    private static boolean launched, setup, finished;
    private static int ticks, stage, stageTicks;
    private static volatile boolean ready;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("dimensionworks.culling.probe") || finished || event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (!launched && mc.screen instanceof TitleScreen) {
                launched = true;
                mc.options.renderDistance().set(5);
                mc.options.simulationDistance().set(5);
                mc.options.framerateLimit().set(60);
                mc.options.pauseOnLostFocus = false;
                mc.options.hideGui = true;
                LevelSettings settings = new LevelSettings("DW Culling Probe", GameType.CREATIVE, false,
                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                Function<RegistryAccess, WorldDimensions> dimensions = access -> access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                    .getOrThrow(WorldPresets.FLAT).createWorldDimensions();
                Object flows = mc.createWorldOpenFlows();
                Method create = java.util.Arrays.stream(flows.getClass().getMethods())
                    .filter(m -> m.getName().equals("createFreshLevel")).findFirst().orElseThrow();
                Object[] args = create.getParameterCount() == 4
                    ? new Object[]{"dw-culling-probe", settings, new WorldOptions(7,false,false), dimensions}
                    : new Object[]{"dw-culling-probe", settings, new WorldOptions(7,false,false), dimensions, mc.screen};
                create.invoke(flows, args);
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            ticks++;
            if (!setup && ticks > 80) {
                setup = true;
                mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    for (BlockPos p : BlockPos.betweenClosed(-18,96,-17,18,107,12)) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(TARGETS[0], AllBlocks.ANDESITE_ENCASED_SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.Y));
                    level.setBlockAndUpdate(TARGETS[0].above(), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(TARGETS[0].below(), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(TARGETS[1], AllBlocks.SHAFT.getDefaultState());
                    for (var direction : net.minecraft.core.Direction.values())
                        level.setBlockAndUpdate(TARGETS[1].relative(direction), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(TARGETS[2], AllBlocks.WATER_WHEEL.getDefaultState());
                    level.setBlockAndUpdate(TARGETS[3], AllBlocks.LARGE_WATER_WHEEL.getDefaultState());
                    level.setBlockAndUpdate(TARGETS[4], AllBlocks.GEARBOX.getDefaultState());
                    level.setBlockAndUpdate(TARGETS[5], AllBlocks.SHAFT.getDefaultState());
                    level.setBlockAndUpdate(TARGETS[6], AllBlocks.WATER_WHEEL.getDefaultState());
                    level.setBlockAndUpdate(TARGETS[7], AllBlocks.LARGE_WATER_WHEEL.getDefaultState());
                    level.setBlockAndUpdate(TARGETS[8], AllBlocks.ENCASED_FAN.getDefaultState());
                    wall(-4,4,-2,false); wall(6,14,-2,false); wall(-14,-6,-8,false);
                    wall(-3,3,10,true);
                    level.setBlockAndUpdate(new BlockPos(10,100,10), Blocks.STONE.defaultBlockState());
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    player.getAbilities().flying = true; player.onUpdateAbilities();
                    player.connection.teleport(.5,100,22,180,0);
                    ready = true;
                });
            }
            if (ready) stageTicks++;
            if (ticks > 2400) throw new IllegalStateException("Client probe timed out");
        } catch (Throwable error) { fail(error); }
    }

    private static void wall(int minX,int maxX,int z,boolean glass) {
        var level = Minecraft.getInstance().getSingleplayerServer().overworld();
        for (int x=minX;x<=maxX;x++) for(int y=96;y<=107;y++)
            level.setBlockAndUpdate(new BlockPos(x,y,z), (glass?Blocks.GLASS:Blocks.STONE).defaultBlockState());
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!ready || finished || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || stageTicks < 100) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            String backend = Backend.REGISTRY.getIdOrThrow(BackendManager.currentBackend()).toString();
            String requested = System.getProperty("dimensionworks.culling.backend");
            check("actual backend " + requested, backend.equals("flywheel:" + requested));
            var frame = stage == 1 ? new ClientOcclusion.Frame(mc.level, mc.gameRenderer.getMainCamera().getPosition()) : ClientOcclusion.frame();
            check("world and camera ready", frame != null && Math.abs(mc.player.getZ()-(stage==4?-18:22)) < 1);
            for (int i=0;i<TARGETS.length;i++) {
                if (stage == 4 && i != 2) continue;
                var be = mc.level.getBlockEntity(TARGETS[i]);
                check("target loaded " + i, be != null);
                boolean hidden = stage != 1 && stage != 3 && i < 5;
                if (stage == 3) hidden = i == 1; // Only the sealed bare shaft remains enclosed.
                if (stage == 4) hidden = false; // The wall is now behind the wheel relative to the camera.
                if (stage == 5) hidden = i == 1 || i == 2;
                if (stage != 1) {
                    boolean actual = frame.hidden(be);
                    if (actual != hidden) {
                        checks.add("DETAIL state="+be.getBlockState()+" bounds="+be.getRenderBoundingBox()+" camera="+mc.gameRenderer.getMainCamera().getPosition()+" reads="+frame.reads()+" supported="+ClientOcclusion.supported(be));
                        for (var d : net.minecraft.core.Direction.values()) {
                            var p = be.getBlockPos().relative(d);
                            var state = mc.level.getBlockState(p);
                            checks.add("DETAIL neighbor="+d+" state="+state+" solid="+state.isSolidRender(mc.level,p));
                        }
                    }
                    check("occlusion stage="+stage+" target="+i, actual == hidden);
                }
                if (BackendManager.isBackendOn()) {
                    var manager = VisualizationManagerImpl.get(mc.level);
                    var storage = (dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage)
                        ((dev.engine_room.flywheel.impl.visualization.VisualManagerImpl<?, ?>) manager.blockEntities()).getStorage();
                    var visual = storage.visualAtPos(TARGETS[i].asLong());
                    check("Flywheel visual exists " + i, visual != null);
                    int[] count = {0};
                    final boolean expectedVisible = !hidden;
                    visual.collectCrumblingInstances(instance -> {
                        if (instance == null) return;
                        count[0]++;
                        check("GPU instance visibility stage="+stage+" target="+be.getBlockPos(),
                            instance.handle().isVisible() == expectedVisible);
                    });
                    check("instances inspected " + i, count[0] > 0);
                }
            }
            if (!BackendManager.isBackendOn() && stage == 0)
                check("vanilla dispatcher actually skipped hidden renders", ClientOcclusion.skippedRenders > 0);
            check("bounded block reads", frame.reads() <= 8192);
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(Path.of("probe-stage-" + stage + ".png").toFile());
            }
            stageTicks = 0;
            if (stage == 0) {
                System.setProperty("dimensionworks.culling.disabled", "true");
            } else if (stage == 1) {
                System.clearProperty("dimensionworks.culling.disabled");
                mc.levelRenderer.allChanged();
            } else if (stage == 2) {
                mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    level.setBlockAndUpdate(TARGETS[0].above(), Blocks.AIR.defaultBlockState());
                    for (int x=-15;x<=15;x++) for(int y=96;y<=107;y++) {
                        level.setBlockAndUpdate(new BlockPos(x,y,-2), Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(new BlockPos(x,y,-8), Blocks.AIR.defaultBlockState());
                    }
                });
            } else if (stage == 3) {
                mc.getSingleplayerServer().execute(() -> {
                    wall(-4,4,-2,false);
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    player.connection.teleport(.5,100,-18,0,0);
                });
            } else if (stage == 4) {
                mc.getSingleplayerServer().execute(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var wheel = (com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity) level.getBlockEntity(TARGETS[2]);
                    wheel.material = Blocks.SPRUCE_PLANKS.defaultBlockState();
                    wheel.sendData();
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    player.connection.teleport(.5,100,22,180,0);
                });
            } else {
                checks.add("PASS COMPLETE backend=" + backend + " checks=" + checks.size());
                Files.write(RESULT, checks);
                finished = true;
                mc.stop();
            }
            stage++;
        } catch (Throwable error) { fail(error); }
    }

    private static void check(String name,boolean ok) {
        if (!ok) throw new IllegalStateException(name);
        checks.add("PASS " + name);
    }
    private static void fail(Throwable error) {
        finished = true;
        checks.add("FAIL " + error);
        error.printStackTrace();
        try { Files.write(RESULT, checks); } catch (Exception ignored) { }
        Minecraft.getInstance().stop();
    }
}
