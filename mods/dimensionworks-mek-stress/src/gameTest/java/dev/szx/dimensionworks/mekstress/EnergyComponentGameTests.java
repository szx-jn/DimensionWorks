package dev.szx.dimensionworks.mekstress;

import dev.szx.dimensionworks.mekstress.integration.mek.ZeroEnergyHandler;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.energy.AttachedEnergy;
import mekanism.common.registries.MekanismItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionWorksMekStress.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnergyComponentGameTests {
    @GameTest(template = "empty")
    public static void tabletRejectsMekanismAndNeoForgeEnergy(GameTestHelper h) {
        ItemStack tablet = new ItemStack(MekanismItems.ENERGY_TABLET.asItem());
        tablet.set(ContainerType.ENERGY.getComponentType().get(), new AttachedEnergy(List.of(1_000L)));
        var fromData = ContainerType.ENERGY.createHandlerIfData(tablet);
        var fromFactory = ContainerType.ENERGY.createHandler(tablet);
        h.assertTrue(fromData instanceof ZeroEnergyHandler && fromFactory instanceof ZeroEnergyHandler,
            "Both real Mekanism component factories must enforce the manufacturing-tablet behavior");
        var nativeEnergy = tablet.getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.item());
        h.assertTrue(nativeEnergy != null && nativeEnergy.getEnergy(0) == 0
            && nativeEnergy.insertEnergy(1_000L, Action.EXECUTE) == 1_000L
            && nativeEnergy.extractEnergy(1_000L, Action.EXECUTE) == 0, "Mekanism cannot charge or drain tablets");
        var fe = tablet.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        h.assertTrue(fe == null || (fe.getEnergyStored() == 0 && fe.receiveEnergy(1_000, false) == 0
            && fe.extractEnergy(1_000, false) == 0), "NeoForge adapters cannot bypass tablet restrictions");
        h.assertTrue(ContainerType.ENERGY.getOrEmpty(tablet).equals(AttachedEnergy.EMPTY),
            "A charged input component is removed rather than retained as hidden FE");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unpoweredCrusherCannotStoreOrExportEnergy(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mekanism:crusher")).defaultBlockState());
        var machine = (mekanism.common.tile.base.TileEntityMekanism) h.getBlockEntity(pos);
        var energy = machine.getEnergyContainers(null).getFirst();
        h.assertTrue(energy.insert(10_000L, Action.EXECUTE, AutomationType.EXTERNAL) == 10_000L,
            "External energy must not replace a physical Create power route");
        energy.setEnergy(10_000L);
        h.assertTrue(energy.getEnergy() == 0 && energy.extract(10_000L, Action.EXECUTE, AutomationType.EXTERNAL) == 0,
            "A stopped mechanical machine must neither expose stored FE nor export it");
        h.assertTrue(energy.serializeNBT(h.getLevel().registryAccess()).isEmpty(),
            "The virtual mechanical budget must never persist as FE in the world");
        h.succeed();
    }
}
