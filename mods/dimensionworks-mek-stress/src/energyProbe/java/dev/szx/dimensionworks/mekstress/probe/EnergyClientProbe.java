package dev.szx.dimensionworks.mekstress.probe;

import appeng.api.config.PowerUnits;
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import dev.szx.dimensionworks.mekstress.api.IMechanicalEnergyBudget;
import dev.szx.dimensionworks.mekstress.integration.mek.MechanicalEnergy;
import dev.szx.dimensionworks.mekstress.integration.mekgenerators.GeneratorPowerBridge;
import dev.szx.dimensionworks.mekstress.memory.MachinePowerManager;
import dev.szx.dimensionworks.mekstress.memory.StressRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Upgrade;
import mekanism.api.math.FloatingLong;
import mekanism.common.config.MekanismConfig;
import mekanism.common.registries.MekanismGases;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityResistiveHeater;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import mekanism.generators.common.config.MekanismGeneratorsConfig;
import mekanism.generators.common.content.fusion.FusionReactorMultiblockData;
import mekanism.generators.common.content.turbine.TurbineMultiblockData;
import mekanism.generators.common.registries.GeneratorsBuilders;
import mekanism.generators.common.registries.GeneratorsFluids;
import mekanism.generators.common.tile.*;
import mekanism.generators.common.tile.fusion.TileEntityFusionReactorPort;
import mekanism.generators.common.tile.turbine.TileEntityTurbineValve;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Real production Forge client, all pack mods, a disposable flat world and native world ticks. */
@Mod("dimensionworks_energy_probe")
@Mod.EventBusSubscriber(modid = "dimensionworks_energy_probe", value = Dist.CLIENT)
public final class EnergyClientProbe {
    private static final List<String> CHECKS = new ArrayList<>();
    private static final Path RESULT = Path.of("energy-probe-result.txt");
    private static volatile boolean worldReady, finished;
    private static boolean launched;
    private static int tick, finishTicks;
    private static final List<TileEntityGenerator> GENERATORS = new ArrayList<>();
    private static final Map<TileEntityGenerator, List<BlockPos>> OUTPUTS = new IdentityHashMap<>();
    private static TileEntityMekanism machine;
    private static TileEntityResistiveHeater heater;
    private static KineticBlockEntity machineShaft;
    private static KineticEnergyAcceptorBlockEntity acceptor;
    private static TileEntityResistiveHeater aeHeater;
    private static TileEntityMekanism chargepad, teleporter;
    private static ItemStack portable;
    private static TurbineMultiblockData turbine;
    private static FusionReactorMultiblockData fusion;
    private static final BlockPos TURBINE_PORT = new BlockPos(40,121,8);
    private static final BlockPos FUSION_PORT = new BlockPos(42,122,25);
    private static FloatingLong originalRatio;
    private static com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore reactor;
    private static final List<BlockPos> REACTOR_SHAFTS = new ArrayList<>();
    private static final BlockPos REACTOR_POS = new BlockPos(15,120,30);
    private static double shieldBeforeLoss;
    private static int chargeWait;

    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("dimensionworks.energy.probe") || event.phase != TickEvent.Phase.END) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (!launched && mc.screen instanceof TitleScreen) {
                launched = true;
                mc.options.renderDistance().set(4); mc.options.simulationDistance().set(4);
                mc.options.framerateLimit().set(30); mc.options.pauseOnLostFocus = false;
                if (Boolean.getBoolean("dimensionworks.energy.reload")) {
                    mc.createWorldOpenFlows().loadLevel(mc.screen, "dw-energy-probe");
                } else {
                    LevelSettings settings = new LevelSettings("DW Energy Probe", GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    java.util.function.Function<RegistryAccess, WorldDimensions> dimensions = access ->
                        access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions();
                    mc.createWorldOpenFlows().createFreshLevel("dw-energy-probe", settings, new WorldOptions(7,false,false), dimensions);
                }
            }
            worldReady = mc.level != null && mc.player != null && mc.getSingleplayerServer() != null;
            if (finished && ++finishTicks == 30) {
                if (mc.level != null) try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    image.writeToFile(Path.of("energy-probe-client.png").toFile());
                }
                mc.stop();
            }
        } catch (Throwable error) { fail(error); }
    }

    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event) {
        if (!Boolean.getBoolean("dimensionworks.energy.probe") || !worldReady || finished || event.phase != TickEvent.Phase.END) return;
        try {
            ServerLevel level = event.getServer().overworld();
            tick++;
            if (Boolean.getBoolean("dimensionworks.energy.reload")) {
                if(tick==80) verifyReload(level);
                return;
            }
            if (tick == 1) setup(level);
            if (tick == 100 && !reactor.canActivate() && chargeWait++<1000) { tick=99; return; }
            if (tick == 100) {
                checkGenerators(true);
                check("reactor native structure bound", reactor.structureValid.get());
                CHECKS.add("DETAIL reactor state="+reactor.reactorState.get()+" startup="+reactor.startupInitialized.get()+" shield="+reactor.shieldCharge.get()+" saturation="+reactor.saturation.get()+" temperature="+reactor.temperature.get());
                var rs=(KineticBlockEntity)level.getBlockEntity(REACTOR_POS.above(5));
                CHECKS.add("DETAIL input shaft speed="+rs.getSpeed()+" theoretical="+rs.getTheoreticalSpeed()+" source="+rs.hasSource()+" overloaded="+rs.isOverStressed()+" capacity="+rs.getOrCreateNetwork().calculateCapacity()+" stress="+rs.getOrCreateNetwork().calculateStress());
                check("reactor charged by mechanical input", reactor.shieldCharge.get()>0 && reactor.saturation.get()>0);
                check("reactor native activation ready", reactor.canActivate());
                reactor.activateReactor();
                reactor.temperature.set(8000);
                var portableEnergy=mekanism.common.integration.energy.EnergyCompatUtils.getStrictEnergyHandler(portable);
                check("portable tool charges through paid mechanical pad",portableEnergy!=null && !portableEnergy.getEnergy(0).isZero());
                MechanicalEnergy.reserveBurst(teleporter,FloatingLong.create(1000));
                var burstEnergy=teleporter.getEnergyContainers(null).get(0);
                near("teleport burst reserves exact FE equivalent",MachinePowerManager.powerRoute(teleporter).network().calculateStress(),800);
                near("teleport budget available only once",burstEnergy.extract(FloatingLong.create(1000),Action.EXECUTE,AutomationType.INTERNAL).doubleValue(),1000);
                check("teleport burst cannot be extracted twice",burstEnergy.extract(FloatingLong.ONE,Action.EXECUTE,AutomationType.INTERNAL).isZero());
                check("working machine has Create power", MachinePowerManager.hasMechanicalPower(machine));
                check("native recipe produced items", machine.getInventorySlots(null).stream().anyMatch(s -> s.getStack().is(Items.FLINT)));
                var energy = machine.getEnergyContainers(null).get(0);
                near("machine native J rate converts to SU", MechanicalEnergy.stressPerRpm(machine) * StressRules.targetRpm(machine),
                    EnergyUnit.FORGE_ENERGY.convertTo(MechanicalEnergy.rate(energy,machine)).doubleValue()*2);
                check("external insertion rejected", energy.insert(FloatingLong.create(100),Action.EXECUTE,AutomationType.EXTERNAL).equals(FloatingLong.create(100)));
                check("external extraction rejected", energy.extract(FloatingLong.create(100),Action.EXECUTE,AutomationType.EXTERNAL).isZero());
                check("virtual power absent from NBT", !energy.serializeNBT().contains("stored"));
                // Exhaust and simulate the same real container, without allowing a second tick's budget.
                var remaining = energy.getEnergy();
                for (int i=0;i<20;i++) near("simulation stable "+i, energy.extract(remaining,Action.SIMULATE,AutomationType.INTERNAL).doubleValue(),remaining.doubleValue());
                energy.extract(remaining,Action.EXECUTE,AutomationType.INTERNAL);
                check("finite energy cannot be extracted twice", energy.extract(FloatingLong.ONE,Action.EXECUTE,AutomationType.INTERNAL).isZero());
                turbine=((TileEntityTurbineValve)level.getBlockEntity(TURBINE_PORT)).getMultiblock();
                fusion=((TileEntityFusionReactorPort)level.getBlockEntity(FUSION_PORT)).getMultiblock();
                check("formed turbine", turbine != null && turbine.isFormed());
                check("formed fusion reactor", fusion != null && fusion.isFormed());
                turbine.gasTank.setStack(MekanismGases.STEAM.getStack(turbine.getSteamCapacity()/2));
                fusion.heatCapacitor.setHeat(fusion.heatCapacitor.getHeatCapacity()*10_000_000D);
                fusion.updateTemperatures();
                GENERATORS.get(3).getGasTanks(null).get(0).setStack(MekanismGases.HYDROGEN.getStack(10000));
                ((TileEntityBioGenerator)GENERATORS.get(4)).bioFuelTank.setStack(GeneratorsFluids.BIOETHANOL.getFluidStack(10000));
            }
            if (tick == 140) {
                check("reactor native running state", reactor.reactorState.get()==com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore.ReactorState.RUNNING);
                near("four reactor shafts share one FE output",reactorPower(level),reactor.generationRate.get()*2);
                check("reactor outputs actually rotate", REACTOR_SHAFTS.stream().allMatch(p -> level.getBlockEntity(p) instanceof KineticBlockEntity k && k.getGeneratedSpeed()==256));
                shieldBeforeLoss=reactor.shieldCharge.get();
                level.setBlockAndUpdate(REACTOR_POS.above(6),Blocks.AIR.defaultBlockState());
                check("turbine actual mechanical output", shaftPower(level,TURBINE_PORT.west())>0);
                near("turbine ports share production", shaftPower(level,TURBINE_PORT.west())+shaftPower(level,new BlockPos(57,121,8)),
                    EnergyUnit.FORGE_ENERGY.convertTo(turbine.getProductionRate()).doubleValue()*2);
                check("fusion thermal generation reaches shaft", shaftPower(level,FUSION_PORT.south())>0);
                check("multiblock FE inventories drained", turbine.energyContainer.getEnergy().isZero() && fusion.energyContainer.getEnergy().isZero());
                check("gas fuel really consumed",GENERATORS.get(3).getGasTanks(null).get(0).getStored()<10000);
                near("gas actual generation converted",totalPower(GENERATORS.get(3)),EnergyUnit.FORGE_ENERGY.convertTo(gasRate()).doubleValue()*2);
                check("bio fuel really consumed", ((TileEntityBioGenerator)GENERATORS.get(4)).bioFuelTank.getFluidAmount()<10000);
                near("bio output is actual generation rather than doubled transfer limit", totalPower(GENERATORS.get(4)),
                    EnergyUnit.FORGE_ENERGY.convertTo(MekanismGeneratorsConfig.generators.bioGeneration.get()).doubleValue()*2);
                // Roof the solar generator and stop the bio generator by removing fuel.
                TileEntityGenerator solar=GENERATORS.get(0);
                level.setBlockAndUpdate(solar.getBlockPos().above(),Blocks.STONE.defaultBlockState());
                ((TileEntityBioGenerator)GENERATORS.get(4)).bioFuelTank.setEmpty();
                ((mekanism.common.capabilities.energy.ResistiveHeaterEnergyContainer)heater.getEnergyContainer()).updateEnergyUsage(FloatingLong.create(1_000_000_000_000D));
                ((mekanism.common.capabilities.energy.ResistiveHeaterEnergyContainer)aeHeater.getEnergyContainer()).updateEnergyUsage(FloatingLong.create(1_000_000_000_000D));
                originalRatio=MekanismConfig.general.forgeConversionRate.get().copy();
                MekanismConfig.general.forgeConversionRate.set(FloatingLong.create(5));
            }
            if (tick == 180) {
                check("loss of SU stops free shield refill",reactor.shieldCharge.get()<shieldBeforeLoss);
                motor(level,REACTOR_POS.above(6),Direction.DOWN);
                level.setBlockAndUpdate(REACTOR_SHAFTS.get(0),Blocks.AIR.defaultBlockState());
                check("roofed solar stops", totalPower(GENERATORS.get(0))==0);
                check("empty bio fuel stops", totalPower(GENERATORS.get(4))==0);
                check("oversized consumer overloads its real network", machineShaft.isOverStressed());
                near("overloaded AE acceptor injects zero",aeValue(),0);
                check("overload blocks work", !MachinePowerManager.hasMechanicalPower(machine));
                TileEntityWindGenerator wind=(TileEntityWindGenerator)GENERATORS.get(1);
                near("custom J/FE ratio is honored", totalPower(wind),
                    MekanismGeneratorsConfig.generators.windGenerationMin.get().multiply(wind.getCurrentMultiplier()).doubleValue()/5*2);
                near("wind two output faces share one production", shaftPower(level,OUTPUTS.get(wind).get(0)),shaftPower(level,OUTPUTS.get(wind).get(1)));
                ((mekanism.common.capabilities.energy.ResistiveHeaterEnergyContainer)heater.getEnergyContainer()).updateEnergyUsage(FloatingLong.ONE);
                level.setBlockAndUpdate(GENERATORS.get(0).getBlockPos().above(),Blocks.AIR.defaultBlockState());
                ((mekanism.common.capabilities.energy.ResistiveHeaterEnergyContainer)aeHeater.getEnergyContainer()).updateEnergyUsage(FloatingLong.ONE);
                MekanismConfig.general.forgeConversionRate.set(originalRatio);
                originalRatio=null;
            }
            if (tick == 220) {
                near("remaining reactor ports do not multiply power",reactorPower(level),reactor.generationRate.get()*2);
                check("restored input recharges shield",reactor.shieldCharge.get()>shieldBeforeLoss*0.99);
                reactor.shutdownReactor();
                check("reducing load recovers network", !machineShaft.isOverStressed() && MachinePowerManager.hasMechanicalPower(machine));
                check("uncovering solar recovers generation", totalPower(GENERATORS.get(0))>0);
                // A stopped and reverse-rotating energy acceptor must use actual mechanical power.
                check("AE acceptor has nonzero mechanical input",aeValue()>0);
                near("AE acceptor FE equivalence", acceptor.calculateStressApplied()*Math.abs(acceptor.getSpeed())/2,
                    PowerUnits.AE.convertTo(PowerUnits.FE,aeValue()));
                level.setBlockAndUpdate(new BlockPos(0,120,10),Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(GENERATORS.get(1).getBlockPos(),Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(TURBINE_PORT,Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(16,120,10),Blocks.AIR.defaultBlockState());
            }
            if (tick == 260) {
                near("reactor shutdown revokes all shaft output",reactorPower(level),0);
                check("removed source stops machine", !MachinePowerManager.hasMechanicalPower(machine));
                check("removed wind leaves no phantom sources", OUTPUTS.get(GENERATORS.get(1)).stream().allMatch(p -> shaftPower(level,p)==0));
                check("broken turbine stops all output", shaftPower(level,new BlockPos(57,121,8))==0);
                near("stopped AE acceptor injects zero", aeValue(),0);
                check("client world continues ticking", level.getGameTime()>250);
                CHECKS.add((CHECKS.stream().anyMatch(v -> v.startsWith("FAIL"))?"FAIL":"PASS")+" COMPLETE checks="+CHECKS.size()); write(); finished=true;
            }
        } catch (Throwable error) { fail(error); }
    }

    private static void setup(ServerLevel level) throws Exception {
        level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,eventServer(level));
        for(int x=-1;x<=4;x++) for(int z=-1;z<=2;z++) level.setChunkForced(x,z,true);
        var player=level.getServer().getPlayerList().getPlayers().get(0);
        // Explicit endgame fixture: preserve real quota code, repair only this disposable test character.
        for(int bit=1;bit<=64;bit<<=1) dev.szx.dimensionworks.rpmlimit.GearHeartEffects.repairEquippedCurse(player,bit);
        dev.szx.dimensionworks.rpmlimit.GearHeartEffects.tickPlayer(player);
        check("endgame fixture has repaired industrial quotas", !dev.szx.dimensionworks.rpmlimit.GearHeartEffects.hasQuota(player.getUUID()));
        dev.szx.dimensionworks.rpmlimit.RpmLimitManager.setLimit(player,10240);
        player.getAbilities().flying=true;player.onUpdateAbilities();player.connection.teleport(12,125,18,180,20);
        checkCoverage(level);
        place(level,new BlockPos(12,120,18),"mekanism:chargepad");
        chargepad=(TileEntityMekanism)level.getBlockEntity(new BlockPos(12,120,18));
        motor(level,new BlockPos(12,120,17),Direction.SOUTH);
        portable=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("mekanism:atomic_disassembler")));
        player.getInventory().setItem(0,portable);player.connection.teleport(12.5,120.125,18.5,180,20);
        motor(level,new BlockPos(24,120,10),Direction.EAST);
        place(level,new BlockPos(25,120,10),"mekanism:teleporter");
        teleporter=(TileEntityMekanism)level.getBlockEntity(new BlockPos(25,120,10));
        place(level,REACTOR_POS,"draconicevolution:reactor_core");
        reactor=(com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore)level.getBlockEntity(REACTOR_POS);
        for(Direction side:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) {
            BlockPos componentPos=REACTOR_POS.relative(side,4);
            place(level,componentPos,"draconicevolution:reactor_stabilizer");
            var component=(com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorComponent)level.getBlockEntity(componentPos);
            component.facing.set(side.getOpposite());
            BlockPos shaft=REACTOR_POS.relative(side,5);placeShaft(level,shaft,side.getAxis());REACTOR_SHAFTS.add(shaft);
        }
        place(level,REACTOR_POS.above(4),"draconicevolution:reactor_injector");
        ((com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorComponent)level.getBlockEntity(REACTOR_POS.above(4))).facing.set(Direction.DOWN);
        placeShaft(level,REACTOR_POS.above(5),Direction.Axis.Y);
        motor(level,REACTOR_POS.above(6),Direction.DOWN);
        reactor.reactableFuel.set(1000);
        reactor.attemptInitialization();reactor.chargeReactor();
        String[] ids={"solar_generator","wind_generator","heat_generator","gas_burning_generator","bio_generator","advanced_solar_generator"};
        for(int i=0;i<ids.length;i++) {
            BlockPos pos=new BlockPos(i*5,120,0);place(level,pos,"mekanismgenerators:"+ids[i]);
            TileEntityGenerator gen=(TileEntityGenerator)level.getBlockEntity(pos);GENERATORS.add(gen);
            List<BlockPos> ports=new ArrayList<>();
            for(Direction side:Direction.values()) if(!gen.getEnergyContainers(side).isEmpty()) {
                BlockPos shaft=pos.relative(side);placeShaft(level,shaft,side.getAxis());ports.add(shaft);
            }
            OUTPUTS.put(gen,ports);gen.getEnergyContainer().setEnergy(FloatingLong.create(1_000_000));
        }
        motor(level,new BlockPos(0,120,10),Direction.EAST);
        placeShaft(level,new BlockPos(1,120,10),Direction.Axis.X);
        place(level,new BlockPos(2,120,10),"mekanism:enrichment_chamber");
        machine=(TileEntityMekanism)level.getBlockEntity(new BlockPos(2,120,10));
        machine.getComponent().addUpgrades(Upgrade.SPEED,8);
        machine.getInventorySlots(null).get(0).setStack(new ItemStack(Items.GRAVEL,16));
        machineShaft=(KineticBlockEntity)level.getBlockEntity(new BlockPos(1,120,10));
        place(level,new BlockPos(1,120,11),"mekanism:resistive_heater");
        heater=(TileEntityResistiveHeater)level.getBlockEntity(new BlockPos(1,120,11));
        // Put the heater above a second vertical branch to the same motor network.
        level.setBlockAndUpdate(new BlockPos(1,120,11),Blocks.AIR.defaultBlockState());
        place(level,new BlockPos(1,121,10),"mekanism:resistive_heater");
        heater=(TileEntityResistiveHeater)level.getBlockEntity(new BlockPos(1,121,10));
        // A gearbox exposes axes for both machine and heater.
        place(level,new BlockPos(1,120,10),"create:gearbox");machineShaft=(KineticBlockEntity)level.getBlockEntity(new BlockPos(1,120,10));
        place(level,new BlockPos(1,120,11),"mekanism:resistive_heater");heater=(TileEntityResistiveHeater)level.getBlockEntity(new BlockPos(1,120,11));
        level.setBlockAndUpdate(new BlockPos(1,121,10),Blocks.AIR.defaultBlockState());
        motor(level,new BlockPos(16,120,10),Direction.EAST);
        place(level,new BlockPos(17,120,10),"appliedcreate:kinetic_energy_acceptor");
        level.setBlockAndUpdate(new BlockPos(17,120,10),level.getBlockState(new BlockPos(17,120,10)).setValue(com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING,Direction.WEST));
        placeShaft(level,new BlockPos(18,120,10),Direction.Axis.X);
        place(level,new BlockPos(19,120,10),"mekanism:resistive_heater");
        aeHeater=(TileEntityResistiveHeater)level.getBlockEntity(new BlockPos(19,120,10));
        acceptor=(KineticEnergyAcceptorBlockEntity)level.getBlockEntity(new BlockPos(17,120,10));
        // Use native builders through their protected fixture entrypoint.
        build(new GeneratorsBuilders.TurbineBuilder(),level,new BlockPos(40,120,0));
        place(level,TURBINE_PORT,"mekanismgenerators:turbine_valve");
        place(level,new BlockPos(56,121,8),"mekanismgenerators:turbine_valve");
        placeShaft(level,TURBINE_PORT.west(),Direction.Axis.X);
        placeShaft(level,new BlockPos(57,121,8),Direction.Axis.X);
        turbine=((TileEntityTurbineValve)level.getBlockEntity(TURBINE_PORT)).getMultiblock();
        build(new GeneratorsBuilders.FusionReactorBuilder(),level,new BlockPos(40,120,21));
        place(level,FUSION_PORT,"mekanismgenerators:fusion_reactor_port");
        ((TileEntityFusionReactorPort)level.getBlockEntity(FUSION_PORT)).setActive(true);
        placeShaft(level,FUSION_PORT.south(),Direction.Axis.Z);
        fusion=((TileEntityFusionReactorPort)level.getBlockEntity(FUSION_PORT)).getMultiblock();
    }

    private static void verifyReload(ServerLevel level) {
        machine=(TileEntityMekanism)level.getBlockEntity(new BlockPos(2,120,10));
        acceptor=(KineticEnergyAcceptorBlockEntity)level.getBlockEntity(new BlockPos(17,120,10));
        check("reload: unpowered machine does not restore energy",!MachinePowerManager.hasMechanicalPower(machine) && machine.getEnergyContainers(null).get(0).getEnergy().isZero());
        near("reload: stopped AE acceptor remains stopped",aeValue(),0);
        near("reload: removed wind has no phantom output",shaftPower(level,new BlockPos(5,119,0)),0);
        near("reload: broken turbine has no phantom output",shaftPower(level,new BlockPos(57,121,8)),0);
        var solar=(TileEntitySolarGenerator)level.getBlockEntity(new BlockPos(0,120,0));
        check("reload: solar resumes actual production",shaftPower(level,new BlockPos(0,119,0))>0);
        near("reload: solar conversion remains exact",shaftPower(level,new BlockPos(0,119,0)),solar.getProductionRate().doubleValue()/MekanismConfig.general.forgeConversionRate.get().doubleValue()*2);
        check("reload: generator cannot restore FE inventory",solar.getEnergyContainer().getEnergy().isZero());
        for(Direction side:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) {
            var block=level.getBlockEntity(REACTOR_POS.relative(side,5));
            if(block instanceof KineticBlockEntity shaft)near("reload: stopped reactor port "+side,shaft.getGeneratedSpeed(),0);
        }
        CHECKS.add((CHECKS.stream().anyMatch(v->v.startsWith("FAIL"))?"FAIL":"PASS")+" RELOAD COMPLETE checks="+CHECKS.size());write();finished=true;
    }

    private static net.minecraft.server.MinecraftServer eventServer(ServerLevel level) { return level.getServer(); }
    private static void checkCoverage(ServerLevel level) {
        int count=0;BlockPos pos=new BlockPos(-8,120,-8);
        for(var id:ForgeRegistries.BLOCKS.getKeys()) {
            if(List.of("ae2","expatternprovider","neoecoae").contains(id.getNamespace())) {
                BlockState state=ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();
                if(state.hasBlockEntity()) {
                    level.setBlockAndUpdate(pos,state);
                    if(level.getBlockEntity(pos) instanceof appeng.blockentity.powersink.AEBasePoweredBlockEntity powered) {
                        near("AE machine rejects FE demand "+id,powered.getExternalPowerDemand(PowerUnits.FE,100),0);
                        near("AE machine rejects FE insertion "+id,powered.injectExternalPower(PowerUnits.FE,100,appeng.api.config.Actionable.MODULATE),100);
                    }
                }
                continue;
            }
            if(!id.getNamespace().equals("mekanism"))continue;
            BlockState state=ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();
            if(!state.hasBlockEntity())continue;
            level.setBlockAndUpdate(pos,state);
            if(level.getBlockEntity(pos) instanceof TileEntityMekanism tile && StressRules.isEligible(tile)) {
                count++;
                for(var energy:tile.getEnergyContainers(null)) {
                    check("energy owner mapped "+id,MechanicalEnergy.owner(energy)==tile);
                    check("finite budget installed "+id,energy instanceof IMechanicalEnergyBudget);
                }
            }
        }
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        check("registry coverage includes factories and special machines: "+count,count>=40);
    }
    private static void build(Object builder,ServerLevel level,BlockPos pos)throws Exception {
        var method=builder.getClass().getDeclaredMethod("build",Level.class,BlockPos.class,boolean.class);
        method.setAccessible(true);method.invoke(builder,level,pos,false);
    }
    private static double reactorPower(ServerLevel level) {
        double total=0;
        for(BlockPos p:REACTOR_SHAFTS) if(level.getBlockEntity(p) instanceof KineticBlockEntity shaft) total+=shaft.calculateAddedStressCapacity()*Math.abs(shaft.getGeneratedSpeed());
        return total;
    }
    private static void checkGenerators(boolean day) {
        for(int i=0;i<GENERATORS.size();i++) {
            TileEntityGenerator gen=GENERATORS.get(i);
            check("generator native storage not exported "+i,gen.getEnergyContainer().getEnergy().isZero());
            if(i==2||i==3||i==4)near("unfueled generator stops "+i,totalPower(gen),0);
            if(i==0||i==1||i==5) {
                check("physical shafts really joined source network "+i,OUTPUTS.get(gen).stream().allMatch(p -> {
                    var k=(KineticBlockEntity)gen.getLevel().getBlockEntity(p);
                    return k.getSpeed()!=0 && k.hasNetwork() && k.getOrCreateNetwork().sources.containsKey(k);
                }));
            }
            if(i==0||i==5) {
                var solar=(TileEntitySolarGenerator)gen;
                check("solar produces with daylight "+i,totalPower(gen)>0);
                near("solar actual J to FE to SU "+i,totalPower(gen),EnergyUnit.FORGE_ENERGY.convertTo(solar.getProductionRate()).doubleValue()*2);
            }
        }
    }
    private static double totalPower(TileEntityGenerator gen) {return OUTPUTS.get(gen).stream().mapToDouble(p->shaftPower((ServerLevel)gen.getLevel(),p)).sum();}
    private static double shaftPower(ServerLevel level,BlockPos pos) {
        return level.getBlockEntity(pos) instanceof KineticBlockEntity shaft?GeneratorPowerBridge.stressPerRpm(shaft)*GeneratorPowerBridge.outputRpm():0;
    }
    private static void place(ServerLevel level,BlockPos pos,String id) {
        level.setBlockAndUpdate(pos,ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)).defaultBlockState());
        if(level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) {
            dev.szx.dimensionworks.rpmlimit.RpmLimitManager.setOwner(kinetic,level.getServer().getPlayerList().getPlayers().get(0).getUUID());
        }
    }
    private static void placeShaft(ServerLevel level,BlockPos pos,Direction.Axis axis) {
        place(level,pos,"create:shaft");level.setBlockAndUpdate(pos,level.getBlockState(pos).setValue(ShaftBlock.AXIS,axis));
    }
    private static void motor(ServerLevel level,BlockPos pos,Direction facing) {
        place(level,pos,"create:creative_motor");level.setBlockAndUpdate(pos,level.getBlockState(pos).setValue(CreativeMotorBlock.FACING,facing));
        ((CreativeMotorBlockEntity)level.getBlockEntity(pos)).generatedSpeed.setValue(256);
    }
    private static void check(String name,boolean ok) {
        CHECKS.add((ok?"PASS ":"FAIL ")+name);
        System.out.println("[DW_ENERGY_PROBE] "+CHECKS.get(CHECKS.size()-1));

    }
    private static FloatingLong gasRate() {
        try { var method=TileEntityGasGenerator.class.getDeclaredMethod("getProductionRate");method.setAccessible(true);return (FloatingLong)method.invoke(GENERATORS.get(3)); }
        catch(Exception error) { throw new RuntimeException(error); }
    }
    private static double aeValue() {
        try { var field=KineticEnergyAcceptorBlockEntity.class.getDeclaredField("aeValue"); field.setAccessible(true); return field.getDouble(acceptor); }
        catch(Exception error) { throw new RuntimeException(error); }
    }
    private static void near(String name,double actual,double expected) {check(name+" actual="+actual+" expected="+expected,Double.isFinite(actual)&&Math.abs(actual-expected)<=Math.max(0.1,Math.abs(expected)*0.00001));}
    private static void write() {try{Files.write(RESULT,CHECKS);}catch(Exception e){throw new RuntimeException(e);}}
    private static void fail(Throwable error) {error.printStackTrace();CHECKS.add("FAIL "+error);write();finished=true;}
}
